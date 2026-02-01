# CapitalX Analytics API Reference

## Base URL
```
http://localhost:8080/api
```

---

## Customer Portfolio API

### Base Path: `/portfolio/customer/{customerId}`

#### 1. Customer Profile
```
GET /portfolio/customer/{customerId}/profile
```
**Description:** Get customer details and metadata  
**Response:** `CustomerOverviewDTO`

**Example:**
```bash
curl -X GET http://localhost:8080/api/portfolio/customer/1/profile
```

**Response (200 OK):**
```json
{
  "customerId": 1,
  "customerCode": "CLIENT001",
  "customerName": "John Doe",
  "createdAt": "2025-12-01T10:00:00Z"
}
```

---

#### 2. Latest Portfolio Snapshot
```
GET /portfolio/customer/{customerId}/portfolio/latest
```
**Description:** Get current portfolio worth and P&L  
**Answer:** "What is Client A's portfolio worth today?"  
**Response:** `PortfolioSnapshotDTO`

**Example:**
```bash
curl -X GET http://localhost:8080/api/portfolio/customer/1/portfolio/latest
```

**Response (200 OK):**
```json
{
  "uploadId": 5,
  "periodLabel": "Q4-2025",
  "periodType": "QUARTERLY",
  "uploadDate": "2026-01-31T21:31:45Z",
  "fileName": "portfolio_q4_2025.xlsx",
  "totalInvestedValue": 1000000.00,
  "totalCurrentValue": 1250000.00,
  "totalProfitLoss": 250000.00,
  "numberOfAssets": 25,
  "numberOfProfitableAssets": 20,
  "numberOfLossAssets": 5
}
```

---

#### 3. Portfolio History
```
GET /portfolio/customer/{customerId}/portfolio/history
```
**Description:** All portfolio snapshots in reverse chronological order  
**Response:** `List<PortfolioSnapshotDTO>`

**Example:**
```bash
curl -X GET http://localhost:8080/api/portfolio/customer/1/portfolio/history
```

---

#### 4. Specific Portfolio Snapshot
```
GET /portfolio/customer/{customerId}/portfolio/snapshot/{uploadId}
```
**Description:** Get portfolio data for a specific upload  
**Response:** `PortfolioSnapshotDTO`

**Example:**
```bash
curl -X GET http://localhost:8080/api/portfolio/customer/1/portfolio/snapshot/5
```

---

#### 5. Portfolio by Period Type
```
GET /portfolio/customer/{customerId}/portfolio/by-period?periodType=QUARTERLY
```
**Description:** Filter snapshots by period type  
**Query Parameters:**
- `periodType` (required): `QUARTERLY`, `ANNUAL`, `CUSTOM`

**Response:** `List<PortfolioSnapshotDTO>`

**Examples:**
```bash
# Quarterly snapshots
curl -X GET "http://localhost:8080/api/portfolio/customer/1/portfolio/by-period?periodType=QUARTERLY"

# Annual snapshots
curl -X GET "http://localhost:8080/api/portfolio/customer/1/portfolio/by-period?periodType=ANNUAL"
```

---

#### 6. Portfolio by Year
```
GET /portfolio/customer/{customerId}/portfolio/by-year/{year}
```
**Description:** Get all snapshots for a specific year  
**Response:** `List<PortfolioSnapshotDTO>`

**Example:**
```bash
curl -X GET http://localhost:8080/api/portfolio/customer/1/portfolio/by-year/2025
```

---

#### 7. Latest Holdings
```
GET /portfolio/customer/{customerId}/holdings/latest
```
**Description:** Current investment holdings  
**Response:** `List<AssetPerformanceDTO>`

**Example:**
```bash
curl -X GET http://localhost:8080/api/portfolio/customer/1/holdings/latest
```

---

#### 8. Active Holdings
```
GET /portfolio/customer/{customerId}/holdings/active
```
**Description:** Holdings not yet exited (ongoing investments)  
**Response:** `List<AssetPerformanceDTO>`

**Example:**
```bash
curl -X GET http://localhost:8080/api/portfolio/customer/1/holdings/active
```

---

#### 9. Exited Holdings
```
GET /portfolio/customer/{customerId}/holdings/exited
```
**Description:** Sold or exited investments  
**Response:** `List<AssetPerformanceDTO>`

**Example:**
```bash
curl -X GET http://localhost:8080/api/portfolio/customer/1/holdings/exited
```

---

#### 10. Profitable Holdings
```
GET /portfolio/customer/{customerId}/holdings/profitable
```
**Description:** Assets with positive P&L  
**Answer:** "Which assets of Client A are making money?"  
**Response:** `List<AssetPerformanceDTO>`

**Example:**
```bash
curl -X GET http://localhost:8080/api/portfolio/customer/1/holdings/profitable
```

**Response (200 OK):**
```json
[
  {
    "holdingId": 101,
    "assetCode": "TCS",
    "assetName": "Tata Consultancy Services",
    "assetType": "STOCK",
    "quantity": 100.0000,
    "buyPrice": 3500.000000,
    "currentPrice": 4200.000000,
    "investedValue": 350000.00,
    "currentValue": 420000.00,
    "profitLoss": 70000.00,
    "profitLossPercentage": 20.0000,
    "investmentStatus": "ACTIVE"
  },
  {
    "holdingId": 102,
    "assetCode": "INFY",
    "assetName": "Infosys Limited",
    "assetType": "STOCK",
    "quantity": 50.0000,
    "buyPrice": 1800.000000,
    "currentPrice": 2000.000000,
    "investedValue": 90000.00,
    "currentValue": 100000.00,
    "profitLoss": 10000.00,
    "profitLossPercentage": 11.1111,
    "investmentStatus": "ACTIVE"
  }
]
```

---

#### 11. Loss-Making Holdings
```
GET /portfolio/customer/{customerId}/holdings/losses
```
**Description:** Assets with negative P&L  
**Answer:** "Which assets of Client A are losing money?"  
**Response:** `List<AssetPerformanceDTO>`

**Example:**
```bash
curl -X GET http://localhost:8080/api/portfolio/customer/1/holdings/losses
```

---

#### 12. Holdings by Asset Type
```
GET /portfolio/customer/{customerId}/holdings/by-type/{assetType}
```
**Description:** Filter holdings by asset category  
**Path Parameters:**
- `assetType`: `STOCK`, `CRYPTO`, `COMMODITY`, `ETF`, `OTHER`

**Response:** `List<AssetPerformanceDTO>`

**Examples:**
```bash
# Stock holdings
curl -X GET http://localhost:8080/api/portfolio/customer/1/holdings/by-type/STOCK

# Crypto holdings
curl -X GET http://localhost:8080/api/portfolio/customer/1/holdings/by-type/CRYPTO

# Commodity holdings
curl -X GET http://localhost:8080/api/portfolio/customer/1/holdings/by-type/COMMODITY
```

---

#### 13. Annual Performance Report
```
GET /portfolio/customer/{customerId}/annual-performance/{financialYear}
```
**Description:** Annual P&L statement for specific year  
**Answer:** "How did Client A perform in FY-2025?"  
**Response:** `AnnualReportDTO`

**Example:**
```bash
curl -X GET http://localhost:8080/api/portfolio/customer/1/annual-performance/2025
```

**Response (200 OK):**
```json
{
  "annualPerfId": 1,
  "financialYear": 2025,
  "openingValue": 1000000.00,
  "closingValue": 1250000.00,
  "totalInvestedDuringYear": 150000.00,
  "totalProfitLoss": 250000.00,
  "returnPercentage": 25.0000,
  "bestPerformingAsset": "BTC",
  "worstPerformingAsset": "GOLD"
}
```

---

#### 14. All Annual Performance Records
```
GET /portfolio/customer/{customerId}/annual-performance/all
```
**Description:** Year-over-year performance history  
**Response:** `List<AnnualReportDTO>`

**Example:**
```bash
curl -X GET http://localhost:8080/api/portfolio/customer/1/annual-performance/all
```

---

#### 15. Portfolio Summary
```
GET /portfolio/customer/{customerId}/summary
```
**Description:** Quick portfolio value & P&L overview  
**Response:** `PortfolioSnapshotDTO`

**Example:**
```bash
curl -X GET http://localhost:8080/api/portfolio/customer/1/summary
```

---

## Portfolio Manager Overview API

### Base Path: `/portfolio/manager`

#### 1. Complete Manager Overview
```
GET /portfolio/manager/overview
```
**Description:** Bird's-eye view of entire book  
**Answer:** "How is my entire book performing?"  
**Response:** `PortfolioAnalyticsDTO`

**Example:**
```bash
curl -X GET http://localhost:8080/api/portfolio/manager/overview
```

**Response (200 OK):**
```json
{
  "totalPortfolioValue": 50000000.00,
  "totalInvestedValue": 45000000.00,
  "totalProfitLoss": 5000000.00,
  "numberOfCustomers": 25,
  "numberOfAssets": 150,
  "topPerformingCustomers": [...],
  "bottomPerformingCustomers": [...],
  "topAssets": [...],
  "riskAssets": [...]
}
```

---

#### 2. Manager Overview by Year
```
GET /portfolio/manager/overview/by-year/{year}
```
**Description:** Manager view filtered by year  
**Response:** `PortfolioAnalyticsDTO`

**Example:**
```bash
curl -X GET http://localhost:8080/api/portfolio/manager/overview/by-year/2025
```

---

#### 3. Top Performing Customers
```
GET /portfolio/manager/top-customers?limit=5
```
**Description:** Best performing clients  
**Query Parameters:**
- `limit` (optional, default=5): Number of customers to return

**Response:** `List<CustomerPerformanceDTO>`

**Examples:**
```bash
# Top 5 (default)
curl -X GET http://localhost:8080/api/portfolio/manager/top-customers

# Top 10
curl -X GET "http://localhost:8080/api/portfolio/manager/top-customers?limit=10"

# Top 20
curl -X GET "http://localhost:8080/api/portfolio/manager/top-customers?limit=20"
```

**Response (200 OK):**
```json
[
  {
    "customerId": 5,
    "customerCode": "PRIME001",
    "customerName": "Premium Client",
    "portfolioValue": 5000000.00,
    "totalProfitLoss": 1000000.00,
    "returnPercentage": 25.0000,
    "numberOfAssets": 30
  },
  {
    "customerId": 3,
    "customerCode": "CORP002",
    "customerName": "Corporate Client",
    "portfolioValue": 3500000.00,
    "totalProfitLoss": 700000.00,
    "returnPercentage": 25.0000,
    "numberOfAssets": 20
  }
]
```

---

#### 4. Bottom Performing Customers
```
GET /portfolio/manager/bottom-customers?limit=5
```
**Description:** Clients dragging returns  
**Answer:** "Which clients are dragging returns?"  
**Query Parameters:**
- `limit` (optional, default=5): Number of customers to return

**Response:** `List<CustomerPerformanceDTO>`

**Example:**
```bash
curl -X GET "http://localhost:8080/api/portfolio/manager/bottom-customers?limit=5"
```

---

#### 5. Top Performing Assets
```
GET /portfolio/manager/top-assets?limit=5
```
**Description:** Best performing assets across all portfolios  
**Query Parameters:**
- `limit` (optional, default=5): Number of assets to return

**Response:** `List<AssetExposureDTO>`

**Example:**
```bash
curl -X GET "http://localhost:8080/api/portfolio/manager/top-assets?limit=10"
```

**Response (200 OK):**
```json
[
  {
    "assetId": 1,
    "assetCode": "BTC",
    "assetName": "Bitcoin",
    "assetType": "CRYPTO",
    "totalValue": 5000000.00,
    "numberOfCustomersHolding": 15,
    "averageReturn": 45.5000,
    "riskLevel": "HIGH"
  },
  {
    "assetId": 2,
    "assetCode": "ETH",
    "assetName": "Ethereum",
    "assetType": "CRYPTO",
    "totalValue": 3000000.00,
    "numberOfCustomersHolding": 12,
    "averageReturn": 35.0000,
    "riskLevel": "HIGH"
  }
]
```

---

#### 6. Risky/Underperforming Assets
```
GET /portfolio/manager/risky-assets?limit=5
```
**Description:** Underperforming or risky assets  
**Answer:** "Which assets are risky across portfolios?"  
**Query Parameters:**
- `limit` (optional, default=5): Number of assets to return

**Response:** `List<AssetExposureDTO>`

**Example:**
```bash
curl -X GET "http://localhost:8080/api/portfolio/manager/risky-assets?limit=5"
```

---

#### 7. Total Portfolio Value
```
GET /portfolio/manager/total-portfolio-value
```
**Description:** Aggregate value of entire book (all customers)  
**Response:** `PortfolioAnalyticsDTO`

**Example:**
```bash
curl -X GET http://localhost:8080/api/portfolio/manager/total-portfolio-value
```

---

#### 8. Asset Exposure
```
GET /portfolio/manager/asset-exposure?limit=10
```
**Description:** Which assets are most exposed across portfolios?  
**Query Parameters:**
- `limit` (optional, default=10): Number of assets to return

**Response:** `List<AssetExposureDTO>`

**Example:**
```bash
curl -X GET "http://localhost:8080/api/portfolio/manager/asset-exposure?limit=15"
```

---

#### 9. Annual P&L Across All Customers
```
GET /portfolio/manager/annual-pnl
```
**Description:** Aggregate annual performance metrics  
**Response:** `PortfolioAnalyticsDTO`

**Example:**
```bash
curl -X GET http://localhost:8080/api/portfolio/manager/annual-pnl
```

---

#### 10. Concentration Analysis
```
GET /portfolio/manager/concentration-analysis
```
**Description:** Customer concentration risk analysis  
**Response:** `List<CustomerPerformanceDTO>`

**Example:**
```bash
curl -X GET http://localhost:8080/api/portfolio/manager/concentration-analysis
```

---

## Error Responses

### 404 - Not Found
```json
{
  "timestamp": "2026-01-31T21:40:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Customer not found with id: 999"
}
```

### 400 - Bad Request
```json
{
  "timestamp": "2026-01-31T21:40:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid period type: INVALID_PERIOD"
}
```

### 500 - Internal Server Error
```json
{
  "timestamp": "2026-01-31T21:40:00Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred: ..."
}
```

---

## HTTP Status Codes

| Code | Meaning | Usage |
|------|---------|-------|
| **200** | OK | Successful read request |
| **400** | Bad Request | Invalid parameters or period type |
| **404** | Not Found | Customer, portfolio, or data not found |
| **409** | Conflict | Data consistency issue |
| **500** | Server Error | Unexpected internal error |

---

## Common Use Cases & Queries

### Use Case 1: Daily Portfolio Review
```bash
# Get latest portfolio value and asset breakdown
curl http://localhost:8080/api/portfolio/customer/1/portfolio/latest
curl http://localhost:8080/api/portfolio/customer/1/holdings/latest
```

### Use Case 2: Monthly P&L Analysis
```bash
# Get annual performance including best/worst assets
curl http://localhost:8080/api/portfolio/customer/1/annual-performance/2025
```

### Use Case 3: Identifying Problem Assets
```bash
# Get loss-making holdings
curl http://localhost:8080/api/portfolio/customer/1/holdings/losses
```

### Use Case 4: Manager Dashboard
```bash
# Get complete overview of entire book
curl http://localhost:8080/api/portfolio/manager/overview

# Get top and bottom performers
curl http://localhost:8080/api/portfolio/manager/top-customers?limit=5
curl http://localhost:8080/api/portfolio/manager/bottom-customers?limit=5

# Get risky assets
curl http://localhost:8080/api/portfolio/manager/risky-assets?limit=5
```

### Use Case 5: Client Segment Analysis
```bash
# Get top performing customers
curl http://localhost:8080/api/portfolio/manager/top-customers?limit=10

# Analyze asset concentration
curl http://localhost:8080/api/portfolio/manager/asset-exposure?limit=20
```

---

## Performance Notes

- **Snapshot queries** return latest data from database (append-only design)
- **Aggregations** (manager overview) may take longer with large datasets
- Consider adding **database indexes** on frequently queried columns:
  - `portfolio_uploads.customer_id`
  - `portfolio_holdings.asset_id`
  - `annual_performance.financial_year`

---

## Rate Limits

Currently **no rate limiting** implemented. Add in future if needed.

---

## Authentication

Currently **no authentication** required. Add JWT or OAuth2 in future if needed.

---

*Last Updated: February 1, 2026*  
*CapitalX Portfolio Management System*  
*Read-Only Analytics API*
