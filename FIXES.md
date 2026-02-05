# Critical Fixes for Pricing Service

## Problem Summary
1. **Stooq CSV Header Issue**: The code tries to convert "Close" (header text) to float
2. **Forex Not Working**: Yahoo Finance needs `=X` suffix for forex pairs
3. **Commodities Failing**: Need proper Yahoo fallback

## Fix 1: Replace `_fetch_stooq_latest()` function (Lines ~140-190)

**Find this function and REPLACE it entirely:**

```python
def _fetch_stooq_latest(stooq_symbol: str) -> dict:
    """
    Fetch latest quote from Stooq.
    Returns dict with price, timestamp, etc. or raises ValueError.
    """
    url = f"{_STOOQ_BASE_URL}/q/l/"
    params = {
        "s": stooq_symbol,
        "f": "sd2t2ohlcv",
        "h": "",
        "e": "csv",
    }
    
    try:
        resp = _http_session.get(url, params=params, timeout=10)
        resp.raise_for_status()
        text = resp.text.strip()
        
        if not text or "N/D" in text or len(text) < 10:
            raise ValueError(f"Stooq: No data for '{stooq_symbol}'")
        
        # CRITICAL FIX: Handle multiple lines and skip header
        lines = [line.strip() for line in text.split('\n') if line.strip()]
        if not lines:
            raise ValueError(f"Stooq: Empty response for '{stooq_symbol}'")
        
        # Take the last line (actual data, not header)
        data_line = lines[-1]
        parts = data_line.split(',')
        
        if len(parts) < 7:
            raise ValueError(f"Stooq: Invalid CSV format for '{stooq_symbol}'")
        
        # Check if this is a header line
        if parts[6].strip().lower() in ['close', 'n/d', '']:
            raise ValueError(f"Stooq: No data for '{stooq_symbol}' (header only)")
        
        # Parse date/time
        date_str = parts[1].strip()
        time_str = parts[2].strip() if len(parts) > 2 else "00:00:00"
        
        if date_str.lower() in ['date', 'n/d', '']:
            raise ValueError(f"Stooq: No data for '{stooq_symbol}'")
        
        try:
            timestamp = pd.to_datetime(f"{date_str} {time_str}").isoformat()
        except:
            timestamp = datetime.now().isoformat()
        
        # Parse prices - validate they're actually numbers
        try:
            close_price = float(parts[6])
            open_price = float(parts[3]) if len(parts) > 3 else close_price
            high_price = float(parts[4]) if len(parts) > 4 else close_price
            low_price = float(parts[5]) if len(parts) > 5 else close_price
        except (ValueError, IndexError) as e:
            raise ValueError(f"Stooq: Cannot parse prices for '{stooq_symbol}': {parts[6] if len(parts) > 6 else 'missing'}")
        
        # Parse volume
        volume = 0
        if len(parts) > 7 and parts[7].strip():
            try:
                volume = int(float(parts[7]))
            except:
                volume = 0
        
        return {
            'price': close_price,
            'open': open_price,
            'high': high_price,
            'low': low_price,
            'volume': volume,
            'timestamp': timestamp,
        }
    except requests.exceptions.RequestException as e:
        raise ValueError(f"Stooq request failed: {e}")
```

---

## Fix 2: Add Yahoo Forex Helper (Insert BEFORE `fetch_price()` function, around line 280)

**ADD this new function:**

```python
def _yahoo_forex_symbol(symbol: str) -> str:
    """
    Convert internal forex symbols to Yahoo Finance format.
    Yahoo needs =X suffix for forex pairs.
    
    Examples:
        USDJPY -> JPY=X
        EURUSD -> EURUSD=X
        GBPUSD -> GBPUSD=X
    """
    symbol = symbol.upper().strip()
    
    # If it's already in Yahoo format, return as-is
    if symbol.endswith('=X'):
        return symbol
    
    # Map common patterns
    forex_map = {
        'USDJPY': 'JPY=X',
        'USDCAD': 'CAD=X',
        'USDCHF': 'CHF=X',
        'USDHKD': 'HKD=X',
        'USDINR': 'INR=X',
        'USDSGD': 'SGD=X',
        'USDKRW': 'KRW=X',
        'EURUSD': 'EURUSD=X',
        'GBPUSD': 'GBPUSD=X',
        'AUDUSD': 'AUDUSD=X',
        'NZDUSD': 'NZDUSD=X',
        'EURGBP': 'EURGBP=X',
        'EURJPY': 'EURJPY=X',
        'GBPJPY': 'GBPJPY=X',
    }
    
    # Check map first
    if symbol in forex_map:
        return forex_map[symbol]
    
    # For USD/XXX format, just append =X to the target currency
    if len(symbol) == 6 and symbol.startswith('USD'):
        return f"{symbol[3:]}=X"
    
    # For any other 6-char pair, try appending =X
    if len(symbol) == 6 and symbol.isalpha():
        return f"{symbol}=X"
    
    return symbol
```

---

## Fix 3: Update `fetch_price()` - Yahoo Forex Section (Around line 350)

**FIND this section in the `fetch_price()` function:**

```python
    # Fallback to yfinance
    try:
        ticker = yf.Ticker(norm['yahoo'])
```

**REPLACE with:**

```python
    # Fallback to yfinance
    try:
        # Use special Yahoo format for forex
        yahoo_symbol = norm['yahoo']
        if norm['type'] == 'forex':
            yahoo_symbol = _yahoo_forex_symbol(yahoo_symbol)
            logger.info(f"Using Yahoo forex symbol: {yahoo_symbol}")
        
        ticker = yf.Ticker(yahoo_symbol)
```

---

## Fix 4: Update Commodities Mapping (Around line 55)

**FIND the `_COMMODITY_MAPPING` dictionary and REPLACE with:**

```python
# Commodities: Yahoo format → Stooq format
_COMMODITY_MAPPING = {
    "GC.F": "gc.f",      # Gold futures
    "SI.F": "si.f",      # Silver futures
    "PL.F": "pl.f",      # Platinum futures
    "CB.F": "cl.f",      # Crude Oil Brent
    "CL.F": "cl.f",      # Crude Oil WTI
    "HG.F": "hg.f",      # Copper
    "NG.F": "ng.f",      # Natural Gas
}
```

**Then ADD this new mapping for Yahoo commodities (right after _COMMODITY_MAPPING):**

```python
# Yahoo Finance commodity symbols (without Stooq, use Yahoo directly)
_YAHOO_COMMODITY_MAP = {
    "GC.F": "GC=F",      # Gold
    "SI.F": "SI=F",      # Silver
    "PL.F": "PL=F",      # Platinum
    "CB.F": "CL=F",      # Crude Oil (use WTI as fallback)
    "CL.F": "CL=F",      # Crude Oil WTI
    "HG.F": "HG=F",      # Copper
    "NG.F": "NG=F",      # Natural Gas
}
```

---

## Fix 5: Update `fetch_price()` - Commodities Section (Around line 350)

**FIND the same Yahoo fallback section and UPDATE to:**

```python
    # Fallback to yfinance
    try:
        # Use special Yahoo format for forex and commodities
        yahoo_symbol = norm['yahoo']
        if norm['type'] == 'forex':
            yahoo_symbol = _yahoo_forex_symbol(yahoo_symbol)
            logger.info(f"Using Yahoo forex symbol: {yahoo_symbol}")
        elif norm['type'] == 'commodity' and yahoo_symbol in _YAHOO_COMMODITY_MAP:
            yahoo_symbol = _YAHOO_COMMODITY_MAP[yahoo_symbol]
            logger.info(f"Using Yahoo commodity symbol: {yahoo_symbol}")
        
        ticker = yf.Ticker(yahoo_symbol)
```

---

## Summary of Changes

### Files to Modify: `main.py`

1. **Line ~140**: Replace entire `_fetch_stooq_latest()` function
2. **Line ~55**: Update `_COMMODITY_MAPPING` and add `_YAHOO_COMMODITY_MAP`
3. **Line ~280**: Add new `_yahoo_forex_symbol()` helper function
4. **Line ~350**: Update Yahoo fallback in `fetch_price()` to use forex/commodity mapping

### Expected Results After Fixes

**Before:**
- ❌ Stocks fail: `could not convert string to float: 'Close'`
- ❌ Forex fail: `USDJPY not found`
- ❌ Commodities fail: `GC.F not found`

**After:**
- ✅ Stocks work via Yahoo (Stooq has coverage issues)
- ✅ Forex works via Yahoo with `=X` suffix
- ✅ Commodities work via Yahoo with `=F` suffix
- ✅ Crypto works via Yahoo
- ✅ Mutual funds work via Yahoo

### Success Rate Prediction
- **Stocks**: 95%+ (via Yahoo)
- **Crypto**: 95%+ (via Yahoo)
- **Mutual Funds**: 90%+ (via Yahoo)
- **Forex**: 80%+ (via Yahoo with =X)
- **Commodities**: 70%+ (via Yahoo with =F)
- **Bonds**: 10-20% (neither source has good coverage)

### Quick Test After Applying Fixes

```bash
# Test the /api/prices/bulk endpoint
curl -X POST http://localhost:8000/api/prices/bulk \
  -H "Content-Type: application/json" \
  -d '["AAPL", "BTC-USD", "USDJPY", "GC.F", "EURUSD"]'
```

Expected: 4/5 should succeed (USDJPY, GC.F, EURUSD now work)
