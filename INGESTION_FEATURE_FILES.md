# File Ingestion Feature - Complete Implementation Map

## 📋 New Files Created (13 Total)

### 1. Dependencies
**File:** `pom.xml` (MODIFIED)
- Added: OpenCSV 5.9 for CSV parsing
- Added: Apache POI 5.2.5 for Excel handling (.xls, .xlsx)

### 2. Custom Exceptions (4 files)

#### `InvalidFileFormatException.java`
- Thrown when filename doesn't match `customerCode_customerName_period.csv` pattern
- HTTP Status: 400 Bad Request
- Example: "Invalid filename format. Expected format: customerCode_customerName_period.csv"

#### `InvalidCSVException.java`
- Thrown when CSV content is invalid or missing required columns
- HTTP Status: 400 Bad Request
- Example: "CSV missing required columns: [current_price, exchange_or_market]"

#### `AssetResolutionException.java`
- Thrown when asset cannot be resolved or created
- HTTP Status: 400 Bad Request
- Example: "Invalid asset type: INVALID"

#### `PortfolioIngestionException.java`
- Thrown for unexpected errors during ingestion
- HTTP Status: 400 Bad Request
- Wraps unexpected exceptions with context

### 3. Data Transfer Objects (1 file)

#### `PortfolioHoldingCSVDTO.java`
- Maps a single CSV row to a portfolio holding object
- Fields:
  - assetCode, assetName, assetType, exchangeOrMarket
  - quantity, buyPrice, currentPrice
  - investedValue, currentValue, profitLoss
  - investmentStartDate, investmentEndDate
- Used internally by CSVParser and PortfolioIngestionService

### 4. Utility Classes (2 files)

#### `FilenameParser.java`
- **Purpose**: Parse and validate uploaded filename
- **Pattern**: `customerCode_customerName_period.csv`
- **Outputs**: FileMetadata object containing:
  - customerCode
  - customerName
  - periodLabel
  - periodType (ANNUAL, QUARTERLY, or CUSTOM)
- **Validation**:
  - Filename format check
  - File extension check (.csv, .xlsx, .xls)
  - Empty component check
  - Automatic period type detection

#### `CSVParser.java`
- **Purpose**: Parse CSV/Excel file content into portfolio holdings
- **Supports**: .csv, .xlsx (Excel 2007+), .xls (Excel 97-2003)
- **Features**:
  - Column header validation (case-insensitive)
  - Required column check (12 columns)
  - Row-by-row parsing with error context
  - Multiple date format support
  - Type conversion (BigDecimal, LocalDate, Enum)
  - Empty row skipping
  - Detailed error messages
- **Output**: List<PortfolioHoldingCSVDTO>

### 5. Service Layer (3 files)

#### `PortfolioIngestionService.java` (Interface)
- Contract for portfolio ingestion service
- Single method: `PortfolioIngestionResult ingestPortfolio(MultipartFile file)`

#### `PortfolioIngestionServiceImpl.java`
- **Scope**: @Transactional (all operations atomic)
- **Dependencies Injected**:
  - CustomerRepository
  - AssetRepository
  - PortfolioUploadRepository
  - PortfolioHoldingRepository
  - PortfolioSummaryRepository
  - AnnualPerformanceRepository
- **Methods**:
  1. `ingestPortfolio()` - Main orchestration method
  2. `resolveOrCreateCustomer()` - Customer resolution logic
  3. `createPortfolioUpload()` - Upload record creation
  4. `parseFileContent()` - File parsing delegation
  5. `processHolding()` - Per-holding processing
  6. `resolveOrCreateAsset()` - Asset resolution logic
  7. `generatePortfolioSummary()` - Summary aggregation
  8. `updateAnnualPerformance()` - Annual P&L calculation
  9. `extractFinancialYear()` - Year extraction from period label

#### `PortfolioIngestionResult.java`
- Response DTO for ingestion results
- Fields:
  - uploadId (Long) - New portfolio_uploads.upload_id
  - customerId (Long) - Customer identifier
  - customerCode (String) - Customer code
  - customerName (String) - Customer name
  - periodLabel (String) - Period label from filename
  - holdingsCount (Integer) - Number of holdings inserted
  - message (String) - Result message
  - success (Boolean) - Success indicator

### 6. REST Controller (1 file)

#### `PortfolioIngestionController.java`
- **Endpoint**: `POST /api/portfolio/ingest/upload`
- **Request**: Multipart file upload (parameter: "file")
- **Response**: PortfolioIngestionResult (JSON)
- **Responsibilities**:
  - Accept multipart file
  - Basic file validation (not empty, has filename)
  - Delegate to PortfolioIngestionService
  - Return result
  - **No business logic** - thin adapter layer

### 7. Exception Handler (MODIFIED)

#### `GlobalExceptionHandler.java` (MODIFIED)
- Added 4 new exception handlers:
  - `@ExceptionHandler(InvalidFileFormatException.class)` → 400
  - `@ExceptionHandler(InvalidCSVException.class)` → 400
  - `@ExceptionHandler(AssetResolutionException.class)` → 400
  - `@ExceptionHandler(PortfolioIngestionException.class)` → 400
- Maintains existing exception handlers
- Provides consistent error response format

### 8. Documentation Files (4 files)

#### `FILE_INGESTION_GUIDE.md`
- **Purpose**: Complete user documentation
- **Sections**:
  - Overview
  - File naming convention (mandatory)
  - Filename parsing rules
  - CSV file format
  - Date format support
  - API endpoint details
  - Processing workflow (step-by-step)
  - Transaction semantics
  - Data integrity guarantees
  - Example usage scenarios
  - Error handling
  - Testing
  - Troubleshooting
  - Architecture notes
  - Future enhancements

#### `FILE_INGESTION_IMPLEMENTATION.md`
- **Purpose**: Implementation summary for developers
- **Sections**:
  - Overview
  - Files created/modified
  - Ingestion workflow
  - Key features
  - API endpoint
  - CSV column requirements
  - Example usage
  - Integration points
  - Documentation references
  - Testing
  - Production readiness checklist

#### `QUICK_START_INGESTION.md`
- **Purpose**: 5-minute setup and usage guide
- **Contents**:
  - Build instructions
  - Startup steps
  - Sample file testing
  - File format quick reference
  - Common errors & solutions
  - Query endpoints
  - Key guarantees

#### `INGESTION_FEATURE_FILES.md` (this file)
- **Purpose**: Complete map of all files and their purposes

### 9. Test/Sample Files (3 files)

#### `CUST001_RaviKumar_FY2025.csv`
- **Customer**: CUST001 / Ravi Kumar
- **Period**: FY2025 (Annual)
- **Holdings**: 4
  - TCS (STOCK, NSE) - 100 units
  - INFY (STOCK, NSE) - 75 units
  - BTC (CRYPTO, BINANCE) - 0.5 units
  - GOLD (COMMODITY, MCX) - 50 units
- **Use Case**: Test annual portfolio with diverse assets

#### `CUST002_AnitaSharma_FY2025.csv`
- **Customer**: CUST002 / Anita Sharma
- **Period**: FY2025 (Annual)
- **Holdings**: 3
  - SBIN (STOCK, NSE) - 200 units (profitable)
  - RELIANCE (STOCK, NSE) - 50 units (loss)
  - COPPER (COMMODITY, MCX) - 100 units (profitable)
- **Use Case**: Test mixed P&L handling

#### `CUST003_VivekPatel_Q1-2026.csv`
- **Customer**: CUST003 / Vivek Patel
- **Period**: Q1-2026 (Quarterly)
- **Holdings**: 3
  - MARUTI (STOCK, NSE) - 150 units (loss)
  - ICICIBANK (STOCK, NSE) - 100 units (profitable)
  - ETH (CRYPTO, BINANCE) - 1 unit (profitable)
- **Use Case**: Test quarterly portfolio and new customer creation

## 🔄 Data Flow Diagram

```
User Upload
    ↓
POST /api/portfolio/ingest/upload
    ↓
PortfolioIngestionController
    ├─ Validate file not empty
    ├─ Validate has filename
    └─ Delegate to service
        ↓
PortfolioIngestionServiceImpl (@Transactional)
    ├─ FilenameParser.parseFilename()
    │  └─ Extract: code, name, period, periodType
    ├─ resolveOrCreateCustomer()
    │  └─ CustomerRepository (lookup/create/update)
    ├─ createPortfolioUpload()
    │  └─ PortfolioUploadRepository (insert)
    ├─ parseFileContent()
    │  ├─ CSVParser.parseCSV() or parseExcel()
    │  └─ List<PortfolioHoldingCSVDTO>
    ├─ FOR EACH holding:
    │  ├─ resolveOrCreateAsset()
    │  │  └─ AssetRepository (lookup/create)
    │  └─ processHolding()
    │     └─ PortfolioHoldingRepository (insert)
    ├─ generatePortfolioSummary()
    │  └─ PortfolioSummaryRepository (insert)
    ├─ IF periodType == ANNUAL:
    │  └─ updateAnnualPerformance()
    │     └─ AnnualPerformanceRepository (insert/update)
    └─ Return PortfolioIngestionResult
        ↓
GlobalExceptionHandler (if error)
    ├─ InvalidFileFormatException → 400
    ├─ InvalidCSVException → 400
    ├─ AssetResolutionException → 400
    ├─ PortfolioIngestionException → 400
    └─ Other exceptions → 500
        ↓
Response (JSON)
    ↓
Client
```

## 🗄️ Database Operations (In Transaction)

```
1. INSERT/UPDATE customers
2. INSERT portfolio_uploads
3. FOR EACH holding:
   a. INSERT/SKIP assets
   b. INSERT portfolio_holdings
4. INSERT portfolio_summary
5. IF annual: INSERT/UPDATE annual_performance
6. COMMIT (or ROLLBACK on error)
```

## 📦 Integration with Existing System

| Component | Usage |
|-----------|-------|
| CustomerRepository | Lookup/create/update customers |
| AssetRepository | Lookup/create assets |
| PortfolioUploadRepository | Insert upload records |
| PortfolioHoldingRepository | Insert holding records |
| PortfolioSummaryRepository | Insert summary records |
| AnnualPerformanceRepository | Insert/update performance |
| Customer (entity) | Read/write customer |
| Asset (entity) | Read/write asset |
| PortfolioUpload (entity) | Write-only |
| PortfolioHolding (entity) | Write-only |
| PortfolioSummary (entity) | Write-only |
| AnnualPerformance (entity) | Read/write |
| Existing Controllers | Unchanged, still work |
| Existing Services | Unchanged, still work |
| Existing Read APIs | Still functional |

## ✅ Validation Flow

### Filename Validation
1. Check not null/empty
2. Check has extension (.csv, .xlsx, .xls)
3. Remove extension
4. Split by underscore → [code, name, period]
5. Validate 3 parts present
6. Validate parts not empty
7. Derive period type

### CSV Column Validation
1. Read header row
2. Normalize column names (lowercase)
3. Check all 12 required columns present
4. Build column index map

### Data Row Validation
1. Skip empty rows
2. For each column:
   - Extract string value
   - Validate not empty (if required)
   - Parse type (BigDecimal, LocalDate, Enum)
   - Handle parsing errors with row context

## 🎯 Key Design Decisions

1. **FilenameParser as static utility** - Stateless, pure function
2. **CSVParser as static utility** - Stateless, supports both CSV and Excel
3. **PortfolioIngestionServiceImpl as transactional bean** - Orchestrates workflow atomically
4. **Customer resolution** - Lookup by code, create if missing
5. **Asset resolution** - Lookup by code (unique), create if missing
6. **Holdings are append-only** - Each upload creates fresh snapshot
7. **Annual performance is upsert** - Insert if new year, update if existing
8. **Period type auto-detection** - FY/ANNUAL → ANNUAL, Q[1-4] → QUARTERLY, else CUSTOM
9. **Financial year extraction** - Regex to find 4-digit year in period label

## 🚀 Deployment Checklist

- [x] Dependencies added to pom.xml
- [x] All exceptions created
- [x] DTOs created
- [x] Utilities created and tested
- [x] Service interface created
- [x] Service implementation created
- [x] Controller created
- [x] Exception handler updated
- [x] Documentation written (3 guides)
- [x] Sample CSV files created
- [x] Code follows existing patterns
- [x] No breaking changes
- [x] Transaction safety verified
- [x] Error handling comprehensive
- [x] Ready for production deployment

## 📚 Documentation Links

- **For Users**: [FILE_INGESTION_GUIDE.md](FILE_INGESTION_GUIDE.md)
- **For Developers**: [FILE_INGESTION_IMPLEMENTATION.md](FILE_INGESTION_IMPLEMENTATION.md)
- **Quick Setup**: [QUICK_START_INGESTION.md](QUICK_START_INGESTION.md)
- **This File**: [INGESTION_FEATURE_FILES.md](INGESTION_FEATURE_FILES.md)

## 🎓 Learning Resources

- **REST API Design**: See PortfolioIngestionController
- **Filename Parsing**: See FilenameParser
- **CSV Parsing**: See CSVParser (supports .csv, .xlsx, .xls)
- **Service Orchestration**: See PortfolioIngestionServiceImpl
- **Transaction Management**: See @Transactional in service
- **Exception Handling**: See GlobalExceptionHandler
- **DTOs**: See PortfolioHoldingCSVDTO, PortfolioIngestionResult

## 📞 Support

Issues or questions? Check:
1. [QUICK_START_INGESTION.md](QUICK_START_INGESTION.md) - Common errors & solutions
2. [FILE_INGESTION_GUIDE.md](FILE_INGESTION_GUIDE.md) - Comprehensive reference
3. Server logs - Exception details
4. Sample CSV files - Working examples
