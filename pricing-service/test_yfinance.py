#!/usr/bin/env python3
"""Test script to diagnose yfinance connection issues"""

import yfinance as yf
import sys

def test_symbol(symbol):
    print(f"\n{'='*60}")
    print(f"Testing symbol: {symbol}")
    print(f"{'='*60}")
    
    try:
        ticker = yf.Ticker(symbol)
        print(f"✓ Ticker object created")
        
        # Try to get info
        try:
            info = ticker.info
            print(f"✓ Info retrieved: {len(info)} fields")
            if 'symbol' in info:
                print(f"  Symbol: {info.get('symbol')}")
            if 'longName' in info:
                print(f"  Name: {info.get('longName')}")
        except Exception as e:
            print(f"✗ Info failed: {e}")
        
        # Try to get history
        try:
            data = ticker.history(period="1d")
            if not data.empty:
                print(f"✓ History retrieved: {len(data)} rows")
                print(f"  Latest Close: ${data['Close'].iloc[-1]:.2f}")
                print(f"  Date: {data.index[-1]}")
            else:
                print(f"✗ History returned empty DataFrame")
        except Exception as e:
            print(f"✗ History failed: {e}")
            import traceback
            traceback.print_exc()
            
    except Exception as e:
        print(f"✗ Failed to create ticker: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    symbols = ["AAPL", "MSFT", "GOOGL"]
    
    if len(sys.argv) > 1:
        symbols = sys.argv[1:]
    
    print("yfinance Test Script")
    print(f"yfinance version: {yf.__version__}")
    print(f"Testing {len(symbols)} symbol(s)...")
    
    for symbol in symbols:
        test_symbol(symbol)
    
    print(f"\n{'='*60}")
    print("Test complete!")
    print(f"{'='*60}")
