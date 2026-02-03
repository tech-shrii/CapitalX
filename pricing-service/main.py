from fastapi import FastAPI, HTTPException
from fastapi.responses import JSONResponse
from functools import lru_cache
import yfinance as yf
from datetime import datetime, timedelta
import asyncio
from typing import List, Dict, Optional, Any
from pydantic import BaseModel
import json
import time
import logging
import os
import pandas as pd

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Configure yfinance cache location (persistent cache)
# Note: yfinance 1.1.0+ uses curl_cffi internally and handles its own session management
# We should NOT pass a custom session as it conflicts with curl_cffi
cache_dir = os.path.join(os.path.expanduser("~"), ".cache", "py-yfinance")
os.makedirs(cache_dir, exist_ok=True)
try:
    yf.set_tz_cache_location(cache_dir)
    logger.info(f"yfinance cache location set to: {cache_dir}")
except Exception as e:
    logger.warning(f"Could not set cache location: {e}")

app = FastAPI(title="Pricing Service", version="1.0.0")

# Pydantic models for request/response
class BulkChartsRequest(BaseModel):
    symbols: List[str]
    period: Optional[str] = "6mo"
    interval: Optional[str] = None

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
            # Note: Do NOT pass session parameter - yfinance handles its own session with curl_cffi
            logger.info(f"Batch downloading {len(symbols_clean)} symbols: {', '.join(symbols_clean[:5])}{'...' if len(symbols_clean) > 5 else ''}")
            
            # Download with group_by='ticker' to get separate DataFrames per symbol
            df = yf.download(symbols_clean, period=period, progress=False, threads=True)
            
            if df.empty:
                raise ValueError("Empty DataFrame returned from batch download")
            
            results = {}
            if isinstance(df.columns, pd.MultiIndex):
                for symbol in symbols_clean:
                    try:
                        if ('Close', symbol) in df.columns:
                            price = df[('Close', symbol)].iloc[-1]
                            if pd.notna(price):
                                results[symbol] = {
                                    "price": float(price),
                                    "timestamp": df.index[-1].isoformat() if hasattr(df.index[-1], 'isoformat') else str(df.index[-1])
                                }
                    except Exception as e:
                        logger.debug(f"Could not extract {symbol} from MultiIndex: {e}")
                        continue
            else:
                # Single symbol case
                if 'Close' in df.columns and len(symbols_clean) == 1:
                    results[symbols_clean[0]] = {
                        "price": float(df['Close'].iloc[-1]),
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

@lru_cache(maxsize=128)
def get_ticker(symbol: str) -> yf.Ticker:
    """Cached function to get a yf.Ticker object"""
    # Note: Do NOT pass session parameter - yfinance handles its own session with curl_cffi
    return yf.Ticker(symbol)

# Helper function to get optimal interval based on period (lean data approach)
def get_optimal_interval(period: str) -> str:
    """Get optimal interval for a given period to minimize data points while maintaining clarity"""
    period_lower = period.lower()
    interval_map = {
        "1d": "15m",      # ~26 data points for 1 day
        "5d": "60m",      # ~35 data points for 5 days
        "1w": "60m",      # ~35 data points for 1 week
        "1mo": "1d",      # ~21 data points for 1 month
        "3mo": "1d",      # ~63 data points for 3 months
        "6mo": "1wk",     # ~26 data points for 6 months (default)
        "1y": "1wk",      # ~52 data points for 1 year
        "5y": "1mo",      # ~60 data points for 5 years
        "max": "1mo"      # ~60 data points for max period
    }
    return interval_map.get(period_lower, "1wk")  # Default to 1wk (6mo default) if period not found

# Helper function to normalize period for yfinance (converts 1w to 5d, etc.)
def normalize_period(period: str) -> str:
    """Normalize period string for yfinance compatibility"""
    period_lower = period.lower()
    period_map = {
        "1w": "5d",      # yfinance uses 5d for 1 week
        "1mo": "1mo",    # yfinance uses 1mo for 1 month
        "6mo": "6mo",    # yfinance uses 6mo for 6 months
        "1y": "1y",      # yfinance uses 1y for 1 year
        "5y": "5y"       # yfinance uses 5y for 5 years
    }
    return period_map.get(period_lower, period)  # Return as-is if not in map

# Helper function to fetch ticker data with retries (for individual symbols or charts)
def fetch_ticker_data(symbol: str, period: str = "1d", interval: str = None, max_retries: int = 3):
    """Fetch ticker data with retry logic and error handling"""
    # Use optimal interval if not provided
    if interval is None:
        interval = get_optimal_interval(period)
    
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
            
            ticker = get_ticker(symbol)
            
            # Normalize period for yfinance compatibility
            normalized_period = normalize_period(period)
            
            # Fetch history data with optimal interval
            data = ticker.history(period=normalized_period, interval=interval)
            
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



# ============================================
# 1️⃣ GET CURRENT PRICE
# ============================================

@app.get("/api/price/{symbol}")
async def get_current_price(symbol: str):
    """Get current price for a symbol using ticker.info for latest data"""
    try:
        ticker = get_ticker(symbol)
        
        # Use ticker.info for real-time/latest price data
        info = ticker.info
        
        # Try multiple price keys as they can vary
        current_price = info.get('currentPrice') or info.get('regularMarketPrice') or info.get('previousClose')
        
        if current_price is None:
            # Fallback to history if info doesn't have price
            logger.warning(f"No price in info for {symbol}, falling back to history")
            data = fetch_ticker_data(symbol, period="1d", interval="15m")
            current_price = float(data["Close"].iloc[-1])
            timestamp = data.index[-1].isoformat()
        else:
            # Use info timestamp if available
            timestamp = info.get('regularMarketTime') or datetime.now().isoformat()
            if isinstance(timestamp, (int, float)):
                # Convert Unix timestamp to ISO format
                timestamp = datetime.fromtimestamp(timestamp).isoformat()
        
        response = {
            "symbol": symbol.upper(),
            "price": float(current_price),
            "timestamp": timestamp,
            "currency": info.get('currency', 'USD')
        }
        
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
async def get_chart_data(symbol: str, period: str = "6mo", interval: str = None):
    """Get chart data (OHLC) for a symbol with optimal intervals"""
    try:
        # Validate periods
        valid_periods = ["1d", "1w", "1mo", "6mo", "1y", "5y"]
        
        if period not in valid_periods:
            raise HTTPException(status_code=400, detail=f"Invalid period. Valid options: {valid_periods}")
        
        # Use optimal interval if not provided
        if interval is None:
            interval = get_optimal_interval(period)
        
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
        
        return response
    
    except Exception as e:
        logger.error(f"Error in get_portfolio_value: {e}")
        raise HTTPException(status_code=500, detail=str(e))

# ============================================
# 5️⃣ GET COMBINED HISTORICAL CHART
# ============================================

@app.post("/api/portfolio/chart")
async def get_portfolio_chart(portfolio: Dict[str, float], period: str = "6mo", interval: str = None):
    """
    Get combined portfolio performance chart
    portfolio: {"AAPL": 10, "MSFT": 5}
    Default period: 6mo with optimal intervals for lean data
    """
    try:
        symbols = list(portfolio.keys())
        if not symbols:
            raise HTTPException(status_code=404, detail="No symbols in portfolio")

        # Use optimal interval if not provided
        if interval is None:
            interval = get_optimal_interval(period)
        
        logger.info(f"Fetching portfolio chart for {len(symbols)} symbols, period={period}, interval={interval}")

        # Fetch all historical data in one go
        # Note: Do NOT pass session parameter - yfinance handles its own session with curl_cffi
        df = yf.download(symbols, period=period, interval=interval, progress=False, threads=True)
        
        if df.empty:
            raise HTTPException(status_code=404, detail="No data found for portfolio")

        # Calculate portfolio value over time
        portfolio_values = pd.DataFrame(index=df.index)
        for symbol, quantity in portfolio.items():
            symbol_upper = symbol.upper()
            close_col = ('Close', symbol_upper)
            if close_col in df.columns:
                portfolio_values[symbol_upper] = df[close_col] * quantity

        # Sum up the values of all assets
        portfolio_values['total'] = portfolio_values.sum(axis=1)

        combined_data = []
        for date, row in portfolio_values.iterrows():
            combined_data.append({
                "time": date.isoformat(),
                "value": round(row['total'], 2)
            })

        response = {
            "portfolio": portfolio,
            "period": period,
            "interval": interval,
            "data": combined_data
        }
        
        return response
    
    except Exception as e:
        logger.error(f"Error in get_portfolio_chart: {e}")
        raise HTTPException(status_code=500, detail=str(e))

# ============================================
# 6️⃣ BULK FETCH CHARTS (for dashboard)
# ============================================

@app.post("/api/charts/bulk")
async def bulk_fetch_charts(request: BulkChartsRequest):
    """
    Bulk fetch chart data for multiple symbols efficiently
    Request: {
        "symbols": ["AAPL", "MSFT", ...],
        "period": "6mo" (default),
        "interval": "1wk" (optional, will use optimal if not provided)
    }
    Returns: {
        "data": {
            "SYMBOL": {
                "symbol": "SYMBOL",
                "period": "6mo",
                "interval": "1wk",
                "data": [{"time": "...", "open": ..., "high": ..., "low": ..., "close": ..., "volume": ...}, ...]
            },
            ...
        }
    }
    """
    try:
        symbols = request.symbols
        period = request.period or "6mo"
        interval = request.interval
        
        if not symbols:
            return {"data": {}}
        
        # Use optimal interval if not provided
        if interval is None:
            interval = get_optimal_interval(period)
        
        logger.info(f"Bulk fetching charts for {len(symbols)} symbols, period={period}, interval={interval}")
        
        # Normalize period for yfinance compatibility
        normalized_period = normalize_period(period)
        
        # Fetch all symbols at once using batch download
        symbols_clean = [s.upper().strip() for s in symbols if s and s.strip()]
        
        try:
            df = yf.download(symbols_clean, period=normalized_period, interval=interval, progress=False, threads=True)
            
            if df.empty:
                raise ValueError("Empty DataFrame returned from batch download")
            
            results = {}
            
            # Process MultiIndex DataFrame (multiple symbols)
            if isinstance(df.columns, pd.MultiIndex):
                for symbol in symbols_clean:
                    try:
                        chart_data = []
                        if ('Close', symbol) in df.columns:
                            for index in df.index:
                                chart_data.append({
                                    "time": index.isoformat() if hasattr(index, 'isoformat') else str(index),
                                    "open": round(float(df[('Open', symbol)].loc[index]), 2),
                                    "high": round(float(df[('High', symbol)].loc[index]), 2),
                                    "low": round(float(df[('Low', symbol)].loc[index]), 2),
                                    "close": round(float(df[('Close', symbol)].loc[index]), 2),
                                    "volume": int(df[('Volume', symbol)].loc[index]) if ('Volume', symbol) in df.columns else 0
                                })
                            
                            results[symbol] = {
                                "symbol": symbol,
                                "period": period,
                                "interval": interval,
                                "data": chart_data
                            }
                    except Exception as e:
                        logger.warning(f"Could not extract chart data for {symbol}: {e}")
                        results[symbol] = {"error": f"Failed to fetch: {str(e)}"}
            else:
                # Single symbol case
                if len(symbols_clean) == 1:
                    symbol = symbols_clean[0]
                    chart_data = []
                    if 'Close' in df.columns:
                        for index in df.index:
                            chart_data.append({
                                "time": index.isoformat() if hasattr(index, 'isoformat') else str(index),
                                "open": round(float(df['Open'].loc[index]), 2),
                                "high": round(float(df['High'].loc[index]), 2),
                                "low": round(float(df['Low'].loc[index]), 2),
                                "close": round(float(df['Close'].loc[index]), 2),
                                "volume": int(df['Volume'].loc[index]) if 'Volume' in df.columns else 0
                            })
                        
                        results[symbol] = {
                            "symbol": symbol,
                            "period": period,
                            "interval": interval,
                            "data": chart_data
                        }
            
            logger.info(f"Bulk charts fetch completed: {len([r for r in results.values() if 'error' not in r])}/{len(symbols)} successful")
            return {"data": results}
            
        except Exception as e:
            logger.warning(f"Batch chart download failed: {e}, falling back to individual fetches")
            # Fallback to individual fetches
            results = {}
            for symbol in symbols_clean[:20]:  # Limit to first 20 to avoid timeout
                try:
                    data = fetch_ticker_data(symbol, period=period, interval=interval, max_retries=1)
                    chart_data = []
                    for index, row in data.iterrows():
                        chart_data.append({
                            "time": index.isoformat() if hasattr(index, 'isoformat') else str(index),
                            "open": round(float(row["Open"]), 2),
                            "high": round(float(row["High"]), 2),
                            "low": round(float(row["Low"]), 2),
                            "close": round(float(row["Close"]), 2),
                            "volume": int(row["Volume"]) if "Volume" in row else 0
                        })
                    
                    results[symbol] = {
                        "symbol": symbol,
                        "period": period,
                        "interval": interval,
                        "data": chart_data
                    }
                    time.sleep(0.2)  # Small delay between requests
                except Exception as e:
                    logger.warning(f"Failed to fetch chart for {symbol}: {e}")
                    results[symbol] = {"error": f"Failed to fetch: {str(e)}"}
            
            return {"data": results}
        
    except Exception as e:
        logger.error(f"Error in bulk_fetch_charts: {e}")
        raise HTTPException(status_code=500, detail=str(e))

# ============================================
# 7️⃣ BULK FETCH ALL SYMBOLS (for login/dashboard)
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
# 9️⃣ CLEAR CACHE
# ============================================

@app.post("/api/cache/clear")
async def clear_cache():
    """Clear yfinance cache (clears persistent cache directory)"""
    try:
        import shutil
        cache_dir = os.path.join(os.path.expanduser("~"), ".cache", "py-yfinance")
        if os.path.exists(cache_dir):
            shutil.rmtree(cache_dir)
            os.makedirs(cache_dir, exist_ok=True)
            logger.info("yfinance cache cleared")
        return {"status": "cache cleared", "cache_dir": cache_dir}
    except Exception as e:
        logger.error(f"Error clearing cache: {e}")
        raise HTTPException(status_code=500, detail=f"Error clearing cache: {str(e)}")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
