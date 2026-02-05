# ================================
# main.py
# ================================

import csv
import io
import logging
import requests
import pandas as pd
import yfinance as yf
from fastapi import FastAPI, Query
import threading
import time
from bs4 import BeautifulSoup
from datetime import datetime, timedelta

# ------------------------
# Logging
# ------------------------

logging.basicConfig(
    level=logging.INFO,
    format="%(levelname)s:%(name)s:%(message)s"
)
logger = logging.getLogger("main")

# ------------------------
# App
# ------------------------

app = FastAPI()

_http = requests.Session()
_http.headers.update({"User-Agent": "Mozilla/5.0"})

# ------------------------
# Rate Limiter
# ------------------------
_last_request_time = 0
_rate_limit_lock = threading.Lock()
MIN_REQUEST_INTERVAL = 15  # seconds

def _rate_limit():
    with _rate_limit_lock:
        global _last_request_time
        elapsed = time.monotonic() - _last_request_time
        if elapsed < MIN_REQUEST_INTERVAL:
            sleep_time = MIN_REQUEST_INTERVAL - elapsed
            logger.info(f"Rate limit hit. Sleeping for {sleep_time:.2f} seconds.")
            time.sleep(sleep_time)
        _last_request_time = time.monotonic()

# ------------------------
# Helpers
# ------------------------

def normalize_symbol(symbol: str) -> str:
    return symbol.strip().lower()

def yahoo_symbol(symbol: str) -> str:
    if symbol.endswith(".f"):
        return symbol.upper()
    if len(symbol) == 6 and symbol.isalpha():   # forex
        return symbol.upper() + "=X"
    return symbol.upper()

# ------------------------
# ST O O Q
# ------------------------

def stooq_symbol(symbol: str) -> str:
    s = normalize_symbol(symbol)

    if "-" in s:         # crypto
        return s
    if len(s) == 6 and s.isalpha():  # forex
        return s
    if "." not in s:
        return s + ".us"
    return s

# ------------------------
# Stooq CSV
# ------------------------

def fetch_stooq_csv(sym):
    url = f"https://stooq.com/q/l/?s={sym}&f=sd2t2ohlc&h&e=csv"
    r = _http.get(url, timeout=10)
    text = r.text

    reader = csv.DictReader(io.StringIO(text))
    rows = list(reader)

    if not rows:
        raise ValueError("Empty CSV")

    close = rows[-1]["Close"]
    return float(close)

# ------------------------
# Stooq HTML (debug)
# ------------------------

def fetch_stooq_html(sym):
    url = f"https://stooq.com/q/?s={sym}"
    r = _http.get(url, timeout=10)

    logger.warning(f"[HTML-DEBUG] URL={r.url}")
    logger.warning(f"[HTML-DEBUG] Status={r.status_code}")

    html = r.text[:600]
    logger.warning(f"[HTML-DEBUG] HEAD={html}")

    soup = BeautifulSoup(r.text, "lxml")

    selectors = [
        "span#aq_last_price",
        "span[id*=last]",
        "td.qr",
        "span.price"
    ]

    for s in selectors:
        tag = soup.select_one(s)
        logger.warning(f"[HTML-DEBUG] selector {s} => {bool(tag)}")
        if tag:
            val = tag.text.replace(",", "").strip()
            return float(val)

    raise ValueError("Price element not found")

# ------------------------
# Stooq Historical
# ------------------------

def fetch_stooq_historical(sym):
    end = datetime.now()
    start = end - timedelta(days=10)

    url = (
        f"https://stooq.com/q/d/l/?s={sym}"
        f"&d1={start.strftime('%Y%m%d')}"
        f"&d2={end.strftime('%Y%m%d')}"
        f"&i=d"
    )

    df = pd.read_csv(url)

    if df.empty:
        raise ValueError("No historical data")

    return float(df.iloc[-1]["Close"])

# ------------------------
# Stooq wrapper
# ------------------------

def fetch_stooq(symbol):
    sym = stooq_symbol(symbol)

    try:
        return fetch_stooq_csv(sym)
    except Exception as e:
        logger.warning(f"[Stooq-CSV] {symbol} failed: {e}")

    try:
        return fetch_stooq_html(sym)
    except Exception as e:
        logger.warning(f"[Stooq] {symbol} failed: {e}")

    try:
        return fetch_stooq_historical(sym)
    except Exception as e:
        logger.warning(f"[Stooq-HIST] {symbol} failed: {e}")

    raise ValueError("Stooq all methods failed")

# ------------------------
# Yahoo
# ------------------------

def fetch_yahoo(symbol):
    sym = yahoo_symbol(symbol)
    try:
        data = yf.download(sym, period="2d", progress=False, threads=True)
        
        if data.empty:
            raise ValueError(f"No Yahoo data for {sym}")
        
        # Handle multi-index DataFrame (when downloading multiple symbols)
        if isinstance(data.columns, pd.MultiIndex):
            # If multiple symbols, take the first one
            close_col = data["Close"]
            if isinstance(close_col, pd.DataFrame):
                close_col = close_col.iloc[:, 0]
            close_value = close_col.iloc[-1]
        else:
            close_value = data["Close"].iloc[-1]
        
        # Handle Series vs scalar
        if isinstance(close_value, pd.Series):
            close_value = close_value.iloc[-1]
        
        price = float(close_value)
        return price
    except Exception as e:
        logger.warning(f"[yfinance] Error fetching {sym}: {e}")
        raise

# ------------------------
# Unified fetch
# ------------------------

def fetch_price(symbol):
    # Try Stooq first (all methods: CSV, HTML, Historical)
    stooq_success = False
    try:
        price = fetch_stooq(symbol)
        logger.info(f"[Stooq] {symbol}: {price}")
        return price
    except ValueError as e:
        # ValueError from fetch_stooq means all Stooq methods failed
        logger.info(f"[Stooq] {symbol}: All Stooq methods failed, trying yfinance...")
    except Exception as e:
        # Other exceptions from Stooq
        logger.warning(f"[Stooq] {symbol} unexpected error: {e}, trying yfinance...")

    # Fallback to yfinance when Stooq fails
    try:
        logger.debug(f"[yfinance] Attempting to fetch {symbol} via yfinance...")
        price = fetch_yahoo(symbol)
        logger.info(f"[yfinance] {symbol}: {price}")
        return price
    except Exception as e:
        logger.warning(f"[yfinance] {symbol} failed: {e}")

    # Both Stooq and yfinance failed
    raise ValueError(f"All sources failed for {symbol}")

# ------------------------
# Unified OPEN price fetch
# ------------------------

def fetch_open_price(symbol):
    # Try Stooq Historical first (best for open)
    sym = stooq_symbol(symbol)

    try:
        end = datetime.now()
        start = end - timedelta(days=10)

        url = (
            f"https://stooq.com/q/d/l/?s={sym}"
            f"&d1={start.strftime('%Y%m%d')}"
            f"&d2={end.strftime('%Y%m%d')}"
            f"&i=d"
        )

        df = pd.read_csv(url)

        if not df.empty:
            open_price = float(df.iloc[-1]["Open"])
            logger.info(f"[Stooq-OPEN] {symbol}: {open_price}")
            return open_price
    except Exception as e:
        logger.warning(f"[Stooq-OPEN] {symbol} failed: {e}")

    # Fallback → Yahoo
    try:
        sym_y = yahoo_symbol(symbol)
        df = yf.download(sym_y, period="2d", progress=False, threads=True)

        if df.empty:
            raise ValueError(f"No Yahoo data for {sym_y}")
        
        # Handle multi-index DataFrame (when downloading multiple symbols)
        if isinstance(df.columns, pd.MultiIndex):
            # If multiple symbols, take the first one
            open_col = df["Open"]
            if isinstance(open_col, pd.DataFrame):
                open_col = open_col.iloc[:, 0]
            open_value = open_col.iloc[-1]
        else:
            open_value = df["Open"].iloc[-1]
        
        # Handle Series vs scalar
        if isinstance(open_value, pd.Series):
            open_value = open_value.iloc[-1]
        
        open_price = float(open_value)
        logger.info(f"[yfinance-OPEN] {symbol}: {open_price}")
        return open_price
    except Exception as e:
        logger.warning(f"[yfinance-OPEN] {symbol} failed: {e}")
        raise

    raise ValueError("All OPEN sources failed")



# ------------------------
# API
# ------------------------

@app.get("/api/health")
def health():
    return {"status": "ok"}

@app.post("/api/prices/bulk")
def bulk_prices(symbols: list[str]):
    out = {}
    success = 0
    _rate_limit()
    for s in symbols:
        try:
            out[s] = fetch_price(s)
            success += 1
        except Exception as e:
            out[s] = None
            logger.warning(f"[bulk] {s} failed: {e}")

    logger.info(f"[bulk prices] {success}/{len(symbols)} successful")
    return out

@app.post("/api/portfolio/chart")
def portfolio_chart(
    symbol: str,
    period: str = Query("1d"),
    interval: str = Query("1d")
):
    sym = yahoo_symbol(symbol)
    df = yf.download(sym, period=period, interval=interval, progress=False)

    if df.empty:
        raise ValueError("No chart data")

    return {
        "dates": df.index.astype(str).tolist(),
        "close": df["Close"].tolist()
    }

@app.post("/api/prices/bulk/open")
def bulk_open_prices(symbols: list[str]):
    out = {}
    success = 0
    _rate_limit()
    for s in symbols:
        try:
            out[s] = fetch_open_price(s)
            success += 1
        except Exception as e:
            out[s] = None
            logger.warning(f"[bulk-open] {s} failed: {e}")

    logger.info(f"[bulk open prices] {success}/{len(symbols)} successful")
    return out


# ================================
# END FILE
# ================================
