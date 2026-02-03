#!/usr/bin/env python3
"""
Quick test script for Pricing Service
Tests all main endpoints to verify the service is working correctly
"""

import requests
import json
import sys
from datetime import datetime

# Configuration
BASE_URL = "http://localhost:8000"
TIMEOUT = 30

def print_section(title):
    print(f"\n{'='*60}")
    print(f"  {title}")
    print(f"{'='*60}")

def test_health_check():
    """Test health check endpoint"""
    print_section("1. Health Check")
    try:
        response = requests.get(f"{BASE_URL}/api/health", timeout=TIMEOUT)
        print(f"Status: {response.status_code}")
        if response.status_code == 200:
            data = response.json()
            print(f"✓ Service is healthy")
            print(f"  Status: {data.get('status')}")
            print(f"  Service: {data.get('service')}")
            print(f"  Timestamp: {data.get('timestamp')}")
            return True
        else:
            print(f"✗ Health check failed: {response.status_code}")
            return False
    except requests.exceptions.ConnectionError:
        print(f"✗ Cannot connect to {BASE_URL}")
        print(f"  Make sure the pricing service is running:")
        print(f"  uvicorn main:app --reload --port 8000")
        return False
    except Exception as e:
        print(f"✗ Error: {e}")
        return False

def test_single_price():
    """Test single price endpoint"""
    print_section("2. Single Price (AAPL)")
    try:
        symbol = "AAPL"
        response = requests.get(f"{BASE_URL}/api/price/{symbol}", timeout=TIMEOUT)
        print(f"Status: {response.status_code}")
        if response.status_code == 200:
            data = response.json()
            print(f"✓ Price fetched successfully")
            print(f"  Symbol: {data.get('symbol')}")
            print(f"  Price: ${data.get('price'):.2f}")
            print(f"  Currency: {data.get('currency')}")
            print(f"  Timestamp: {data.get('timestamp')}")
            return True
        else:
            print(f"✗ Failed: {response.status_code}")
            print(f"  Response: {response.text}")
            return False
    except Exception as e:
        print(f"✗ Error: {e}")
        return False

def test_multiple_prices():
    """Test multiple prices endpoint"""
    print_section("3. Multiple Prices (Batch)")
    try:
        symbols = ["AAPL", "MSFT", "GOOGL"]
        response = requests.post(
            f"{BASE_URL}/api/prices",
            json=symbols,
            headers={"Content-Type": "application/json"},
            timeout=TIMEOUT
        )
        print(f"Status: {response.status_code}")
        if response.status_code == 200:
            data = response.json()
            prices_data = data.get("data", {})
            print(f"✓ Batch fetch successful")
            print(f"  Requested: {len(symbols)} symbols")
            print(f"  Received: {len(prices_data)} prices")
            for symbol in symbols:
                symbol_upper = symbol.upper()
                if symbol_upper in prices_data:
                    price_info = prices_data[symbol_upper]
                    if "error" in price_info:
                        print(f"  ✗ {symbol_upper}: {price_info['error']}")
                    else:
                        print(f"  ✓ {symbol_upper}: ${price_info.get('price', 0):.2f}")
                else:
                    print(f"  ✗ {symbol_upper}: Not found")
            return True
        else:
            print(f"✗ Failed: {response.status_code}")
            print(f"  Response: {response.text}")
            return False
    except Exception as e:
        print(f"✗ Error: {e}")
        return False

def test_bulk_prices():
    """Test bulk prices endpoint"""
    print_section("4. Bulk Prices (Optimized)")
    try:
        symbols = ["AAPL", "MSFT", "GOOGL", "JPM", "TSLA"]
        response = requests.post(
            f"{BASE_URL}/api/prices/bulk",
            json=symbols,
            headers={"Content-Type": "application/json"},
            timeout=TIMEOUT
        )
        print(f"Status: {response.status_code}")
        if response.status_code == 200:
            data = response.json()
            prices_data = data.get("data", {})
            print(f"✓ Bulk fetch successful")
            print(f"  Requested: {len(symbols)} symbols")
            print(f"  Received: {len(prices_data)} prices")
            success_count = sum(1 for v in prices_data.values() if "error" not in v)
            print(f"  Successful: {success_count}/{len(symbols)}")
            for symbol in symbols[:3]:  # Show first 3
                symbol_upper = symbol.upper()
                if symbol_upper in prices_data:
                    price_info = prices_data[symbol_upper]
                    if "error" in price_info:
                        print(f"  ✗ {symbol_upper}: {price_info['error']}")
                    else:
                        print(f"  ✓ {symbol_upper}: ${price_info.get('price', 0):.2f}")
            return True
        else:
            print(f"✗ Failed: {response.status_code}")
            print(f"  Response: {response.text}")
            return False
    except Exception as e:
        print(f"✗ Error: {e}")
        return False

def test_portfolio_value():
    """Test portfolio value endpoint"""
    print_section("5. Portfolio Value")
    try:
        portfolio = {
            "AAPL": 10,
            "MSFT": 5,
            "GOOGL": 3
        }
        response = requests.post(
            f"{BASE_URL}/api/portfolio/value",
            json=portfolio,
            headers={"Content-Type": "application/json"},
            timeout=TIMEOUT
        )
        print(f"Status: {response.status_code}")
        if response.status_code == 200:
            data = response.json()
            print(f"✓ Portfolio value calculated")
            print(f"  Total Value: ${data.get('total_value', 0):.2f}")
            breakdown = data.get("breakdown", {})
            print(f"  Breakdown:")
            for symbol, info in breakdown.items():
                if "error" in info:
                    print(f"    ✗ {symbol}: {info['error']}")
                else:
                    print(f"    ✓ {symbol}: {info.get('quantity')} @ ${info.get('price', 0):.2f} = ${info.get('value', 0):.2f}")
            return True
        else:
            print(f"✗ Failed: {response.status_code}")
            print(f"  Response: {response.text}")
            return False
    except Exception as e:
        print(f"✗ Error: {e}")
        return False

def test_chart_data():
    """Test chart data endpoint"""
    print_section("6. Chart Data (AAPL)")
    try:
        symbol = "AAPL"
        period = "1mo"
        interval = "1d"
        response = requests.get(
            f"{BASE_URL}/api/chart/{symbol}?period={period}&interval={interval}",
            timeout=TIMEOUT
        )
        print(f"Status: {response.status_code}")
        if response.status_code == 200:
            data = response.json()
            chart_data = data.get("data", [])
            print(f"✓ Chart data fetched")
            print(f"  Symbol: {data.get('symbol')}")
            print(f"  Period: {data.get('period')}")
            print(f"  Interval: {data.get('interval')}")
            print(f"  Data Points: {len(chart_data)}")
            if chart_data:
                latest = chart_data[-1]
                print(f"  Latest Close: ${latest.get('close', 0):.2f}")
                print(f"  Date: {latest.get('time', 'N/A')}")
            return True
        else:
            print(f"✗ Failed: {response.status_code}")
            print(f"  Response: {response.text}")
            return False
    except Exception as e:
        print(f"✗ Error: {e}")
        return False

def test_portfolio_chart():
    """Test portfolio chart endpoint"""
    print_section("7. Portfolio Chart")
    try:
        portfolio = {
            "AAPL": 10,
            "MSFT": 5
        }
        period = "1mo"
        interval = "1d"
        response = requests.post(
            f"{BASE_URL}/api/portfolio/chart?period={period}&interval={interval}",
            json=portfolio,
            headers={"Content-Type": "application/json"},
            timeout=TIMEOUT
        )
        print(f"Status: {response.status_code}")
        if response.status_code == 200:
            data = response.json()
            chart_data = data.get("data", [])
            print(f"✓ Portfolio chart fetched")
            print(f"  Period: {data.get('period')}")
            print(f"  Interval: {data.get('interval')}")
            print(f"  Data Points: {len(chart_data)}")
            if chart_data:
                latest = chart_data[-1]
                print(f"  Latest Value: ${latest.get('value', 0):.2f}")
                print(f"  Date: {latest.get('time', 'N/A')}")
            return True
        else:
            print(f"✗ Failed: {response.status_code}")
            print(f"  Response: {response.text}")
            return False
    except Exception as e:
        print(f"✗ Error: {e}")
        return False

def test_bulk_charts():
    """Test bulk charts endpoint"""
    print_section("8. Bulk Charts")
    try:
        request_body = {
            "symbols": ["AAPL", "MSFT", "GOOGL"],
            "period": "1mo",
            "interval": "1d"
        }
        response = requests.post(
            f"{BASE_URL}/api/charts/bulk",
            json=request_body,
            headers={"Content-Type": "application/json"},
            timeout=TIMEOUT
        )
        print(f"Status: {response.status_code}")
        if response.status_code == 200:
            data = response.json()
            charts_data = data.get("data", {})
            print(f"✓ Bulk chart fetch successful")
            print(f"  Requested: {len(request_body['symbols'])} symbols")
            print(f"  Received: {len(charts_data)} charts")
            for symbol in request_body['symbols']:
                symbol_upper = symbol.upper()
                if symbol_upper in charts_data:
                    chart_info = charts_data[symbol_upper]
                    if "error" in chart_info:
                        print(f"  ✗ {symbol_upper}: {chart_info['error']}")
                    else:
                        print(f"  ✓ {symbol_upper}: {len(chart_info.get('data', []))} data points")
                else:
                    print(f"  ✗ {symbol_upper}: Not found")
            return True
        else:
            print(f"✗ Failed: {response.status_code}")
            print(f"  Response: {response.text}")
            return False
    except Exception as e:
        print(f"✗ Error: {e}")
        return False

def test_international_symbols():
    """Test international symbols"""
    print_section("9. International Symbols")
    try:
        symbols = ["RELIANCE.NS", "HSBA.L", "7203.T"]  # India, UK, Japan
        response = requests.post(
            f"{BASE_URL}/api/prices/bulk",
            json=symbols,
            headers={"Content-Type": "application/json"},
            timeout=TIMEOUT
        )
        print(f"Status: {response.status_code}")
        if response.status_code == 200:
            data = response.json()
            prices_data = data.get("data", {})
            print(f"✓ International symbols tested")
            for symbol in symbols:
                symbol_upper = symbol.upper()
                if symbol_upper in prices_data:
                    price_info = prices_data[symbol_upper]
                    if "error" in price_info:
                        print(f"  ✗ {symbol_upper}: {price_info['error']}")
                    else:
                        print(f"  ✓ {symbol_upper}: ${price_info.get('price', 0):.2f}")
                else:
                    print(f"  ✗ {symbol_upper}: Not found")
            return True
        else:
            print(f"✗ Failed: {response.status_code}")
            return False
    except Exception as e:
        print(f"✗ Error: {e}")
        return False

def test_clear_cache():
    """Test clear cache endpoint"""
    print_section("10. Clear Cache")
    try:
        response = requests.post(f"{BASE_URL}/api/cache/clear", timeout=TIMEOUT)
        print(f"Status: {response.status_code}")
        if response.status_code == 200:
            data = response.json()
            print(f"✓ Cache cleared successfully")
            print(f"  Status: {data.get('status')}")
            return True
        else:
            print(f"✗ Failed: {response.status_code}")
            print(f"  Response: {response.text}")
            return False
    except Exception as e:
        print(f"✗ Error: {e}")
        return False

def main():
    """Run all tests"""
    print("\n" + "="*60)
    print("  PRICING SERVICE TEST SUITE")
    print("="*60)
    print(f"Base URL: {BASE_URL}")
    print(f"Started: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    
    results = []
    
    # Run tests
    results.append(("Health Check", test_health_check()))
    
    # Only continue if health check passes
    if results[0][1]:
        results.append(("Single Price", test_single_price()))
        results.append(("Multiple Prices", test_multiple_prices()))
        results.append(("Bulk Prices", test_bulk_prices()))
        results.append(("Portfolio Value", test_portfolio_value()))
        results.append(("Chart Data", test_chart_data()))
        results.append(("Portfolio Chart", test_portfolio_chart()))
        results.append(("Bulk Charts", test_bulk_charts()))
        results.append(("International Symbols", test_international_symbols()))
        results.append(("Clear Cache", test_clear_cache()))
    else:
        print("\n⚠ Skipping other tests - service is not available")
    
    # Summary
    print_section("TEST SUMMARY")
    passed = sum(1 for _, result in results if result)
    total = len(results)
    print(f"Passed: {passed}/{total}")
    print("\nResults:")
    for name, result in results:
        status = "✓ PASS" if result else "✗ FAIL"
        print(f"  {status} - {name}")
    
    print(f"\nCompleted: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("="*60 + "\n")
    
    # Exit code
    sys.exit(0 if passed == total else 1)

if __name__ == "__main__":
    main()
