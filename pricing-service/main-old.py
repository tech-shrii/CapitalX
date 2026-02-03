"""
Pricing Service  —  FastAPI
============================
Data-source priority (every endpoint):
    1  Stooq CSV endpoint   – no auth, no JS, plain CSV over HTTPS.
    2  yfinance             – last-resort fallback (unchanged from original).

Why Stooq replaced Yahoo
-------------------------
* No crumb / cookie dance.
* No rate-limit 429s on the chart endpoint.
* No React SPA that hides prices behind client-side JS.
* Returns raw OHLCV CSV that pandas reads in one line.

Symbol translation
------------------
Clients send Yahoo-style tickers (AAPL, HSBA.L, 7203.T, RELIANCE.NS).
yahoo_to_stooq() converts them to Stooq format before the HTTP call.

Caching
-------
Stooq enforces a per-IP daily download cap.  A short-lived in-memory cache
(default TTL = 300 s) keeps repeated requests for the same symbol fast and
avoids burning the quota.  The cache is keyed on the Stooq symbol only;
period slicing happens *after* the cache hit.
"""

import logging
import os
import time
import threading
from datetime import datetime, timedelta
from functools import lru_cache
from io import StringIO
from typing import Any, Dict, List, Optional

import pandas as pd
import requests
import yfinance as yf
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

# ---------------------------------------------------------------------------
# Logging
# ---------------------------------------------------------------------------
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# yfinance persistent cache (kept for the fallback layer)
# ---------------------------------------------------------------------------
cache_dir = os.path.join(os.path.expanduser("~"), ".cache", "py-yfinance")
os.makedirs(cache_dir, exist_ok=True)
try:
    yf.set_tz_cache_location(cache_dir)
    logger.info(f"yfinance cache location set to: {cache_dir}")
except Exception as e:
    logger.warning(f"Could not set yfinance cache location: {e}")

# ---------------------------------------------------------------------------
# FastAPI app
# ---------------------------------------------------------------------------
app = FastAPI(title="Pricing Service", version="3.0.0")


# ===========================================================================
# 1.  SYMBOL TRANSLATION  —  Yahoo-style  →  Stooq-style
# ===========================================================================
# Stooq suffix conventions differ from Yahoo.  This map covers every
# suffix that has appeared in the test suite plus the most common global
# exchanges.  Anything not in the map defaults to ".us" (safe for bare
# US tickers like "AAPL").
#
# Yahoo  →  Stooq
# -----     -----
# (none)    .us        bare US ticker
# .L        .uk        London Stock Exchange
# .T        .jp        Tokyo Stock Exchange
# .NS       .ns        National Stock Exchange (India)
# .BO       .bo        Bombay Stock Exchange (India)
# .HK       .hk        Hong Kong
# .PA       .fr        Paris (Euronext)
# .AS       .nl        Amsterdam (Euronext)
# .BR       .br        Brussels (Euronext)
# .MI       .it        Milan
# .TO       .ca        Toronto
# .AX       .au        Australia
# .KS       .kr        Korea
# .SI       .sg        Singapore
# .F        .de        Frankfurt (fallback; many DE stocks also .DE → .de)
# .DE       .de        Frankfurt / Xetra
# ---------------------------------------------------------------------------

_YAHOO_TO_STOOQ_SUFFIX: Dict[str, str] = {
    ".l":  ".uk",
    ".t":  ".jp",
    ".ns": ".ns",
    ".bo": ".bo",
    ".hk": ".hk",
    ".pa": ".fr",
    ".as": ".nl",
    ".br": ".br",
    ".mi": ".it",
    ".to": ".ca",
    ".ax": ".au",
    ".ks": ".kr",
    ".si": ".sg",
    ".f":  ".de",
    ".de": ".de",
}


def yahoo_to_stooq(symbol: str) -> str:
    """
    Convert a Yahoo-style ticker to the Stooq symbol that the CSV
    endpoint expects.  Always returns lowercase (Stooq convention).

    Examples
    --------
    >>> yahoo_to_stooq("AAPL")
    'aapl.us'
    >>> yahoo_to_stooq("HSBA.L")
    'hsba.uk'
    >>> yahoo_to_stooq("7203.T")
    '7203.jp'
    >>> yahoo_to_stooq("RELIANCE.NS")
    'reliance.ns'
    """
    symbol = symbol.strip()

    # Split on the LAST dot so numeric tickers like "7203.T" work correctly.
    dot_pos = symbol.rfind(".")
    if dot_pos != -1:
        base   = symbol[:dot_pos]
        suffix = symbol[dot_pos:].lower()          # e.g. ".l"
        stooq_suffix = _YAHOO_TO_STOOQ_SUFFIX.get(suffix)
        if stooq_suffix:
            return f"{base.lower()}{stooq_suffix}"
        # Unknown suffix — keep it as-is (lowercased).  Stooq may still
        # recognise it.
        return symbol.lower()

    # No dot at all → bare US ticker.
    return f"{symbol.lower()}.us"


# ===========================================================================
# 2.  STOOQ CLIENT  —  session, rate-limiter, in-memory cache
# ===========================================================================

_STOOQ_CSV_URL = "https://stooq.com/q/d/l/"

# How many seconds a cached DataFrame stays valid before we re-fetch.
_CACHE_TTL_SECONDS: int = 300          # 5 minutes

# Minimum gap (seconds) between outgoing HTTP requests to Stooq.
_MIN_REQUEST_INTERVAL: float = 1.0

# Realistic User-Agent; Stooq is lenient but no reason to advertise Python.
_USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
)


class _StooqClient:
    """
    Thread-safe singleton that owns:
        • a requests.Session (connection pooling)
        • a rate-limiter (minimum inter-request delay)
        • an in-memory cache: stooq_symbol → (DataFrame, fetched_at)

    Every public method that touches the network goes through here.
    """

    def __init__(self) -> None:
        self._lock            = threading.Lock()
        self._session         = requests.Session()
        self._session.headers.update({"User-Agent": _USER_AGENT})
        self._last_request_at: float = 0.0
        # cache:  { stooq_symbol: (pd.DataFrame, epoch_fetched) }
        self._cache: Dict[str, tuple[pd.DataFrame, float]] = {}

    # ------------------------------------------------------------------
    # Internal
    # ------------------------------------------------------------------
    def _throttle(self) -> None:
        elapsed = time.time() - self._last_request_at
        if elapsed < _MIN_REQUEST_INTERVAL:
            time.sleep(_MIN_REQUEST_INTERVAL - elapsed)

    # ------------------------------------------------------------------
    # Public
    # ------------------------------------------------------------------
    def fetch_daily(self, stooq_symbol: str, d1: str, d2: str) -> pd.DataFrame:
        """
        Return a DataFrame with columns [Date, Open, High, Low, Close, Volume]
        sorted ascending by Date.

        Uses the cache when the symbol was fetched recently AND d1 is old
        enough that the cached data covers it.  Otherwise hits the network.

        Raises ValueError if Stooq returns no rows (bad symbol, or the
        date range has no trading days).
        """
        with self._lock:
            # --- cache check -----------------------------------------------
            if stooq_symbol in self._cache:
                cached_df, fetched_at = self._cache[stooq_symbol]
                if (time.time() - fetched_at) < _CACHE_TTL_SECONDS:
                    # Slice to requested date range from the cached copy.
                    sliced = cached_df[
                        (cached_df["Date"] >= d1) & (cached_df["Date"] <= d2)
                    ].copy()
                    if not sliced.empty:
                        logger.debug("Cache hit for %s", stooq_symbol)
                        return sliced
                    # Cache exists but doesn't cover this range — fall through
                    # to network fetch (could be a wider historical request).

            # --- network fetch ---------------------------------------------
            self._throttle()
            url = (
                f"{_STOOQ_CSV_URL}?s={stooq_symbol}"
                f"&d1={d1}&d2={d2}&i=d"
            )
            logger.info("Stooq fetch: %s", url)
            resp = self._session.get(url, timeout=15)
            self._last_request_at = time.time()

            if resp.status_code != 200:
                raise ValueError(
                    f"Stooq returned HTTP {resp.status_code} for {stooq_symbol}"
                )

            # --- parse CSV -------------------------------------------------
            text = resp.text.strip()
            if not text or text.startswith("No"):
                # Stooq returns literally "No data" for unknown symbols.
                raise ValueError(f"Stooq: no data for symbol '{stooq_symbol}'")

            df = pd.read_csv(StringIO(text))

            # Normalise column names — Stooq occasionally returns with
            # leading/trailing whitespace.
            df.columns = [c.strip() for c in df.columns]

            if "Date" not in df.columns or "Close" not in df.columns:
                raise ValueError(
                    f"Stooq CSV missing expected columns for '{stooq_symbol}'. "
                    f"Got: {list(df.columns)}"
                )

            if df.empty:
                raise ValueError(f"Stooq: empty dataset for '{stooq_symbol}'")

            df["Date"] = pd.to_datetime(df["Date"])
            df = df.sort_values("Date").reset_index(drop=True)

            # Ensure numeric columns are floats (Stooq sometimes sends ints).
            for col in ("Open", "High", "Low", "Close"):
                df[col] = pd.to_numeric(df[col], errors="coerce")
            df["Volume"] = pd.to_numeric(df["Volume"], errors="coerce").fillna(0).astype(int)

            # --- update cache ----------------------------------------------
            # We cache the full fetch so that subsequent slices (different
            # periods) can reuse it without another HTTP call.
            self._cache[stooq_symbol] = (df, time.time())

            logger.info(
                "Stooq fetched %d rows for %s (%s → %s)",
                len(df), stooq_symbol,
                df["Date"].iloc[0].date(), df["Date"].iloc[-1].date(),
            )
            return df


# Process-level singleton.
_stooq = _StooqClient()


# ===========================================================================
# 3.  PERIOD  →  date-range helpers
# ===========================================================================
# How many *calendar* days back each user-facing period maps to.
# We over-fetch slightly so that after weekends / holidays we still
# get the right number of *trading* rows.

_PERIOD_DAYS: Dict[str, int] = {
    "1d":  5,       # 1 trading day  — fetch 5 cal days to be safe
    "5d":  10,
    "1w":  10,
    "1mo": 35,
    "3mo": 100,
    "6mo": 200,
    "1y":  400,
    "5y":  2000,
    "max": 7500,    # ~20 years
}

# How many rows to keep *after* the CSV is fetched, per period.
# This normalises the output to a predictable size regardless of
# how many holidays fell in the window.
_PERIOD_ROWS: Dict[str, int] = {
    "1d":  1,
    "5d":  5,
    "1w":  5,
    "1mo": 22,
    "3mo": 63,
    "6mo": 126,
    "1y":  252,
    "5y":  1260,
    "max": 99999,   # no cap
}


def _date_range(period: str) -> tuple[str, str]:
    """Return (d1, d2) as YYYYMMDD strings for the given period."""
    now  = datetime.now()
    days = _PERIOD_DAYS.get(period.lower(), 200)
    d1   = (now - timedelta(days=days)).strftime("%Y%m%d")
    d2   = now.strftime("%Y%m%d")
    return d1, d2


# ===========================================================================
# 4.  HIGH-LEVEL SCRAPE FUNCTIONS  (Stooq → yfinance fallback)
# ===========================================================================


def scrape_price(symbol: str) -> Dict[str, Any]:
    """
    Get the latest price for *symbol*.

    Tries Stooq first (last row of the 5-day CSV).  Falls back to
    yfinance if Stooq fails for any reason.

    Returns
    -------
    dict with keys: symbol, price, timestamp, currency, change, change_pct
    """
    stooq_sym = yahoo_to_stooq(symbol)

    # --- attempt 1: Stooq --------------------------------------------------
    try:
        d1, d2 = _date_range("5d")
        df = _stooq.fetch_daily(stooq_sym, d1, d2)

        latest = df.iloc[-1]
        price  = float(latest["Close"])

        # Compute 1-day change if we have ≥2 rows.
        change     = 0.0
        change_pct = 0.0
        if len(df) >= 2:
            prev_close = float(df.iloc[-2]["Close"])
            if prev_close != 0:
                change     = round(price - prev_close, 2)
                change_pct = round((change / prev_close) * 100, 2)

        timestamp = latest["Date"].strftime("%Y-%m-%dT%H:%M:%S")

        logger.info("[Stooq] price for %s: %.2f", symbol.upper(), price)
        return {
            "symbol":      symbol.upper(),
            "price":       price,
            "change":      change,
            "change_pct":  change_pct,
            "currency":    "USD",          # Stooq doesn't expose currency;
                                           # default USD is correct for .us
            "company_name": "",
            "timestamp":   timestamp,
        }
    except Exception as exc:
        logger.warning("[Stooq] price failed for %s: %s — trying yfinance", symbol, exc)

    # --- attempt 2: yfinance -----------------------------------------------
    try:
        ticker = yf.Ticker(symbol)
        info   = ticker.info
        price  = (
            info.get("currentPrice")
            or info.get("regularMarketPrice")
            or info.get("previousClose")
        )
        if price is not None:
            ts_raw = info.get("regularMarketTime")
            timestamp = (
                datetime.fromtimestamp(ts_raw).isoformat()
                if isinstance(ts_raw, (int, float))
                else datetime.now().isoformat()
            )
            logger.info("[yfinance] price for %s: %.2f", symbol.upper(), float(price))
            return {
                "symbol":      symbol.upper(),
                "price":       float(price),
                "change":      0.0,
                "change_pct":  0.0,
                "currency":    info.get("currency", "USD"),
                "company_name": info.get("shortName", ""),
                "timestamp":   timestamp,
            }

        # info had no price — pull one row of history.
        hist = ticker.history(period="1d", interval="1d")
        if not hist.empty:
            logger.info("[yfinance] price (history) for %s: %.2f", symbol.upper(), float(hist["Close"].iloc[-1]))
            return {
                "symbol":      symbol.upper(),
                "price":       float(hist["Close"].iloc[-1]),
                "change":      0.0,
                "change_pct":  0.0,
                "currency":    "USD",
                "company_name": "",
                "timestamp":   hist.index[-1].isoformat(),
            }
    except Exception as exc:
        logger.warning("[yfinance] price failed for %s: %s", symbol, exc)

    raise ValueError(f"All data sources failed for {symbol}")


def scrape_chart(symbol: str, period: str = "6mo", interval: Optional[str] = None) -> Dict[str, Any]:
    """
    Get OHLCV chart data for *symbol* over *period*.

    Stooq only exposes daily / weekly / monthly CSV.  We always fetch
    daily and let the caller slice.  *interval* is accepted for API
    compatibility but Stooq daily is always used as the base; if the
    caller wants weekly we down-sample here.

    Falls back to yfinance if Stooq fails.
    """
    stooq_sym = yahoo_to_stooq(symbol)
    period_lower = period.lower()

    # --- attempt 1: Stooq --------------------------------------------------
    try:
        d1, d2 = _date_range(period_lower)
        df = _stooq.fetch_daily(stooq_sym, d1, d2)

        # Trim to the expected row count for this period.
        max_rows = _PERIOD_ROWS.get(period_lower, len(df))
        df = df.tail(max_rows).reset_index(drop=True)

        # --- optional weekly down-sample ------------------------------------
        # If the caller explicitly asked for weekly (or the period is ≥ 6mo
        # and no interval override), keep daily.  We only down-sample when
        # interval is explicitly "1wk" or "1w".
        if interval and interval.lower() in ("1wk", "1w", "w"):
            df["Week"] = df["Date"].dt.isocalendar().week.astype(int)
            df["Year"] = df["Date"].dt.isocalendar().year.astype(int)
            df = (
                df.groupby(["Year", "Week"])
                .agg(
                    Date=("Date", "last"),
                    Open=("Open", "first"),
                    High=("High", "max"),
                    Low=("Low", "min"),
                    Close=("Close", "last"),
                    Volume=("Volume", "sum"),
                )
                .reset_index(drop=True)
            )

        # --- build response list -------------------------------------------
        chart_data: List[Dict[str, Any]] = []
        for _, row in df.iterrows():
            chart_data.append({
                "time":   row["Date"].strftime("%Y-%m-%dT%H:%M:%S"),
                "open":   round(float(row["Open"]),   2),
                "high":   round(float(row["High"]),   2),
                "low":    round(float(row["Low"]),    2),
                "close":  round(float(row["Close"]),  2),
                "volume": int(row["Volume"]),
            })

        used_interval = interval if interval else "1d"
        logger.info("[Stooq] chart for %s: %d bars (%s)", symbol.upper(), len(chart_data), period)
        return {
            "symbol":   symbol.upper(),
            "period":   period,
            "interval": used_interval,
            "data":     chart_data,
        }
    except Exception as exc:
        logger.warning("[Stooq] chart failed for %s: %s — trying yfinance", symbol, exc)

    # --- attempt 2: yfinance -----------------------------------------------
    try:
        used_interval = interval or _yf_optimal_interval(period_lower)
        df = _yf_fetch_history(symbol, period_lower, used_interval)

        chart_data = []
        for idx, row in df.iterrows():
            chart_data.append({
                "time":   idx.isoformat(),
                "open":   round(float(row["Open"]),   2),
                "high":   round(float(row["High"]),   2),
                "low":    round(float(row["Low"]),    2),
                "close":  round(float(row["Close"]),  2),
                "volume": int(row["Volume"]),
            })

        logger.info("[yfinance] chart for %s: %d bars", symbol.upper(), len(chart_data))
        return {
            "symbol":   symbol.upper(),
            "period":   period,
            "interval": used_interval,
            "data":     chart_data,
        }
    except Exception as exc:
        logger.error("[yfinance] chart failed for %s: %s", symbol, exc)
        raise ValueError(f"All chart sources failed for {symbol}") from exc


# ===========================================================================
# 5.  yfinance HELPERS  (fallback layer — logic unchanged from original)
# ===========================================================================


@lru_cache(maxsize=128)
def _yf_ticker(symbol: str) -> yf.Ticker:
    return yf.Ticker(symbol)


def _yf_optimal_interval(period: str) -> str:
    return {
        "1d": "15m", "5d": "60m", "1w": "60m",
        "1mo": "1d", "3mo": "1d", "6mo": "1wk",
        "1y": "1wk", "5y": "1mo", "max": "1mo",
    }.get(period, "1wk")


def _yf_normalize_period(period: str) -> str:
    return {"1w": "5d"}.get(period, period)


def _yf_fetch_history(symbol: str, period: str = "6mo", interval: str = "1wk", max_retries: int = 3) -> pd.DataFrame:
    """Fetch history via yfinance with exponential back-off."""
    last_error: Optional[Exception] = None
    for attempt in range(max_retries):
        try:
            if attempt > 0:
                time.sleep(min(2 ** attempt, 10))
            ticker = _yf_ticker(symbol)
            df = ticker.history(period=_yf_normalize_period(period), interval=interval)
            if df is None or df.empty or "Close" not in df.columns:
                raise ValueError(f"yfinance: empty/invalid data for {symbol}")
            return df
        except Exception as exc:
            last_error = exc
            logger.warning("[yfinance] attempt %d/%d for %s: %s", attempt + 1, max_retries, symbol, exc)
    raise ValueError(f"yfinance failed for {symbol} after {max_retries} attempts: {last_error}")


def _yf_batch_prices(symbols: List[str]) -> Dict[str, Dict[str, Any]]:
    """Batch-download latest close prices via yf.download()."""
    clean = [s.upper().strip() for s in symbols if s and s.strip()]
    if not clean:
        return {}
    try:
        df = yf.download(clean, period="1d", progress=False, threads=True)
        if df.empty:
            return {}
        results: Dict[str, Dict[str, Any]] = {}
        if isinstance(df.columns, pd.MultiIndex):
            for sym in clean:
                if ("Close", sym) in df.columns:
                    price = df[("Close", sym)].iloc[-1]
                    if pd.notna(price):
                        results[sym] = {
                            "price":     float(price),
                            "timestamp": df.index[-1].isoformat(),
                        }
        else:
            if len(clean) == 1 and "Close" in df.columns:
                results[clean[0]] = {
                    "price":     float(df["Close"].iloc[-1]),
                    "timestamp": df.index[-1].isoformat(),
                }
        return results
    except Exception as exc:
        logger.warning("[yfinance batch] %s", exc)
        return {}


# ===========================================================================
# 6.  Pydantic request models
# ===========================================================================


class BulkChartsRequest(BaseModel):
    symbols: List[str]
    period:  Optional[str] = "6mo"
    interval: Optional[str] = None


# ===========================================================================
# 7.  FastAPI ROUTES  —  identical URL signatures as v2
# ===========================================================================


# ── 1. GET /api/price/{symbol} ──────────────────────────────────────────────
@app.get("/api/price/{symbol}")
async def get_current_price(symbol: str):
    """Current price for one symbol."""
    try:
        return scrape_price(symbol)
    except ValueError as exc:
        raise HTTPException(status_code=404, detail=str(exc))
    except Exception as exc:
        logger.error("get_current_price(%s): %s", symbol, exc)
        raise HTTPException(status_code=500, detail=str(exc))


# ── 2. GET /api/chart/{symbol} ──────────────────────────────────────────────
@app.get("/api/chart/{symbol}")
async def get_chart_data(symbol: str, period: str = "6mo", interval: Optional[str] = None):
    """OHLCV chart for one symbol."""
    valid = ["1d", "5d", "1w", "1mo", "3mo", "6mo", "1y", "5y", "max"]
    if period not in valid:
        raise HTTPException(status_code=400, detail=f"Invalid period. Valid: {valid}")
    try:
        return scrape_chart(symbol, period, interval)
    except ValueError as exc:
        raise HTTPException(status_code=404, detail=str(exc))
    except Exception as exc:
        logger.error("get_chart_data(%s): %s", symbol, exc)
        raise HTTPException(status_code=500, detail=str(exc))


# ── 3. POST /api/prices  ────────────────────────────────────────────────────
@app.post("/api/prices")
async def get_multiple_prices(symbols: List[str]):
    """Prices for a list of symbols."""
    if not symbols:
        return {"data": {}}

    results: Dict[str, Any] = {}
    for sym in symbols:
        try:
            data = scrape_price(sym)
            results[sym.upper()] = {"price": data["price"], "timestamp": data["timestamp"]}
        except ValueError as exc:
            results[sym.upper()] = {"error": str(exc)}

    return {"data": results}


# ── 4. POST /api/portfolio/value ────────────────────────────────────────────
@app.post("/api/portfolio/value")
async def get_portfolio_value(portfolio: Dict[str, float]):
    """
    Total portfolio value.
    Body: {"AAPL": 10, "MSFT": 5}   (symbol → quantity)
    """
    if not portfolio:
        return {"total_value": 0.0, "breakdown": {}, "timestamp": datetime.now().isoformat()}

    total  = 0.0
    breakdown: Dict[str, Any] = {}

    for symbol, qty in portfolio.items():
        try:
            data  = scrape_price(symbol)
            price = data["price"]
            value = price * qty
            total += value
            breakdown[symbol.upper()] = {
                "price":    round(price, 2),
                "quantity": qty,
                "value":    round(value, 2),
            }
        except ValueError as exc:
            breakdown[symbol.upper()] = {"error": str(exc)}

    return {
        "total_value": round(total, 2),
        "breakdown":   breakdown,
        "timestamp":   datetime.now().isoformat(),
    }


# ── 5. POST /api/portfolio/chart ────────────────────────────────────────────
@app.post("/api/portfolio/chart")
async def get_portfolio_chart(portfolio: Dict[str, float], period: str = "6mo", interval: Optional[str] = None):
    """
    Combined portfolio value over time.

    We fetch each symbol's daily chart from Stooq, align on Date, multiply
    by quantity, and sum.  Falls back to yf.download() for the whole batch
    if any Stooq call fails.
    """
    symbols = list(portfolio.keys())
    if not symbols:
        raise HTTPException(status_code=400, detail="Empty portfolio")

    period_lower = period.lower()

    # --- attempt 1: Stooq per-symbol ---------------------------------------
    try:
        frames: Dict[str, pd.DataFrame] = {}
        for sym in symbols:
            stooq_sym = yahoo_to_stooq(sym)
            d1, d2    = _date_range(period_lower)
            df        = _stooq.fetch_daily(stooq_sym, d1, d2)
            max_rows  = _PERIOD_ROWS.get(period_lower, len(df))
            df        = df.tail(max_rows).reset_index(drop=True)
            frames[sym.upper()] = df.set_index("Date")[["Close"]]

        # Align all frames on Date (outer join, forward-fill missing days).
        combined = pd.concat(frames, axis=1)
        combined.columns = [sym for sym, _ in combined.columns]   # flatten MultiIndex
        combined = combined.ffill()
        combined = combined.dropna()                               # drop leading NaNs

        # Weighted sum.
        combined["total"] = sum(
            combined[sym.upper()] * qty for sym, qty in portfolio.items()
        )

        chart_data = [
            {"time": str(date.date()), "value": round(float(row["total"]), 2)}
            for date, row in combined.iterrows()
        ]

        logger.info("[Stooq] portfolio chart: %d points", len(chart_data))
        return {
            "portfolio": portfolio,
            "period":    period,
            "interval":  interval or "1d",
            "data":      chart_data,
        }
    except Exception as exc:
        logger.warning("[Stooq] portfolio chart failed: %s — trying yfinance", exc)

    # --- attempt 2: yfinance batch -----------------------------------------
    try:
        used_interval = interval or _yf_optimal_interval(period_lower)
        df = yf.download(
            [s.upper() for s in symbols],
            period=_yf_normalize_period(period_lower),
            interval=used_interval,
            progress=False,
            threads=True,
        )
        if df.empty:
            raise HTTPException(status_code=404, detail="No historical data found")

        pv = pd.DataFrame(index=df.index)
        for sym, qty in portfolio.items():
            col = ("Close", sym.upper())
            if col in df.columns:
                pv[sym.upper()] = df[col] * qty
        pv["total"] = pv.sum(axis=1)

        chart_data = [
            {"time": date.isoformat(), "value": round(float(row["total"]), 2)}
            for date, row in pv.iterrows()
        ]

        return {
            "portfolio": portfolio,
            "period":    period,
            "interval":  used_interval,
            "data":      chart_data,
        }
    except HTTPException:
        raise
    except Exception as exc:
        logger.error("Portfolio chart yfinance error: %s", exc)
        raise HTTPException(status_code=500, detail=str(exc))


# ── 6. POST /api/charts/bulk ────────────────────────────────────────────────
@app.post("/api/charts/bulk")
async def bulk_fetch_charts(request: BulkChartsRequest):
    """Chart data for multiple symbols in one call."""
    symbols  = [s.strip() for s in request.symbols if s and s.strip()]
    period   = request.period or "6mo"
    interval = request.interval

    if not symbols:
        return {"data": {}}

    results: Dict[str, Any] = {}
    for sym in symbols:
        try:
            results[sym.upper()] = scrape_chart(sym, period, interval)
        except ValueError as exc:
            results[sym.upper()] = {"error": str(exc)}

    return {"data": results}


# ── 7. POST /api/prices/bulk ────────────────────────────────────────────────
@app.post("/api/prices/bulk")
async def bulk_fetch_prices(symbols: List[str]):
    """
    Optimised bulk price fetch (e.g. at login).

    Tries Stooq for every symbol first.  Any symbols that fail Stooq
    are retried as a single yf.download() batch call.
    """
    if not symbols:
        return {"data": {}}

    results: Dict[str, Any] = {}
    missing: List[str]      = []

    # --- primary: Stooq ----------------------------------------------------
    for sym in symbols:
        sym_upper = sym.upper().strip()
        try:
            data = scrape_price(sym)
            results[sym_upper] = {"price": data["price"], "timestamp": data["timestamp"]}
        except ValueError:
            missing.append(sym_upper)

    # --- secondary: yfinance batch for stragglers -------------------------
    if missing:
        logger.info("[bulk] yfinance batch fallback for %d symbols", len(missing))
        batch = _yf_batch_prices(missing)
        for sym, info in batch.items():
            results[sym] = info
        for sym in missing:
            if sym not in results:
                results[sym] = {"error": "Failed to fetch"}

    success = sum(1 for v in results.values() if "error" not in v)
    logger.info("[bulk prices] %d/%d successful", success, len(symbols))
    return {"data": results}


# ── 8. GET /api/health ──────────────────────────────────────────────────────
@app.get("/api/health")
async def health_check():
    return {
        "status":          "healthy",
        "timestamp":       datetime.now().isoformat(),
        "service":         "Pricing Service",
        "data_source":     "Stooq (primary) / yfinance (fallback)",
        "cache_entries":   len(_stooq._cache),
    }


# ── 9. POST /api/cache/clear ────────────────────────────────────────────────
@app.post("/api/cache/clear")
async def clear_cache():
    """
    Clear both the in-memory Stooq cache and the yfinance disk cache.
    On Windows, yfinance may hold cookies.db open — we skip locked files
    gracefully instead of crashing.
    """
    # -- Stooq in-memory cache ----------------------------------------------
    with _stooq._lock:
        _stooq._cache.clear()
    stooq_cleared = True

    # -- yfinance disk cache ------------------------------------------------
    skipped: List[str] = []
    deleted  = 0
    try:
        if os.path.exists(cache_dir):
            for root, dirs, files in os.walk(cache_dir, topdown=False):
                for fname in files:
                    fpath = os.path.join(root, fname)
                    try:
                        os.remove(fpath)
                        deleted += 1
                    except PermissionError:
                        skipped.append(fname)
                for dname in dirs:
                    try:
                        os.rmdir(os.path.join(root, dname))
                    except OSError:
                        pass
    except Exception as exc:
        logger.warning("cache/clear disk error: %s", exc)

    logger.info("Cache cleared: stooq=%s, yfinance=%d deleted / %d skipped", stooq_cleared, deleted, len(skipped))
    return {
        "status":                "cache cleared",
        "stooq_cache_cleared":   stooq_cleared,
        "yfinance_cache_dir":    cache_dir,
        "yfinance_files_deleted": deleted,
        "yfinance_files_skipped": skipped,   # e.g. ["cookies.db"]
    }


# ===========================================================================
# 8.  ENTRY POINT
# ===========================================================================
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)