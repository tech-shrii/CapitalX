from fastapi import FastAPI, HTTPException
from fastapi.responses import JSONResponse
from functools import lru_cache
import yfinance as yf
from datetime import datetime, timedelta
import asyncio
from typing import List, Dict, Optional
import json
import time
import logging
import os
import pandas as pd

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Configure yfinance cache location (persistent cache)
cache_dir = os.path.join(os.path.expanduser("~"), ".cache", "py-yfinance")
os.makedirs(cache_dir, exist_ok=True)
try:
    yf.set_tz_cache_location(cache_dir)
    logger.info(f"yfinance cache location set to: {cache_dir}")
except Exception as e:
    logger.warning(f"Could not set cache location: {e}")

app = FastAPI(title="Pricing Service", version="1.0.0")

# Helper function to batch download multiple symbols at once
def batch_download_prices(symbols: List[str], period: str = "1d", max_retries: int = 2):
    """Batch download prices for multiple symbols using yf.download() - much more efficient"""
    if not symbols:
        return {}
    
    symbols_clean = [s.upper().strip() for s in symbols if s and s.strip()]
    if not symbols_clean:
        return {}
    
    last_error = None
    for attempt in range(max_retries):
        try:
            if attempt > 0:
                delay = min(2 ** attempt, 10)
                logger.info(f"Retrying batch download (attempt {attempt + 1}/{max_retries}) after {delay}s")
                time.sleep(delay)
            
            # Use yf.download() for batch fetching - much faster than individual calls
            logger.info(f"Batch downloading {len(symbols_clean)} symbols: {', '.join(symbols_clean[:5])}{'...' if len(symbols_clean) > 5 else ''}")
            
            # Download with group_by='ticker' to get separate DataFrames per symbol
            df = yf.download(symbols_clean, period=period, progress=False, group_by='ticker', threads=True, show_errors=False)
            
            if df.empty:
                raise ValueError("Empty DataFrame returned from batch download")
            
            results = {}
            
            # yf.download() with group_by='ticker' returns a dict of DataFrames when multiple symbols
            if isinstance(df, dict):
                # Multiple symbols - df is a dict {symbol: DataFrame}
                for symbol in symbols_clean:
                    if symbol in df and not df[symbol].empty:
                        symbol_df = df[symbol]
                        if 'Close' in symbol_df.columns:
                            results[symbol] = {
                                "price": float(symbol_df['Close'].iloc[-1]),
                                "timestamp": symbol_df.index[-1].isoformat() if hasattr(symbol_df.index[-1], 'isoformat') else str(symbol_df.index[-1])
                            }
            elif isinstance(df.columns, pd.MultiIndex):
                # MultiIndex columns - each symbol has its own column group
                for symbol in symbols_clean:
                    try:
                        # Try to extract symbol data from MultiIndex
                        if symbol in [col[1] for col in df.columns if len(col) > 1]:
                            symbol_cols = df.xs(symbol, level=1, axis=1, drop_level=False)
                            if not symbol_cols.empty and 'Close' in symbol_cols.columns:
                                close_col = symbol_cols['Close'] if isinstance(symbol_cols['Close'], pd.Series) else symbol_cols['Close'].iloc[:, 0]
                                results[symbol] = {
                                    "price": float(close_col.iloc[-1]),
                                    "timestamp": df.index[-1].isoformat() if hasattr(df.index[-1], 'isoformat') else str(df.index[-1])
                                }
                    except Exception as e:
                        logger.debug(f"Could not extract {symbol} from MultiIndex: {e}")
                        continue
            else:
                # Single symbol or flat structure
                if 'Close' in df.columns:
                    # Single symbol case
                    if len(symbols_clean) == 1:
                        results[symbols_clean[0]] = {
                            "price": float(df['Close'].iloc[-1]),
                            "timestamp": df.index[-1].isoformat() if hasattr(df.index[-1], 'isoformat') else str(df.index[-1])
                        }
                    else:
                        # Multiple symbols but returned as flat DataFrame - try to parse
                        for symbol in symbols_clean:
                            # Look for columns that might contain this symbol
                            close_cols = [col for col in df.columns if 'Close' in str(col) and symbol in str(col)]
                            if close_cols:
                                results[symbol] = {
                                    "price": float(df[close_cols[0]].iloc[-1]),
                                    "timestamp": df.index[-1].isoformat() if hasattr(df.index[-1], 'isoformat') else str(df.index[-1])
                                }
            
            logger.info(f"Successfully batch downloaded {len(results)}/{len(symbols_clean)} symbols")
            return results
            
        except Exception as e:
            error_msg = str(e)
            last_error = error_msg
            logger.warning(f"Batch download failed (attempt {attempt + 1}/{max_retries}): {error_msg}")
            if attempt < max_retries - 1:
                continue
    
    logger.error(f"Batch download failed after {max_retries} attempts: {last_error}")
    # Fallback: try individual fetches for critical symbols
    logger.info("Falling back to individual fetches...")
    results = {}
    for symbol in symbols_clean[:10]:  # Limit fallback to first 10 to avoid timeout
        try:
            data = fetch_ticker_data(symbol, period=period, interval="1d", max_retries=1)
            results[symbol] = {
                "price": float(data["Close"].iloc[-1]),
                "timestamp": data.index[-1].isoformat() if hasattr(data.index[-1], 'isoformat') else str(data.index[-1])
            }
            time.sleep(0.3)
        except:
            pass
    return results

# Helper function to fetch ticker data with retries (for individual symbols or charts)
def fetch_ticker_data(symbol: str, period: str = "1d", interval: str = "1d", max_retries: int = 3):
    """Fetch ticker data with retry logic and error handling"""
    last_error = None
    
    for attempt in range(max_retries):
        try:
            # Add delay to avoid rate limiting (especially on retries)
            if attempt > 0:
                delay = min(2 ** attempt, 10)  # Exponential backoff: 2s, 4s, 8s, max 10s
                logger.info(f"Retrying {symbol} (attempt {attempt + 1}/{max_retries}) after {delay}s delay")
                time.sleep(delay)
            else:
                # Small initial delay to avoid hammering the API
                time.sleep(0.2)
            
            ticker = yf.Ticker(symbol)
            
            # Fetch history data with timeout handling
            try:
                if interval == "1d" and period == "1d":
                    data = ticker.history(period=period, timeout=10)
                else:
                    data = ticker.history(period=period, interval=interval, timeout=10)
            except Exception as hist_error:
                # Try without timeout parameter (older yfinance versions)
                logger.debug(f"History with timeout failed, trying without: {hist_error}")
                if interval == "1d" and period == "1d":
                    data = ticker.history(period=period)
                else:
                    data = ticker.history(period=period, interval=interval)
            
            if data is None or data.empty:
                raise ValueError(f"No data returned for {symbol} (empty DataFrame)")
            
            # Validate we got actual price data
            if "Close" not in data.columns:
                raise ValueError(f"Invalid data format for {symbol} (missing Close column)")
            
            logger.info(f"Successfully fetched data for {symbol} ({len(data)} rows)")
            return data
            
        except ValueError as ve:
            # Re-raise ValueError immediately (these are expected errors)
            raise
        except Exception as e:
            error_msg = str(e)
            last_error = error_msg
            logger.warning(f"Failed to get ticker '{symbol}' (attempt {attempt + 1}/{max_retries}): {error_msg}")
            
            # If it's a JSON decode error or network issue, retry
            if "Expecting value" in error_msg or "JSON" in error_msg or "timeout" in error_msg.lower():
                if attempt < max_retries - 1:
                    continue
            
            # For other errors, don't retry
            if attempt == max_retries - 1:
                break
    
    # All retries exhausted
    raise ValueError(f"Failed to fetch data for {symbol} after {max_retries} attempts. Last error: {last_error}")

# In-memory cache with expiry
class CacheWithExpiry:
    def __init__(self):
        self.cache = {}
        self.expiry = {}
    
    def set(self, key: str, value, ttl: int = 3600):
        """Set cache with TTL in seconds"""
        self.cache[key] = value
        self.expiry[key] = datetime.now() + timedelta(seconds=ttl)
    
    def get(self, key: str):
        """Get from cache if not expired"""
        if key not in self.cache:
            return None
        if datetime.now() > self.expiry.get(key, datetime.now()):
            del self.cache[key]
            return None
        return self.cache[key]
    
    def clear_expired(self):
        """Remove expired entries"""
        now = datetime.now()
        expired = [k for k, v in self.expiry.items() if now > v]
        for k in expired:
            self.cache.pop(k, None)
            self.expiry.pop(k, None)

cache = CacheWithExpiry()

# ============================================
# 1️⃣ GET CURRENT PRICE
# ============================================

@app.get("/api/price/{symbol}")
async def get_current_price(symbol: str):
    """Get current price for a symbol"""
    try:
        cache_key = f"price_{symbol.upper()}"
        cached = cache.get(cache_key)
        if cached:
            return cached
        
        data = fetch_ticker_data(symbol, period="1d", interval="1d")
        
        latest_price = float(data["Close"].iloc[-1])
        timestamp = data.index[-1].isoformat()
        
        response = {
            "symbol": symbol.upper(),
            "price": latest_price,
            "timestamp": timestamp,
            "currency": "USD"
        }
        
        cache.set(cache_key, response, ttl=60)  # Cache for 60 seconds
        return response
    
    except ValueError as e:
        logger.error(f"Price fetch error for {symbol}: {e}")
        raise HTTPException(status_code=404, detail=str(e))
    except Exception as e:
        logger.error(f"Unexpected error for {symbol}: {e}")
        raise HTTPException(status_code=500, detail=f"Error fetching price for {symbol}: {str(e)}")

# ============================================
# 2️⃣ GET CHART DATA (OHLC)
# ============================================

@app.get("/api/chart/{symbol}")
async def get_chart_data(symbol: str, period: str = "1mo", interval: str = "1d"):
    """Get chart data (OHLC) for a symbol"""
    try:
        # Validate periods/intervals
        valid_periods = ["1d", "5d", "1mo", "3mo", "6mo", "1y", "5y", "max"]
        valid_intervals = ["1m", "5m", "15m", "30m", "1h", "1d", "1wk"]
        
        if period not in valid_periods or interval not in valid_intervals:
            raise HTTPException(status_code=400, detail="Invalid period or interval")
        
        cache_key = f"chart_{symbol.upper()}_{period}_{interval}"
        cached = cache.get(cache_key)
        if cached:
            return cached
        
        df = fetch_ticker_data(symbol, period=period, interval=interval)
        
        chart_data = []
        for index, row in df.iterrows():
            chart_data.append({
                "time": index.isoformat(),
                "open": round(float(row["Open"]), 2),
                "high": round(float(row["High"]), 2),
                "low": round(float(row["Low"]), 2),
                "close": round(float(row["Close"]), 2),
                "volume": int(row["Volume"])
            })
        
        response = {
            "symbol": symbol.upper(),
            "period": period,
            "interval": interval,
            "data": chart_data
        }
        
        ttl = 3600 if interval == "1d" else 300  # Cache daily data longer
        cache.set(cache_key, response, ttl=ttl)
        return response
    
    except ValueError as e:
        logger.error(f"Chart fetch error for {symbol}: {e}")
        raise HTTPException(status_code=404, detail=str(e))
    except Exception as e:
        logger.error(f"Unexpected chart error for {symbol}: {e}")
        raise HTTPException(status_code=500, detail=f"Error fetching chart for {symbol}: {str(e)}")

# ============================================
# 3️⃣ GET MULTIPLE PRICES (Portfolio)
# ============================================

@app.post("/api/prices")
async def get_multiple_prices(symbols: List[str]):
    """Get current prices for multiple symbols - uses batch download for efficiency"""
    try:
        if not symbols:
            return {"data": {}}
        
        # Use batch download for better performance
        batch_results = batch_download_prices(symbols, period="1d")
        
        # Format results
        results = {}
        for symbol in symbols:
            symbol_upper = symbol.upper().strip()
            if symbol_upper in batch_results:
                results[symbol_upper] = batch_results[symbol_upper]
            else:
                # Fallback to individual fetch if batch failed for this symbol
                try:
                    data = fetch_ticker_data(symbol_upper, period="1d", interval="1d")
                    results[symbol_upper] = {
                        "price": float(data["Close"].iloc[-1]),
                        "timestamp": data.index[-1].isoformat() if hasattr(data.index[-1], 'isoformat') else str(data.index[-1])
                    }
                except Exception as e:
                    logger.warning(f"Failed to fetch price for {symbol}: {e}")
                    results[symbol_upper] = {"error": f"Failed to fetch: {str(e)}"}
        
        return {"data": results}
    except Exception as e:
        logger.error(f"Error in get_multiple_prices: {e}")
        raise HTTPException(status_code=500, detail=str(e))

# ============================================
# 4️⃣ GET COMBINED PORTFOLIO VALUE
# ============================================

@app.post("/api/portfolio/value")
async def get_portfolio_value(portfolio: Dict[str, float]):
    """
    Get total portfolio value and breakdown
    portfolio: {"AAPL": 10, "MSFT": 5} (quantity per symbol)
    Uses batch download for efficiency
    """
    try:
        cache_key = f"portfolio_value_{hash(frozenset(portfolio.items()))}"
        cached = cache.get(cache_key)
        if cached:
            return cached
        
        symbols = list(portfolio.keys())
        if not symbols:
            return {
                "total_value": 0.0,
                "breakdown": {},
                "timestamp": datetime.now().isoformat()
            }
        
        # Use batch download for all symbols at once
        batch_results = batch_download_prices(symbols, period="1d")
        
        total_value = 0.0
        breakdown = {}
        
        for symbol, quantity in portfolio.items():
            symbol_upper = symbol.upper().strip()
            if symbol_upper in batch_results:
                price = batch_results[symbol_upper]["price"]
                value = price * quantity
                total_value += value
                breakdown[symbol_upper] = {
                    "price": price,
                    "quantity": quantity,
                    "value": round(value, 2)
                }
            else:
                # Fallback to individual fetch
                try:
                    data = fetch_ticker_data(symbol_upper, period="1d", interval="1d")
                    price = float(data["Close"].iloc[-1])
                    value = price * quantity
                    total_value += value
                    breakdown[symbol_upper] = {
                        "price": price,
                        "quantity": quantity,
                        "value": round(value, 2)
                    }
                except Exception as e:
                    logger.warning(f"Failed to fetch price for {symbol}: {e}")
                    breakdown[symbol_upper] = {"error": f"Failed to fetch: {str(e)}"}
        
        response = {
            "total_value": round(total_value, 2),
            "breakdown": breakdown,
            "timestamp": datetime.now().isoformat()
        }
        
        cache.set(cache_key, response, ttl=60)
        return response
    
    except Exception as e:
        logger.error(f"Error in get_portfolio_value: {e}")
        raise HTTPException(status_code=500, detail=str(e))

# ============================================
# 5️⃣ GET COMBINED HISTORICAL CHART
# ============================================

@app.post("/api/portfolio/chart")
async def get_portfolio_chart(portfolio: Dict[str, float], period: str = "1mo", interval: str = "1d"):
    """
    Get combined portfolio performance chart
    portfolio: {"AAPL": 10, "MSFT": 5}
    """
    try:
        cache_key = f"portfolio_chart_{hash(frozenset(portfolio.items()))}_{period}_{interval}"
        cached = cache.get(cache_key)
        if cached:
            return cached
        
        # Fetch all symbol data
        all_data = {}
        for symbol in portfolio.keys():
            try:
                df = fetch_ticker_data(symbol, period=period, interval=interval)
                all_data[symbol.upper()] = df
                # Small delay between requests
                time.sleep(0.5)
            except Exception as e:
                logger.warning(f"Failed to fetch chart data for {symbol}: {e}")
                # Continue with other symbols
        
        if not all_data:
            raise HTTPException(status_code=404, detail="No data found for portfolio")
        
        # Combine portfolio values over time
        combined_data = []
        # Get common index (timestamps)
        common_dates = all_data[list(all_data.keys())[0]].index
        
        for date in common_dates:
            total_value = 0.0
            for symbol, quantity in portfolio.items():
                symbol_upper = symbol.upper()
                if symbol_upper in all_data and date in all_data[symbol_upper].index:
                    price = float(all_data[symbol_upper].loc[date, "Close"])
                    total_value += price * quantity
            
            combined_data.append({
                "time": date.isoformat(),
                "value": round(total_value, 2)
            })
        
        response = {
            "portfolio": portfolio,
            "period": period,
            "interval": interval,
            "data": combined_data
        }
        
        ttl = 3600 if interval == "1d" else 300
        cache.set(cache_key, response, ttl=ttl)
        return response
    
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# ============================================
# 6️⃣ BULK FETCH ALL SYMBOLS (for login/dashboard)
# ============================================

@app.post("/api/prices/bulk")
async def bulk_fetch_prices(symbols: List[str]):
    """
    Bulk fetch prices for all symbols at once using batch download
    Optimized for fetching all client stocks on login
    Returns: {"data": {"SYMBOL": {"price": float, "timestamp": str}, ...}}
    """
    try:
        if not symbols:
            return {"data": {}}
        
        logger.info(f"Bulk fetching prices for {len(symbols)} symbols")
        
        # Use batch download - much more efficient
        batch_results = batch_download_prices(symbols, period="1d")
        
        # Format response
        results = {}
        for symbol in symbols:
            symbol_upper = symbol.upper().strip()
            if symbol_upper in batch_results:
                results[symbol_upper] = batch_results[symbol_upper]
            else:
                results[symbol_upper] = {"error": "Failed to fetch"}
        
        logger.info(f"Bulk fetch completed: {len([r for r in results.values() if 'error' not in r])}/{len(symbols)} successful")
        return {"data": results}
        
    except Exception as e:
        logger.error(f"Error in bulk_fetch_prices: {e}")
        raise HTTPException(status_code=500, detail=str(e))

# ============================================
# 7️⃣ HEALTH CHECK
# ============================================

@app.get("/api/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "timestamp": datetime.now().isoformat(),
        "service": "Pricing Service"
    }

# ============================================
# 8️⃣ CLEAR CACHE
# ============================================

@app.post("/api/cache/clear")
async def clear_cache():
    """Clear expired cache entries"""
    cache.clear_expired()
    return {"status": "cache cleared"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
