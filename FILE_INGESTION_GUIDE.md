# Portfolio File Ingestion Feature

## Overview

The Portfolio File Ingestion feature allows portfolio managers to upload CSV or Excel files containing portfolio holdings. The system automatically processes the file to:

1. **Resolve/Create Customers** - Based on customer code and name from the filename
2. **Create Portfolio Upload Records** - Tracks each upload as a historical snapshot
3. **Resolve/Create Assets** - Auto-creates assets if they don't exist
4. **Insert Portfolio Holdings** - Records individual holdings (immutable, append-only)
5. **Generate Portfolio Summary** - Aggregates totals and asset counts
6. **Update Annual Performance** - For annual uploads, updates P&L analytics

## File Naming Convention (MANDATORY)

All uploaded files **must** follow this exact naming pattern:

```
customerCode_customerName_period.csv
```

### Examples:
- `CUST001_RaviKumar_FY2025.csv` → Customer: CUST001, Name: Ravi Kumar, Period: FY2025 (Annual)
- `CUST002_AnitaSharma_Q1-2026.csv` → Customer: CUST002, Name: Anita Sharma, Period: Q1-2026 (Quarterly)
- `CUST003_JohnDoe_Custom.csv` → Customer: CUST003, Name: John Doe, Period: Custom

### Period Type Detection:
- **ANNUAL**: Detected if label starts with `FY`, contains `ANNUAL`, or ends with 4-digit year
- **QUARTERLY**: Detected if label matches `Q[1-4]` pattern or contains `QUARTER`
- **CUSTOM**: All other patterns default to custom

## Filename Parsing Rules

| Component | Pattern | Example | Maps To |
|-----------|---------|---------|---------|
| `customerCode` | Alphanumeric | `CUST001` | `customers.customer_code` |
| `customerName` | Any text (no underscores) | `RaviKumar` | `customers.customer_name` |
| `period` | Any text (no underscores) | `FY2025` | `portfolio_uploads.period_label` |

**Validation:**
- All three components required
- Components separated by single underscore `_`
- File must end with `.csv`, `.xlsx`, or `.xls`
- No empty components allowed

## CSV File Format

### Supported File Types
- `.csv` (Comma-separated values)
- `.xlsx` (Excel 2007+)
- `.xls` (Excel 97-2003)

### Required Columns (Case-insensitive)
The CSV **must** contain all these columns (order doesn't matter):

```
asset_code          | Asset identifier (e.g., TCS, INFY, BTC)
asset_name          | Asset name (e.g., Tata Consultancy Services)
asset_type          | STOCK, CRYPTO, COMMODITY, ETF, or OTHER
exchange_or_market  | Trading venue (e.g., NSE, BINANCE, MCX)
quantity            | Numeric, decimal allowed (e.g., 100, 0.5)
buy_price           | Purchase price per unit (e.g., 3200.00)
current_price       | Current market price per unit (e.g., 3500.00)
invested_value      | Total investment (quantity × buy_price)
current_value       | Current total value (quantity × current_price)
profit_loss         | Profit or loss on this holding
investment_start_date | Date holding started (required)
investment_end_date | Date holding ended (optional, can be empty)
```

### Date Format Support
- `yyyy-MM-dd` (preferred: 2024-04-01)
- `dd-MM-yyyy` (2024-04-01)
- `MM/dd/yyyy` (04/01/2024)
- `dd/MM/yyyy` (01/04/2024)

### Example CSV Structure
```csv
asset_code,asset_name,asset_type,exchange_or_market,quantity,buy_price,current_price,invested_value,current_value,profit_loss,investment_start_date,investment_end_date
TCS,Tata Consultancy Services,STOCK,NSE,100,3200.00,3500.00,320000.00,350000.00,30000.00,2024-04-01,
INFY,Infosys Ltd,STOCK,NSE,75,1400.00,1350.00,105000.00,101250.00,-3750.00,2024-05-15,
BTC,Bitcoin,CRYPTO,BINANCE,0.5,2000000.00,2400000.00,1000000.00,1200000.00,200000.00,2024-06-01,
```

## API Endpoint

### Upload Portfolio File

**Endpoint:**
```
POST /api/portfolio/ingest/upload
```

**Request:**
- Method: `POST`
- Content-Type: `multipart/form-data`
- Parameter: `file` (the CSV or Excel file)

**Example (cURL):**
```bash
curl -X POST \
  -F "file=@CUST001_RaviKumar_FY2025.csv" \
  http://localhost:8080/api/portfolio/ingest/upload
```

**Example (JavaScript/Fetch):**
```javascript
const formData = new FormData();
formData.append('file', fileInput.files[0]);

fetch('http://localhost:8080/api/portfolio/ingest/upload', {
  method: 'POST',
  body: formData
})
.then(response => response.json())
.then(data => console.log(data));
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

### Error Responses

**400 Bad Request - Invalid Filename:**
```json
"Invalid file format: Invalid filename format. Expected format: customerCode_customerName_period.csv. Got: CUST001RaviKumar2025.csv"
```

**400 Bad Request - Invalid CSV:**
```json
"Invalid CSV content: CSV missing required columns: [current_price, exchange_or_market]"
```

**400 Bad Request - Invalid Asset Type:**
```json
"Invalid CSV content: Invalid asset type: INVALID. Valid types: STOCK, CRYPTO, COMMODITY, ETF, OTHER"
```

**400 Bad Request - Invalid Date Format:**
```json
"Invalid CSV content: Error parsing row 3: Cannot parse date 'invalid-date'. Supported formats: yyyy-MM-dd, dd-MM-yyyy, MM/dd/yyyy, dd/MM/yyyy"
```

**500 Internal Server Error:**
```json
"Portfolio ingestion failed: Unexpected error during portfolio ingestion: [error details]"
```

## Processing Workflow

### 1. Filename Validation
- Extract: `customerCode`, `customerName`, `period`
- Derive: `periodType` (ANNUAL, QUARTERLY, or CUSTOM)
- Reject if filename doesn't match pattern

### 2. Customer Resolution
```
IF customer_code exists:
    UPDATE customer_name (if different)
    REUSE customer_id
ELSE:
    CREATE new customer with customer_code and customer_name
```

**Guarantees:** Customer ID remains stable across uploads

### 3. Portfolio Upload Creation
```
INSERT INTO portfolio_uploads (
  customer_id, period_type, period_label, 
  upload_date, file_name
) VALUES (...)
```

Each upload is a new immutable record (never updated)

### 4. CSV Content Parsing
- Read CSV rows
- Validate all required columns present
- Parse each row into PortfolioHoldingCSVDTO
- Stop if any row is invalid (transaction rollback)

### 5. Asset Resolution
```
FOR EACH holding in CSV:
    asset = LOOKUP asset BY asset_code
    IF asset not found:
        CREATE new asset (code, name, type, exchange)
    LINK holding to asset
```

**Assets are global and reusable** across customers

### 6. Portfolio Holdings Insertion
```
FOR EACH holding in CSV:
    INSERT INTO portfolio_holdings (
      upload_id, customer_id, asset_id,
      quantity, buy_price, current_price,
      invested_value, current_value, profit_loss,
      investment_start_date, investment_end_date
    ) VALUES (...)
```

**Append-only:** Holdings are never updated or deleted

### 7. Portfolio Summary Generation
After all holdings inserted:
```
INSERT INTO portfolio_summary (
  upload_id, customer_id,
  total_invested_value, total_current_value, total_profit_loss,
  number_of_assets, number_of_profitable_assets, number_of_loss_assets
) VALUES (...)
```

Aggregates enable fast dashboard queries

### 8. Annual Performance Update
**Only for ANNUAL period uploads:**
```
annual_performance record for (customer_id, financial_year):
  - opening_value (inherited from previous year's closing)
  - closing_value (from this upload's total_current_value)
  - total_invested_during_year
  - total_profit_loss
  - best_performing_asset (highest profit_loss)
  - worst_performing_asset (lowest profit_loss)
```

## Transaction Semantics

**All operations are atomic:** Either the entire ingestion succeeds or the entire transaction rolls back.

If any step fails:
- ✗ Filename invalid → Reject immediately (no database change)
- ✗ CSV missing columns → Reject (no database change)
- ✗ Asset creation fails → Transaction rollback
- ✗ Holdings insertion fails → Transaction rollback

## Data Integrity Guarantees

### 1. Customer Stability
- Customer ID never changes once created
- Customer name can be updated
- All holdings linked to stable customer ID

### 2. Append-Only Holdings
- Each upload creates fresh portfolio snapshot
- Holdings are immutable (never updated/deleted)
- Full historical audit trail preserved

### 3. Asset Reusability
- Assets created once, reused across uploads
- Prevents duplicate asset records
- Single source of truth per asset

### 4. Portfolio Snapshot Integrity
- Each upload has exactly one portfolio_summary
- Summary totals match sum of all holdings
- Summary supports instant dashboard queries

## Example Usage Scenarios

### Scenario 1: New Customer, First Portfolio
```
File: CUST001_RaviKumar_FY2025.csv

Result:
- Customer CUST001 created
- 4 new assets created (TCS, INFY, BTC, GOLD)
- Portfolio upload #5 created
- 4 holdings inserted
- Portfolio summary generated
- Annual performance created for FY 2025
```

### Scenario 2: Existing Customer, New Portfolio
```
File: CUST001_RaviKumar_FY2026.csv

Result:
- Customer CUST001 already exists (reused)
- Assets auto-matched by code (0 new assets)
- Portfolio upload #6 created (different from #5)
- 4 new holdings inserted for upload #6
- Portfolio summary generated for upload #6
- Annual performance created for FY 2026
```

### Scenario 3: Customer Name Update
```
Previous: CUST001_RaviKumar_...csv → customer_name = "Ravi Kumar"
New file: CUST001_RaviK_FY2026.csv (name typo corrected)

Result:
- Customer CUST001 updated: customer_name = "Ravi K"
- Customer ID remains #1 (stable)
- All history preserved
```

## Error Handling

### Client Validation Errors (400)
Reject immediately, no partial writes:
- Invalid filename format
- Missing CSV columns
- Invalid asset type
- Unparseable dates
- Empty file
- Invalid file extension

### Server Errors (500)
Wrapped in transaction, full rollback:
- Database connection issues
- Constraint violations
- Unexpected exceptions

## Testing

Sample CSV files provided:
- `CUST001_RaviKumar_FY2025.csv` - 4 diverse assets (stocks, crypto, commodity)
- `CUST002_AnitaSharma_FY2025.csv` - 3 assets with mixed P&L
- `CUST003_VivekPatel_Q1-2026.csv` - Quarterly portfolio

To test:
```bash
# Test 1: Valid upload
curl -X POST -F "file=@CUST001_RaviKumar_FY2025.csv" \
  http://localhost:8080/api/portfolio/ingest/upload

# Test 2: Invalid filename
curl -X POST -F "file=@invalid_format.csv" \
  http://localhost:8080/api/portfolio/ingest/upload

# Test 3: Missing column
# (Edit CSV to remove a required column, then upload)
```

## Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| "Invalid filename format" | Wrong filename pattern | Use: `CODE_NAME_PERIOD.csv` |
| "Missing required columns" | CSV headers incorrect | Check column names in CSV header row |
| "Invalid asset type" | Typo in asset_type | Use: STOCK, CRYPTO, COMMODITY, ETF, OTHER |
| "Cannot parse date" | Wrong date format | Use: yyyy-MM-dd, dd-MM-yyyy, MM/dd/yyyy, or dd/MM/yyyy |
| "Unexpected error" | Check server logs | Review exception stack trace in logs |
| "Empty file" | Upload failed | Verify file selected and not corrupted |

## Architecture Notes

### Layer Responsibilities

**Controller** (`PortfolioIngestionController`)
- Accept multipart upload
- Basic file validation
- Delegate to service

**Service** (`PortfolioIngestionServiceImpl`)
- Orchestrate entire workflow
- Customer resolution
- Asset resolution
- Holdings insertion
- Summary generation
- Annual performance update
- Transactional boundary

**Utilities**
- `FilenameParser`: Filename → metadata extraction
- `CSVParser`: File content → holding objects

**Repositories**
- Access data layer
- No business logic

### Transaction Model

```
@Transactional
ingestPortfolio() {
    try {
        parseFilename()
        resolveCustomer()
        createUpload()
        parseCSV()
        processHoldings()
        generateSummary()
        updateAnnualPerformance()
        return SUCCESS
    } catch (ValidationException e) {
        // Automatic rollback
        throw e
    }
}
```

## Future Enhancements

Potential improvements:
1. **Batch uploads** - Process multiple files atomically
2. **Duplicate detection** - Prevent re-uploading same file
3. **Partial error handling** - Skip invalid rows, log warnings
4. **Excel validation** - Data quality checks before insertion
5. **Asset matching** - Fuzzy matching for typos
6. **Webhook notifications** - Alert on completion/failure
7. **Audit trail** - Track who uploaded what when
8. **S3 integration** - Upload to cloud storage first
