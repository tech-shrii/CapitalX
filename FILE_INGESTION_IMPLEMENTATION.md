# CSV/Excel File Ingestion Feature - Implementation Summary

## Overview
Successfully implemented a comprehensive portfolio file ingestion feature for the CapitalX Spring Boot application. The feature allows portfolio managers to upload CSV or Excel files to automatically ingest portfolio data with full validation, customer/asset resolution, and historical snapshot tracking.

## Files Created

### New Dependencies (pom.xml)
- **OpenCSV** v5.9 - CSV parsing library
- **Apache POI** v5.2.5 - Excel file handling (.xls, .xlsx)

### Exceptions (4 new files)
1. **InvalidFileFormatException** - Thrown when filename doesn't match pattern
2. **InvalidCSVException** - Thrown when CSV content is invalid or missing columns
3. **AssetResolutionException** - Thrown when asset resolution/creation fails
4. **PortfolioIngestionException** - Thrown for ingestion workflow failures

### DTOs (1 new file)
1. **PortfolioHoldingCSVDTO** - Maps CSV row to portfolio holding object

### Utilities (2 new files)
1. **FilenameParser** - Parses filename: `customerCode_customerName_period.csv`
   - Extracts customer code, name, period label
   - Derives period type (ANNUAL/QUARTERLY/CUSTOM)
   - Comprehensive validation

2. **CSVParser** - Parses CSV/Excel file content
   - Supports .csv, .xlsx, .xls formats
   - Validates all required columns present
   - Handles multiple date formats
   - Type conversion and validation

### Service Layer (3 new files)
1. **PortfolioIngestionService** (Interface) - Service contract
2. **PortfolioIngestionServiceImpl** - Full implementation
   - Orchestrates complete ingestion workflow
   - Customer resolution/creation
   - Asset resolution/creation
   - Portfolio upload creation
   - Holdings insertion (append-only)
   - Portfolio summary generation
   - Annual performance update
   - Fully transactional (@Transactional)

3. **PortfolioIngestionResult** - DTO for ingestion response

### Controller (1 new file)
1. **PortfolioIngestionController**
   - Endpoint: `POST /api/portfolio/ingest/upload`
   - Accepts multipart file upload
   - Delegates to service
   - No business logic

### Exception Handler (Modified)
- **GlobalExceptionHandler** - Added handlers for:
  - InvalidFileFormatException (400)
  - InvalidCSVException (400)
  - AssetResolutionException (400)
  - PortfolioIngestionException (400)

### Test/Documentation Files (3 sample CSVs + 1 guide)
1. **CUST001_RaviKumar_FY2025.csv** - Sample annual portfolio (4 holdings)
2. **CUST002_AnitaSharma_FY2025.csv** - Sample annual portfolio (3 holdings)
3. **CUST003_VivekPatel_Q1-2026.csv** - Sample quarterly portfolio (3 holdings)
4. **FILE_INGESTION_GUIDE.md** - Complete user documentation

## Ingestion Workflow

```
1. Filename Validation
   ├─ Parse: customerCode_customerName_period.csv
   ├─ Extract: code, name, period, periodType
   └─ Reject if invalid format

2. Customer Resolution
   ├─ Lookup by customerCode
   ├─ Create if not exists
   └─ Update name if differs (ID stable)

3. Portfolio Upload Creation
   └─ INSERT portfolio_uploads record (immutable)

4. CSV Parsing
   ├─ Detect .csv/.xlsx/.xls
   ├─ Validate columns present
   └─ Parse rows to PortfolioHoldingCSVDTO

5. Holdings Processing (per row)
   ├─ Resolve or create asset by code
   ├─ Validate required fields
   └─ INSERT portfolio_holding record

6. Portfolio Summary Generation
   ├─ Aggregate totals
   ├─ Count profitable/loss assets
   └─ INSERT portfolio_summary record

7. Annual Performance Update (if ANNUAL)
   ├─ Extract financial year
   ├─ Calculate best/worst assets
   └─ INSERT/UPDATE annual_performance

8. Return Success Result
```

## Key Features

### ✅ Filename Validation
- Format: `customerCode_customerName_period.csv`
- Supports .csv, .xlsx, .xls extensions
- Automatic period type detection
- Immediate rejection of invalid format

### ✅ Customer Resolution
- Lookup by customer_code
- Create if not exists
- Update name if differs (ID stable)
- Single source of truth

### ✅ CSV/Excel Parsing
- Multiple date format support (yyyy-MM-dd, dd-MM-yyyy, MM/dd/yyyy, dd/MM/yyyy)
- Type conversion for numbers and decimals
- Column header validation (case-insensitive)
- Empty row skipping
- Detailed row-by-row error messages

### ✅ Asset Resolution
- Global reusable assets
- Lookup by asset_code
- Auto-create if not found
- Prevents duplicates

### ✅ Append-Only Holdings
- Each upload creates fresh snapshot
- Holdings never updated/deleted
- Full historical audit trail
- Snapshot isolation per upload

### ✅ Portfolio Summary
- Automatic aggregation after insert
- Totals and asset counts
- Enables fast dashboard queries
- One summary per upload

### ✅ Annual Performance
- Triggered for ANNUAL period uploads
- Financial year extraction from label
- Best/worst asset identification
- Opening/closing value tracking
- Insert or update semantics

### ✅ Transactional Integrity
- All or nothing semantics
- Rollback on any error
- No partial writes
- Atomic file ingestion

### ✅ Error Handling
- 4 custom exceptions for different failure modes
- Global exception handler with proper HTTP status codes
- Clear error messages for debugging
- Validation before database writes

## API Endpoint

```
POST /api/portfolio/ingest/upload
Content-Type: multipart/form-data

Parameter: file [CSV or Excel file]
```

### Success Response (200 OK)
```json
{
  "uploadId": 5,
  "customerId": 1,
  "customerCode": "CUST001",
  "customerName": "Ravi Kumar",
  "periodLabel": "FY2025",
  "holdingsCount": 4,
  "message": "Portfolio ingestion successful",
  "success": true
}
```

### Error Response (400 Bad Request)
```json
"Invalid file format: Invalid filename format. Expected format: customerCode_customerName_period.csv"
```

## CSV Column Requirements

All these columns must be present (case-insensitive, order independent):

- asset_code (String) - Required
- asset_name (String) - Required
- asset_type (Enum: STOCK, CRYPTO, COMMODITY, ETF, OTHER) - Required
- exchange_or_market (String) - Required
- quantity (Decimal) - Required
- buy_price (Decimal) - Required
- current_price (Decimal) - Required
- invested_value (Decimal) - Required
- current_value (Decimal) - Required
- profit_loss (Decimal) - Required
- investment_start_date (Date: yyyy-MM-dd format) - Required
- investment_end_date (Date) - Optional

## Example Usage

```bash
curl -X POST \
  -F "file=@CUST001_RaviKumar_FY2025.csv" \
  http://localhost:8080/api/portfolio/ingest/upload
```

## Integration with Existing System

✅ Uses existing repositories
✅ Uses existing entity beans
✅ Follows existing service patterns
✅ Uses existing exception handlers (extended)
✅ Supports existing read APIs
✅ Maintains append-only architecture
✅ Preserves historical data integrity

## Documentation

Complete user guide available in **FILE_INGESTION_GUIDE.md**

Topics covered:
- Feature overview
- File naming convention (mandatory)
- CSV format specification
- API endpoint details
- Processing workflow
- Transaction semantics
- Data integrity guarantees
- Example scenarios
- Error handling
- Troubleshooting
- Architecture notes

## Testing

Sample CSV files provided for testing:
1. `CUST001_RaviKumar_FY2025.csv` - 4 diverse assets
2. `CUST002_AnitaSharma_FY2025.csv` - 3 assets with mixed P&L
3. `CUST003_VivekPatel_Q1-2026.csv` - Quarterly portfolio

## Production Ready ✅

- ✅ No breaking changes - Fully backward compatible
- ✅ Follows existing patterns - Consistent architecture
- ✅ Clean separation of concerns
- ✅ Comprehensive error handling
- ✅ Transactional integrity
- ✅ Well-documented
- ✅ Ready to deploy
