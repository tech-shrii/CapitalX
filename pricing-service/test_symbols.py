"""
Test Script for Pricing Service Symbol Mappings
================================================
Tests all symbols from new_assets.csv against the pricing service.
"""

import json
import pandas as pd
from collections import defaultdict

# Import the normalize_symbol function from main.py
import sys
sys.path.insert(0, '/home/claude')
from main import normalize_symbol, fetch_price


def load_assets_from_csv(filepath):
    """Load assets from CSV file."""
    df = pd.read_csv(filepath)
    assets = []
    for _, row in df.iterrows():
        assets.append({
            'name': row['name'],
            'category': row['category'],
            'symbol': row['symbol'],
            'currency': row['currency']
        })
    return assets


def test_symbol_mappings(assets):
    """Test symbol normalization for all assets."""
    print("=" * 80)
    print("SYMBOL MAPPING TEST")
    print("=" * 80)
    
    results_by_category = defaultdict(list)
    
    for asset in assets:
        symbol = asset['symbol']
        category = asset['category']
        
        norm = normalize_symbol(symbol)
        
        result = {
            'symbol': symbol,
            'name': asset['name'],
            'category': category,
            'normalized': norm
        }
        
        results_by_category[category].append(result)
        
        print(f"\n{asset['name']}")
        print(f"  Original:  {symbol}")
        print(f"  Category:  {category}")
        print(f"  Type:      {norm['type']}")
        print(f"  Yahoo:     {norm['yahoo']}")
        print(f"  Stooq:     {norm['stooq']}")
        if norm['needs_inversion']:
            print(f"  ⚠️  INVERSION: Rate will be inverted (1/price)")
    
    print("\n" + "=" * 80)
    print("SUMMARY BY CATEGORY")
    print("=" * 80)
    for category, results in sorted(results_by_category.items()):
        print(f"\n{category}: {len(results)} assets")
        for r in results:
            print(f"  - {r['symbol']}: {r['normalized']['stooq']}")


def test_price_fetching(assets, max_tests=5):
    """
    Test actual price fetching for a sample of assets.
    WARNING: This makes real API calls, so we limit the number.
    """
    print("\n" + "=" * 80)
    print("PRICE FETCHING TEST (Sample)")
    print("=" * 80)
    print(f"Testing {max_tests} assets from each category...\n")
    
    by_category = defaultdict(list)
    for asset in assets:
        by_category[asset['category']].append(asset)
    
    results = {
        'success': [],
        'failed': []
    }
    
    for category, cat_assets in sorted(by_category.items()):
        print(f"\n--- {category} ---")
        sample = cat_assets[:min(max_tests, len(cat_assets))]
        
        for asset in sample:
            symbol = asset['symbol']
            try:
                data = fetch_price(symbol)
                print(f"✓ {symbol:12s} {data['price']:>12.6f} {asset['currency']:3s} (source: {data.get('source', 'unknown')})")
                results['success'].append({
                    'symbol': symbol,
                    'category': category,
                    'price': data['price'],
                    'source': data.get('source', 'unknown')
                })
            except Exception as e:
                print(f"✗ {symbol:12s} FAILED: {str(e)[:60]}")
                results['failed'].append({
                    'symbol': symbol,
                    'category': category,
                    'error': str(e)
                })
    
    print("\n" + "=" * 80)
    print("PRICE FETCHING SUMMARY")
    print("=" * 80)
    print(f"Successful: {len(results['success'])}")
    print(f"Failed:     {len(results['failed'])}")
    
    if results['success']:
        print("\nSuccess by source:")
        by_source = defaultdict(int)
        for r in results['success']:
            by_source[r['source']] += 1
        for source, count in sorted(by_source.items()):
            print(f"  {source}: {count}")
    
    if results['failed']:
        print("\nFailed assets:")
        for r in results['failed']:
            print(f"  {r['symbol']:12s} ({r['category']}): {r['error'][:60]}")
    
    # Success rate by category
    print("\nSuccess rate by category:")
    by_cat = defaultdict(lambda: {'success': 0, 'total': 0})
    for r in results['success']:
        by_cat[r['category']]['success'] += 1
        by_cat[r['category']]['total'] += 1
    for r in results['failed']:
        by_cat[r['category']]['total'] += 1
    
    for category in sorted(by_cat.keys()):
        stats = by_cat[category]
        rate = (stats['success'] / stats['total'] * 100) if stats['total'] > 0 else 0
        print(f"  {category:15s} {stats['success']}/{stats['total']} ({rate:.1f}%)")
    
    return results


def main():
    """Main test function."""
    csv_path = "/mnt/user-data/uploads/new_assets.csv"
    
    print("Loading assets from CSV...")
    assets = load_assets_from_csv(csv_path)
    print(f"Loaded {len(assets)} assets\n")
    
    # Test 1: Symbol mappings
    test_symbol_mappings(assets)
    
    # Test 2: Price fetching (sample)
    print("\n")
    response = input("Do you want to test actual price fetching? (y/n): ")
    if response.lower() == 'y':
        test_price_fetching(assets, max_tests=3)
    else:
        print("\nSkipping price fetching test.")
    
    print("\n" + "=" * 80)
    print("TEST COMPLETE")
    print("=" * 80)
    print("\nNext steps:")
    print("1. Review the symbol mappings above")
    print("2. Check SYMBOL_MAPPING.md for detailed documentation")
    print("3. Consider adding dedicated APIs for bonds, forex, and commodities")
    print("4. Start the server with: python main.py")
    print("5. Test the bulk endpoint with: POST /api/prices/bulk")


if __name__ == "__main__":
    main()
