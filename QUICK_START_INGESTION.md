# File Ingestion Quick Start Guide

## 5-Minute Setup

### 1. Build the Project
```bash
cd springboot/CapitalX/CapitalX
mvn clean package
```

### 2. Start the Application
```bash
java -jar target/CapitalX-0.0.1-SNAPSHOT.jar
```

Application runs on `http://localhost:8080`

### 3. Test with Sample File
Use one of the provided sample CSV files:
- `CUST001_RaviKumar_FY2025.csv`
- `CUST002_AnitaSharma_FY2025.csv`
- `CUST003_VivekPatel_Q1-2026.csv`

#### Using cURL:
```bash
curl -X POST \
  -F "file=@CUST001_RaviKumar_FY2025.csv" \
  http://localhost:8080/api/portfolio/ingest/upload
```

#### Expected Response (200 OK):
```json
{
  "uploadId": 1,
  "customerId": 1,
  "customerCode": "CUST001",
  "customerName": "Ravi Kumar",
  "periodLabel": "FY2025",
  "holdingsCount": 4,
  "message": "Portfolio ingestion successful",
  "success": true
}
```

## File Format Requirements

### Filename Format (MANDATORY)
```
customerCode_customerName_period.csv
```

Examples:
- ✅ `CUST001_RaviKumar_FY2025.csv`
- ✅ `CUST002_AnitaSharma_Q1-2026.csv`
- ❌ `RaviKumar_FY2025.csv` (missing customer code)
- ❌ `CUST001_Ravi_Kumar_FY2025.csv` (underscores in name not allowed)

### CSV Columns Required
All of these must be present in the header row:

```
asset_code, asset_name, asset_type, exchange_or_market,
quantity, buy_price, current_price,
invested_value, current_value, profit_loss,
investment_start_date, investment_end_date
```

### Valid Asset Types
- `STOCK` (e.g., TCS, INFY)
- `CRYPTO` (e.g., BTC, ETH)
- `COMMODITY` (e.g., GOLD, COPPER)
- `ETF` (e.g., Gold ETF, Bank ETF)
- `OTHER` (for other types)

### Date Format
Use any of these formats:
- `yyyy-MM-dd` (preferred: 2024-04-01)
- `dd-MM-yyyy` (2024-04-01)
- `MM/dd/yyyy` (04/01/2024)
- `dd/MM/yyyy` (01/04/2024)

## API Endpoint

```
POST /api/portfolio/ingest/upload
Content-Type: multipart/form-data
Parameter name: file
```

## Common Errors & Solutions

| Error | Cause | Fix |
|-------|-------|-----|
| "Invalid filename format" | Filename doesn't match pattern | Use: `CODE_NAME_PERIOD.csv` |
| "CSV missing required columns" | Missing column in CSV | Check all 12 columns present |
| "Invalid asset type: SHARES" | Unknown asset type | Use: STOCK, CRYPTO, COMMODITY, ETF, OTHER |
| "Cannot parse date" | Wrong date format | Use: yyyy-MM-dd or dd-MM-yyyy |
| "File is empty" | No data rows in CSV | Add at least one data row after header |

## What Gets Created

When you upload a file, the system creates:

1. **Customer** (if new) - Identified by customer_code
2. **Assets** (if new) - Identified by asset_code
3. **Portfolio Upload** - Historical snapshot record
4. **Portfolio Holdings** - One per CSV row (immutable)
5. **Portfolio Summary** - Aggregated totals
6. **Annual Performance** (if annual) - P&L analytics

## Query the Data

After ingestion, query with existing endpoints:

```bash
# Get customer profile
curl http://localhost:8080/api/portfolio/customer/1/profile

# Get latest portfolio
curl http://localhost:8080/api/portfolio/customer/1/portfolio/latest

# Get portfolio history
curl http://localhost:8080/api/portfolio/customer/1/portfolio/history

# Get annual performance
curl http://localhost:8080/api/portfolio/customer/1/annual-report/2025
```

## Next Steps

1. **Read** `FILE_INGESTION_GUIDE.md` for comprehensive documentation
2. **Test** with sample CSV files
3. **Create** your own CSV files matching the format
4. **Deploy** to production when ready

## Key Guarantees

✅ **Atomic Ingestion** - Either entire file succeeds or entire transaction rolls back  
✅ **Customer Stability** - Customer ID never changes once created  
✅ **Append-Only Holdings** - Each upload creates fresh snapshot, never modified  
✅ **Asset Reusability** - Assets created once, reused across customers  
✅ **Historical Accuracy** - Full audit trail of all uploads

## Questions?

See **FILE_INGESTION_GUIDE.md** for detailed information on:
- Processing workflow
- Data integrity guarantees
- Transaction semantics
- Error handling
- Architecture notes
- Future enhancements
