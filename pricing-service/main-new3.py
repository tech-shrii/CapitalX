"""
Pricing Service  —  FastAPI (v4.0)
====================================
Enhanced with comprehensive symbol mapping for bonds, forex, commodities, and optimized bulk fetching.

Data-source priority:
    1. Stooq CSV endpoint   – no auth, no JS, plain CSV over HTTPS
    2. yfinance             – fallback for unsupported symbols

Key improvements:
- Comprehensive symbol translation for all asset types
- Optimized bulk price fetching
- Better error handling for bonds and forex
- Proper datetime parsing for Stooq responses
"""

import logging
import os
import time
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
# yfinance persistent cache
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
app = FastAPI(title="Pricing Service", version="4.0.0")


# ===========================================================================
# 1. COMPREHENSIVE SYMBOL TRANSLATION
# ===========================================================================

# Forex pairs: Yahoo/Internal → Stooq format
_FOREX_MAPPING = {
    # Major pairs
    "USDEUR": "eurusd",     # EUR/USD (inverted)
    "USDGBP": "gbpusd",     # GBP/USD (inverted)
    "USDJPY": "usdjpy",
    "USDCAD": "usdcad",
    "USDHKD": "usdhkd",
    "USDINR": "usdinr",
    "AUDUSD": "audusd",
    "EURGBP": "eurgbp",
    # Additional common pairs
    "EURUSD": "eurusd",
    "GBPUSD": "gbpusd",
    "USDCHF": "usdchf",
    "NZDUSD": "nzdusd",
    "USDSGD": "usdsgd",
}

# Commodities: Yahoo format → Stooq format
_COMMODITY_MAPPING = {
    "GC.F": "gc.f",      # Gold futures
    "SI.F": "si.f",      # Silver futures
    "PL.F": "pl.f",      # Platinum futures
    "CB.F": "cl.f",      # Crude Oil Brent (Note: Stooq uses CL for crude)
    "CL.F": "cl.f",      # Crude Oil WTI
    "HG.F": "hg.f",      # Copper
    "NG.F": "ng.f",      # Natural Gas
}

# Government bonds: Yahoo format → Stooq/Alternative
_BOND_MAPPING = {
    "10YJPY.B": "10yjpy",    # Japan 10Y - try without suffix
    "10YINY.B": "10yiny",    # India 10Y
    "10YDEY.B": "10ydey",    # Germany 10Y
    "10YDKY.B": "10ydky",    # Denmark 10Y
    "TNX": "^tnx",           # US 10Y Treasury Yield
    "^TNX": "^tnx",
}

# Stock exchange suffix mapping (Yahoo → Stooq)
_EXCHANGE_SUFFIX = {
    ".L": ".uk",      # London
    ".T": ".jp",      # Tokyo
    ".NS": ".ns",     # India NSE
    ".BO": ".bo",     # India BSE
    ".HK": ".hk",     # Hong Kong
    ".PA": ".fr",     # Paris
    ".AS": ".nl",     # Amsterdam
    ".BR": ".br",     # Brussels
    ".MI": ".it",     # Milan
    ".TO": ".ca",     # Toronto
    ".AX": ".au",     # Australia
    ".KS": ".kr",     # Korea
    ".SI": ".sg",     # Singapore
    ".F": ".f",       # Frankfurt
    ".DE": ".de",     # Xetra
}


def normalize_symbol(symbol: str) -> dict:
    """
    Normalize any symbol format to both Yahoo and Stooq formats.
    
    Returns:
        dict with keys: 'original', 'yahoo', 'stooq', 'type', 'needs_inversion'
    """
    symbol = symbol.strip().upper()
    result = {
        'original': symbol,
        'yahoo': symbol,
        'stooq': None,
        'type': 'unknown',
        'needs_inversion': False
    }
    
    # 1. Check if it's a forex pair (6 chars, no dot)
    if len(symbol) == 6 and symbol.isalpha() and "." not in symbol:
        result['type'] = 'forex'
        if symbol in _FOREX_MAPPING:
            result['stooq'] = _FOREX_MAPPING[symbol]
            # Check if we need to invert the rate (USD/EUR → EUR/USD)
            if symbol.startswith("USD") and symbol != "USDJPY" and symbol != "USDCAD":
                result['needs_inversion'] = True
        else:
            result['stooq'] = symbol.lower()
        return result
    
    # 2. Check if it's a bond
    if ".B" in symbol or symbol in _BOND_MAPPING:
        result['type'] = 'bond'
        if symbol in _BOND_MAPPING:
            result['stooq'] = _BOND_MAPPING[symbol]
        else:
            # Generic bond handling
            result['stooq'] = symbol.replace(".B", "").lower()
        return result
    
    # 3. Check if it's a commodity futures
    if ".F" in symbol:
        result['type'] = 'commodity'
        if symbol in _COMMODITY_MAPPING:
            result['stooq'] = _COMMODITY_MAPPING[symbol]
        else:
            # Generic commodity handling
            result['stooq'] = symbol.lower()
        return result
    
    # 4. Check if it's a crypto (contains hyphen)
    if "-USD" in symbol or "-USDT" in symbol:
        result['type'] = 'crypto'
        result['stooq'] = symbol.lower()
        return result
    
    # 5. Check if it's a mutual fund (5 chars ending in X)
    if len(symbol) == 5 and symbol.endswith("X"):
        result['type'] = 'mutual_fund'
        result['stooq'] = f"{symbol.lower()}.us"
        return result
    
    # 6. Stock with exchange suffix
    dot_pos = symbol.rfind(".")
    if dot_pos != -1:
        result['type'] = 'stock'
        base = symbol[:dot_pos]
        suffix = symbol[dot_pos:].lower()
        stooq_suffix = _EXCHANGE_SUFFIX.get(suffix)
        if stooq_suffix:
            result['stooq'] = f"{base.lower()}{stooq_suffix}"
        else:
            result['stooq'] = symbol.lower()
        return result
    
    # 7. Bare US stock ticker
    result['type'] = 'stock'
    result['stooq'] = f"{symbol.lower()}.us"
    
    return result


# ===========================================================================
# 2. STOOQ CLIENT with improved error handling
# ===========================================================================

_STOOQ_BASE_URL = "https://stooq.com"
_USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
)
_http_session = requests.Session()
_http_session.headers.update({"User-Agent": _USER_AGENT})


def _fetch_stooq_latest(stooq_symbol: str) -> dict:
    """
    Fetch latest quote from Stooq.
    1) Try CSV endpoint
    2) If CSV fails -> scrape HTML
    """
    # ----------------------------
    # 1) CSV ENDPOINT
    # ----------------------------
    try:
        url = f"{_STOOQ_BASE_URL}/q/l/"
        params = {
            "s": stooq_symbol,
            "f": "sd2t2ohlc",
            "h": "",
            "e": "csv",
        }

        resp = _http_session.get(url, params=params, timeout=10)
        resp.raise_for_status()

        lines = resp.text.strip().splitlines()

        if not lines:
            raise ValueError("Empty CSV response")

        # Skip header if present
        if lines[0].lower().startswith("symbol"):
            line = lines[1]
        else:
            line = lines[0]

        parts = line.split(",")

        close_price = float(parts[6])

        return {
            "price": close_price,
            "open": float(parts[3]),
            "high": float(parts[4]),
            "low": float(parts[5]),
            "volume": 0,
            "timestamp": datetime.now().isoformat(),
        }

    except Exception as csv_error:
        logger.warning(f"[Stooq-CSV] {stooq_symbol} failed: {csv_error}")

    # ----------------------------
    # 2) HTML FALLBACK
    # ----------------------------
    try:
        url = f"{_STOOQ_BASE_URL}/q/?s={stooq_symbol}"
        resp = _http_session.get(url, timeout=10)
        resp.raise_for_status()

        from bs4 import BeautifulSoup

        soup = BeautifulSoup(resp.text, "lxml")

        price_tag = soup.select_one("span#aq_{0}_c2".format(stooq_symbol))

        if not price_tag:
            raise ValueError("Price element not found")

        price = float(price_tag.text.replace(",", ""))

        return {
            "price": price,
            "open": price,
            "high": price,
            "low": price,
            "volume": 0,
            "timestamp": datetime.now().isoformat(),
        }

    except Exception as html_error:
        raise ValueError(
            f"Stooq CSV & HTML failed for '{stooq_symbol}': {html_error}"
        )



def _fetch_stooq_historical(stooq_symbol: str, d1: str, d2: str) -> pd.DataFrame:
    """
    Fetch historical data from Stooq.
    Returns DataFrame with columns [Date, Open, High, Low, Close, Volume].
    """
    url = f"{_STOOQ_BASE_URL}/q/d/l/"
    params = {"s": stooq_symbol, "d1": d1, "d2": d2, "i": "d"}
    
    logger.info(f"Stooq historical: {stooq_symbol}, {d1} → {d2}")
    
    try:
        resp = _http_session.get(url, params=params, timeout=15)
        resp.raise_for_status()
        
        text = resp.text.strip()
        if not text or text.startswith("No") or len(text) < 20:
            raise ValueError(f"No historical data for '{stooq_symbol}'")
        
        df = pd.read_csv(StringIO(text))
        df.columns = [c.strip() for c in df.columns]
        
        if "Date" not in df.columns or "Close" not in df.columns:
            raise ValueError(f"Missing columns for '{stooq_symbol}'")
        
        if df.empty:
            raise ValueError(f"Empty dataset for '{stooq_symbol}'")
        
        df["Date"] = pd.to_datetime(df["Date"], errors='coerce')
        df = df.dropna(subset=['Date'])
        df = df.sort_values("Date").reset_index(drop=True)
        
        for col in ("Open", "High", "Low", "Close"):
            if col in df.columns:
                df[col] = pd.to_numeric(df[col], errors="coerce")
        
        if "Volume" in df.columns:
            df["Volume"] = pd.to_numeric(df["Volume"], errors="coerce").fillna(0).astype(int)
        else:
            df["Volume"] = 0
        
        return df
        
    except Exception as e:
        raise ValueError(f"Stooq historical failed for '{stooq_symbol}': {e}")


# ===========================================================================
# 3. PERIOD HELPERS
# ===========================================================================

_PERIOD_DAYS = {
    "1d": 5, "5d": 10, "1w": 10, "1mo": 35, "3mo": 100,
    "6mo": 200, "1y": 400, "5y": 2000, "max": 7500,
}

_PERIOD_ROWS = {
    "1d": 1, "5d": 5, "1w": 5, "1mo": 22, "3mo": 63,
    "6mo": 126, "1y": 252, "5y": 1260, "max": 99999,
}


def _date_range(period: str) -> tuple:
    """Return (d1, d2) as YYYYMMDD strings."""
    now = datetime.now()
    days = _PERIOD_DAYS.get(period.lower(), 200)
    d1 = (now - timedelta(days=days)).strftime("%Y%m%d")
    d2 = now.strftime("%Y%m%d")
    return d1, d2


# ===========================================================================
# 4. HIGH-LEVEL PRICE FETCHING
# ===========================================================================


def fetch_price(symbol: str) -> Dict[str, Any]:
    """
    Get latest price for symbol.
    Tries Stooq first, falls back to yfinance.
    """
    norm = normalize_symbol(symbol)
    stooq_sym = norm['stooq']
    
    # Try Stooq first
    if stooq_sym:
        try:
            data = _fetch_stooq_latest(stooq_sym)
            price = data['price']
            
            # Invert rate if needed (e.g., EUR/USD instead of USD/EUR)
            if norm['needs_inversion'] and price > 0:
                price = 1.0 / price
            
            # Get previous close for change calculation
            try:
                d1, d2 = _date_range("5d")
                hist_df = _fetch_stooq_historical(stooq_sym, d1, d2)
                
                change = 0.0
                change_pct = 0.0
                if len(hist_df) >= 2:
                    prev_close = float(hist_df.iloc[-2]["Close"])
                    if norm['needs_inversion'] and prev_close > 0:
                        prev_close = 1.0 / prev_close
                    if prev_close != 0:
                        change = round(price - prev_close, 6)
                        change_pct = round((change / prev_close) * 100, 4)
            except:
                change = 0.0
                change_pct = 0.0
            
            logger.info(f"[Stooq] {symbol}: {price:.6f}")
            return {
                "symbol": symbol.upper(),
                "price": round(price, 6),
                "change": change,
                "change_pct": change_pct,
                "currency": "USD",
                "company_name": "",
                "timestamp": data['timestamp'],
                "source": "stooq"
            }
        except Exception as e:
            logger.warning(f"[Stooq] {symbol} failed: {e}")
    
    # Fallback to yfinance
    try:
        ticker = yf.Ticker(norm['yahoo'])
        
        # Try to get price from info
        info = ticker.info
        price = info.get("currentPrice") or info.get("regularMarketPrice") or info.get("previousClose")
        
        # If info fails, try history
        if price is None:
            hist = ticker.history(period="2d")
            if not hist.empty:
                price = hist['Close'].iloc[-1]
            else:
                raise ValueError("yfinance: No price found")
        
        # Get timestamp
        ts_raw = info.get("regularMarketTime")
        timestamp = (
            datetime.fromtimestamp(ts_raw).isoformat()
            if isinstance(ts_raw, (int, float))
            else datetime.now().isoformat()
        )
        
        # Calculate change
        prev_close = info.get('previousClose')
        change = 0.0
        change_pct = 0.0
        if prev_close and prev_close > 0:
            change = round(price - prev_close, 6)
            change_pct = round((change / prev_close) * 100, 4)
        
        logger.info(f"[yfinance] {symbol}: {price:.6f}")
        return {
            "symbol": symbol.upper(),
            "price": float(price),
            "change": change,
            "change_pct": change_pct,
            "currency": info.get("currency", "USD"),
            "company_name": info.get("shortName", ""),
            "timestamp": timestamp,
            "source": "yfinance"
        }
    except Exception as e:
        logger.error(f"[yfinance] {symbol} failed: {e}")
        raise ValueError(f"All sources failed for {symbol}")


def fetch_chart(symbol: str, period: str = "6mo", interval: Optional[str] = None) -> Dict[str, Any]:
    """
    Get OHLCV chart data.
    Tries Stooq first, falls back to yfinance.
    """
    norm = normalize_symbol(symbol)
    stooq_sym = norm['stooq']
    period_lower = period.lower()
    
    # Try Stooq first
    if stooq_sym:
        try:
            d1, d2 = _date_range(period_lower)
            df = _fetch_stooq_historical(stooq_sym, d1, d2)
            
            max_rows = _PERIOD_ROWS.get(period_lower, len(df))
            df = df.tail(max_rows).reset_index(drop=True)
            
            chart_data = []
            for _, row in df.iterrows():
                close_price = float(row["Close"])
                open_price = float(row["Open"])
                high_price = float(row["High"])
                low_price = float(row["Low"])
                
                # Invert if needed
                if norm['needs_inversion']:
                    close_price = 1.0 / close_price if close_price > 0 else 0
                    open_price = 1.0 / open_price if open_price > 0 else 0
                    high_price = 1.0 / low_price if low_price > 0 else 0  # inverted!
                    low_price = 1.0 / high_price if high_price > 0 else 0  # inverted!
                
                chart_data.append({
                    "time": row["Date"].strftime("%Y-%m-%dT%H:%M:%S"),
                    "open": round(open_price, 6),
                    "high": round(max(high_price, low_price), 6),
                    "low": round(min(high_price, low_price), 6),
                    "close": round(close_price, 6),
                    "volume": int(row["Volume"]),
                })
            
            logger.info(f"[Stooq] chart {symbol}: {len(chart_data)} bars")
            return {
                "symbol": symbol.upper(),
                "period": period,
                "interval": "1d",
                "data": chart_data,
                "source": "stooq"
            }
        except Exception as e:
            logger.warning(f"[Stooq] chart {symbol} failed: {e}")
    
    # Fallback to yfinance
    try:
        interval = interval or _yf_optimal_interval(period_lower)
        df = _yf_fetch_history(norm['yahoo'], period_lower, interval)
        
        chart_data = []
        for idx, row in df.iterrows():
            chart_data.append({
                "time": idx.isoformat(),
                "open": round(float(row["Open"]), 6),
                "high": round(float(row["High"]), 6),
                "low": round(float(row["Low"]), 6),
                "close": round(float(row["Close"]), 6),
                "volume": int(row["Volume"]),
            })
        
        logger.info(f"[yfinance] chart {symbol}: {len(chart_data)} bars")
        return {
            "symbol": symbol.upper(),
            "period": period,
            "interval": interval,
            "data": chart_data,
            "source": "yfinance"
        }
    except Exception as e:
        logger.error(f"[yfinance] chart {symbol} failed: {e}")
        raise ValueError(f"All chart sources failed for {symbol}")


# ===========================================================================
# 5. YFINANCE HELPERS (fallback layer)
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
    """Fetch history via yfinance with retry logic."""
    last_error = None
    for attempt in range(max_retries):
        try:
            if attempt > 0:
                time.sleep(min(2 ** attempt, 10))
            ticker = _yf_ticker(symbol)
            df = ticker.history(period=_yf_normalize_period(period), interval=interval)
            if df is None or df.empty or "Close" not in df.columns:
                raise ValueError(f"Empty data for {symbol}")
            return df
        except Exception as e:
            last_error = e
            logger.warning(f"[yfinance] attempt {attempt + 1}/{max_retries} for {symbol}: {e}")
    raise ValueError(f"yfinance failed for {symbol}: {last_error}")


# ===========================================================================
# 6. PYDANTIC MODELS
# ===========================================================================


class BulkChartsRequest(BaseModel):
    symbols: List[str]
    period: Optional[str] = "6mo"
    interval: Optional[str] = None


# ===========================================================================
# 7. FASTAPI ROUTES
# ===========================================================================


@app.get("/api/price/{symbol}")
def get_current_price(symbol: str):
    """Get current price for one symbol."""
    try:
        return fetch_price(symbol)
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))
    except Exception as e:
        logger.error(f"get_current_price({symbol}): {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/chart/{symbol}")
def get_chart_data(symbol: str, period: str = "6mo", interval: Optional[str] = None):
    """Get OHLCV chart for one symbol."""
    valid = ["1d", "5d", "1w", "1mo", "3mo", "6mo", "1y", "5y", "max"]
    if period not in valid:
        raise HTTPException(status_code=400, detail=f"Invalid period. Valid: {valid}")
    try:
        return fetch_chart(symbol, period, interval)
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))
    except Exception as e:
        logger.error(f"get_chart_data({symbol}): {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/prices")
def get_multiple_prices(symbols: List[str]):
    """Get prices for multiple symbols (legacy endpoint)."""
    return bulk_fetch_prices(symbols)


@app.post("/api/prices/bulk")
def bulk_fetch_prices(symbols: List[str]):
    """
    OPTIMIZED bulk price fetch.
    Fetches all symbols efficiently and returns complete data.
    """
    if not symbols:
        return {"data": {}}
    
    results = {}
    success_count = 0
    
    for symbol in symbols:
        sym_upper = symbol.upper().strip()
        try:
            data = fetch_price(symbol)
            results[sym_upper] = {
                "price": data["price"],
                "change": data.get("change", 0.0),
                "change_pct": data.get("change_pct", 0.0),
                "timestamp": data["timestamp"],
                "source": data.get("source", "unknown")
            }
            success_count += 1
        except Exception as e:
            results[sym_upper] = {"error": str(e)}
            logger.warning(f"[bulk] {sym_upper} failed: {e}")
    
    logger.info(f"[bulk prices] {success_count}/{len(symbols)} successful")
    return {"data": results, "success_count": success_count, "total": len(symbols)}


@app.post("/api/portfolio/value")
def get_portfolio_value(portfolio: Dict[str, float]):
    """
    Calculate total portfolio value.
    Body: {"AAPL": 10, "MSFT": 5}
    """
    if not portfolio:
        return {
            "total_value": 0.0,
            "breakdown": {},
            "timestamp": datetime.now().isoformat()
        }
    
    total = 0.0
    breakdown = {}
    
    for symbol, qty in portfolio.items():
        try:
            data = fetch_price(symbol)
            price = data["price"]
            value = price * qty
            total += value
            breakdown[symbol.upper()] = {
                "price": round(price, 6),
                "quantity": qty,
                "value": round(value, 2),
                "change": data.get("change", 0.0),
                "change_pct": data.get("change_pct", 0.0),
            }
        except Exception as e:
            breakdown[symbol.upper()] = {"error": str(e)}
    
    return {
        "total_value": round(total, 2),
        "breakdown": breakdown,
        "timestamp": datetime.now().isoformat(),
    }


@app.post("/api/portfolio/chart")
def get_portfolio_chart(
    portfolio: Dict[str, float],
    period: str = "6mo",
    interval: Optional[str] = None
):
    """
    Get combined portfolio value over time.
    """
    symbols = list(portfolio.keys())
    if not symbols:
        raise HTTPException(status_code=400, detail="Empty portfolio")
    
    period_lower = period.lower()
    
    # Try Stooq for each symbol
    try:
        frames = {}
        for sym in symbols:
            norm = normalize_symbol(sym)
            if norm['stooq']:
                d1, d2 = _date_range(period_lower)
                df = _fetch_stooq_historical(norm['stooq'], d1, d2)
                max_rows = _PERIOD_ROWS.get(period_lower, len(df))
                df = df.tail(max_rows).reset_index(drop=True)
                
                # Invert if needed
                if norm['needs_inversion']:
                    df['Close'] = 1.0 / df['Close']
                
                frames[sym.upper()] = df.set_index("Date")[["Close"]]
        
        # Combine all frames
        combined = pd.concat(frames, axis=1)
        combined.columns = [sym for sym, _ in combined.columns]
        combined = combined.ffill().dropna()
        
        # Calculate weighted sum
        combined["total"] = sum(
            combined[sym.upper()] * qty
            for sym, qty in portfolio.items()
            if sym.upper() in combined.columns
        )
        
        chart_data = [
            {"time": str(date.date()), "value": round(float(row["total"]), 2)}
            for date, row in combined.iterrows()
        ]
        
        logger.info(f"[Stooq] portfolio chart: {len(chart_data)} points")
        return {
            "portfolio": portfolio,
            "period": period,
            "interval": "1d",
            "data": chart_data,
            "source": "stooq"
        }
    except Exception as e:
        logger.warning(f"[Stooq] portfolio chart failed: {e}")
    
    # Fallback to yfinance
    try:
        interval = interval or _yf_optimal_interval(period_lower)
        df = yf.download(
            [s.upper() for s in symbols],
            period=_yf_normalize_period(period_lower),
            interval=interval,
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
            "period": period,
            "interval": interval,
            "data": chart_data,
            "source": "yfinance"
        }
    except Exception as e:
        logger.error(f"Portfolio chart error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/charts/bulk")
def bulk_fetch_charts(request: BulkChartsRequest):
    """Get chart data for multiple symbols."""
    symbols = [s.strip() for s in request.symbols if s and s.strip()]
    period = request.period or "6mo"
    interval = request.interval
    
    if not symbols:
        return {"data": {}}
    
    results = {}
    for sym in symbols:
        try:
            results[sym.upper()] = fetch_chart(sym, period, interval)
        except Exception as e:
            results[sym.upper()] = {"error": str(e)}
    
    return {"data": results}


@app.get("/api/health")
def health_check():
    """Health check endpoint."""
    return {
        "status": "healthy",
        "timestamp": datetime.now().isoformat(),
        "service": "Pricing Service v4.0",
        "data_sources": ["Stooq (primary)", "yfinance (fallback)"],
    }


@app.post("/api/cache/clear")
def clear_cache():
    """Clear all caches."""
    # Clear function caches
    _yf_ticker.cache_clear()
    
    # Clear yfinance disk cache
    deleted = 0
    skipped = []
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
    except Exception as e:
        logger.warning(f"Cache clear error: {e}")
    
    return {
        "status": "cache cleared",
        "yfinance_cache_dir": cache_dir,
        "files_deleted": deleted,
        "files_skipped": skipped,
    }


# ===========================================================================
# 8. ENTRY POINT
# ===========================================================================

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
