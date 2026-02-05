#!/usr/bin/env python3
"""
Script to import historical price data from CSV into asset_prices table.
- Reads from extract-data-2026-02-04.csv
- Inserts into MySQL database (portfolio_db)
- Sets source as MANUAL
- Avoids duplicates (symbol + date combination)
- Sets time as 00:00:00 for all dates
"""

import csv
import mysql.connector
from datetime import datetime, time
from decimal import Decimal
import sys
import os

# Database configuration
DB_CONFIG = {
    'host': 'localhost',
    'port': 3306,
    'user': 'root',
    'password': '1234',
    'database': 'portfolio_db'
}

# CSV file path (relative to script location)
CSV_FILE = '../extract-data-2026-02-04.csv'

def get_csv_path():
    """Get absolute path to CSV file"""
    script_dir = os.path.dirname(os.path.abspath(__file__))
    csv_path = os.path.join(script_dir, CSV_FILE)
    if not os.path.exists(csv_path):
        # Try alternative locations
        alt_paths = [
            os.path.join(os.path.dirname(script_dir), 'extract-data-2026-02-04.csv'),
            'extract-data-2026-02-04.csv'
        ]
        for path in alt_paths:
            if os.path.exists(path):
                return path
        raise FileNotFoundError(f"CSV file not found. Tried: {csv_path}")
    return csv_path

def parse_date(date_str):
    """Parse date string and set time to 00:00:00"""
    try:
        # Parse date (YYYY-MM-DD format)
        date_obj = datetime.strptime(date_str.strip(), '%Y-%m-%d').date()
        # Combine with time 00:00:00
        datetime_obj = datetime.combine(date_obj, time(0, 0, 0))
        return datetime_obj
    except ValueError as e:
        print(f"Error parsing date '{date_str}': {e}")
        return None

def check_duplicate(cursor, symbol, price_date):
    """Check if a price entry already exists for this symbol and date"""
    # Check for exact match on symbol and date (ignoring time)
    query = """
        SELECT COUNT(*) FROM asset_prices 
        WHERE symbol = %s AND DATE(price_date) = DATE(%s)
    """
    cursor.execute(query, (symbol.upper(), price_date))
    count = cursor.fetchone()[0]
    return count > 0

def insert_price(cursor, symbol, price, price_date):
    """Insert a price entry into asset_prices table"""
    insert_query = """
        INSERT INTO asset_prices (symbol, current_price, price_date, source, asset_id)
        VALUES (%s, %s, %s, %s, NULL)
    """
    try:
        cursor.execute(insert_query, (
            symbol.upper(),
            Decimal(str(price)),
            price_date,
            'MANUAL'
        ))
        return True
    except mysql.connector.Error as e:
        print(f"Error inserting {symbol} on {price_date.date()}: {e}")
        return False

def alter_table_if_needed(cursor):
    """Alter asset_prices table to allow NULL asset_id if needed"""
    try:
        # Check if asset_id column allows NULL
        cursor.execute("""
            SELECT IS_NULLABLE 
            FROM INFORMATION_SCHEMA.COLUMNS 
            WHERE TABLE_SCHEMA = %s 
            AND TABLE_NAME = 'asset_prices' 
            AND COLUMN_NAME = 'asset_id'
        """, (DB_CONFIG['database'],))
        result = cursor.fetchone()
        
        if result and result[0] == 'NO':
            print("Altering asset_prices table to allow NULL asset_id...")
            cursor.execute("ALTER TABLE asset_prices MODIFY COLUMN asset_id BIGINT NULL")
            print("Table altered successfully")
        else:
            print("asset_id column already allows NULL")
    except mysql.connector.Error as e:
        print(f"Warning: Could not alter table (may already be correct): {e}")

def import_csv_to_database():
    """Main function to import CSV data into database"""
    csv_path = get_csv_path()
    print(f"Reading CSV file: {csv_path}")
    
    # Connect to database
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        cursor = conn.cursor()
        print("Connected to MySQL database")
        
        # Alter table to allow NULL asset_id if needed
        alter_table_if_needed(cursor)
        conn.commit()
    except mysql.connector.Error as e:
        print(f"Error connecting to database: {e}")
        return
    
    try:
        inserted_count = 0
        skipped_count = 0
        error_count = 0
        
        with open(csv_path, 'r', encoding='utf-8') as csvfile:
            reader = csv.DictReader(csvfile)
            
            # Validate headers
            expected_headers = ['symbol', 'date', 'adjusted_close_price']
            if not all(header in reader.fieldnames for header in expected_headers):
                print(f"Error: CSV must have headers: {expected_headers}")
                print(f"Found headers: {reader.fieldnames}")
                return
            
            print(f"Starting import...")
            
            for row_num, row in enumerate(reader, start=2):  # Start at 2 (row 1 is header)
                try:
                    symbol = row['symbol'].strip()
                    date_str = row['date'].strip()
                    price_str = row['adjusted_close_price'].strip()
                    
                    # Validate required fields
                    if not symbol or not date_str or not price_str:
                        print(f"Row {row_num}: Skipping - missing required fields")
                        skipped_count += 1
                        continue
                    
                    # Parse date
                    price_date = parse_date(date_str)
                    if price_date is None:
                        skipped_count += 1
                        continue
                    
                    # Parse price
                    try:
                        price = float(price_str)
                        if price <= 0:
                            print(f"Row {row_num}: Skipping - invalid price: {price}")
                            skipped_count += 1
                            continue
                    except ValueError:
                        print(f"Row {row_num}: Skipping - invalid price format: {price_str}")
                        skipped_count += 1
                        continue
                    
                    # Check for duplicate
                    if check_duplicate(cursor, symbol, price_date):
                        skipped_count += 1
                        if row_num % 100 == 0:
                            print(f"Processed {row_num} rows... (duplicate: {symbol} on {date_str})")
                        continue
                    
                    # Insert price
                    if insert_price(cursor, symbol, price, price_date):
                        inserted_count += 1
                        if inserted_count % 100 == 0:
                            print(f"Inserted {inserted_count} prices...")
                    else:
                        error_count += 1
                    
                except Exception as e:
                    print(f"Row {row_num}: Error processing row: {e}")
                    error_count += 1
                    continue
        
        # Commit transaction
        conn.commit()
        print(f"\n{'='*60}")
        print(f"Import completed!")
        print(f"  Inserted: {inserted_count} prices")
        print(f"  Skipped (duplicates): {skipped_count} prices")
        print(f"  Errors: {error_count} prices")
        print(f"{'='*60}")
        
    except FileNotFoundError as e:
        print(f"Error: {e}")
    except Exception as e:
        print(f"Error during import: {e}")
        conn.rollback()
    finally:
        cursor.close()
        conn.close()
        print("Database connection closed")

if __name__ == "__main__":
    print("Historical Price Data Importer")
    print("="*60)
    import_csv_to_database()
