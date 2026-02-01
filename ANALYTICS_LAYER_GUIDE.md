# CapitalX Analytics Layer Implementation Guide

## Overview

This document describes the complete **Read-Only Analytics Layer** implemented for the CapitalX Portfolio Management System. The implementation follows the architectural patterns and best practices from the reference FinDemyApp_Server project.

---

## Architecture & Layer Stack

```
┌──────────────────────────────────────────────────────────┐
│           REST Controllers (HTTP Endpoints)              │
│  ├─ CustomerPortfolioController                          │
│  └─ PortfolioManagerOverviewController                   │
├──────────────────────────────────────────────────────────┤
│              Service Layer (Business Logic)              │
│  ├─ CustomerReadService / CustomerReadServiceImpl        │
│  ├─ PortfolioSnapshotService / Impl                      │
│  ├─ PortfolioHoldingReadService / Impl                   │
│  ├─ PortfolioSummaryReadService / Impl                   │
│  ├─ AnnualPerformanceReadService / Impl                  │
│  └─ PortfolioAnalyticsService / Impl (Cross-Customer)    │
├──────────────────────────────────────────────────────────┤
│           Mapper Layer (Entity ↔ DTO Transform)          │
│  ├─ CustomerMapper                                       │
│  ├─ PortfolioMapper                                      │
│  ├─ AssetPerformanceMapper                               │
│  └─ AnnualPerformanceMapper                              │
├──────────────────────────────────────────────────────────┤
│              DTO Layer (API Contracts)                   │
│  ├─ CustomerOverviewDTO                                 │
│  ├─ PortfolioSnapshotDTO                                │
│  ├─ AssetPerformanceDTO                                 │
│  ├─ AnnualReportDTO                                     │
│  ├─ PortfolioAnalyticsDTO                               │
│  ├─ CustomerPerformanceDTO                              │
│  └─ AssetExposureDTO                                    │
├──────────────────────────────────────────────────────────┤
│         Exception Layer (Error Handling)                 │
│  ├─ CustomerNotFoundException                           │
│  ├─ PortfolioSnapshotNotFoundException                   │
│  ├─ AnnualPerformanceNotAvailableException               │
│  ├─ InvalidPeriodException                              │
│  ├─ DataConsistencyException                            │
│  └─ GlobalExceptionHandler                              │
├──────────────────────────────────────────────────────────┤
│        Repository Layer (Data Access)                    │
│  ├─ CustomerRepository                                  │
│  ├─ AssetRepository                                     │
│  ├─ PortfolioUploadRepository                           │
│  ├─ PortfolioHoldingRepository                          │
│  ├─ PortfolioSummaryRepository                          │
│  └─ AnnualPerformanceRepository                         │
├──────────────────────────────────────────────────────────┤
│         Entity/Bean Layer (Database Models)              │
│  ├─ Customer                                            │
│  ├─ Asset                                               │
│  ├─ PortfolioUpload                                     │
│  ├─ PortfolioHolding                                    │
│  ├─ PortfolioSummary                                    │
│  └─ AnnualPerformance                                   │
├──────────────────────────────────────────────────────────┤
│           MySQL Database (Append-Only Design)            │
│  ├─ customers                                           │
│  ├─ assets                                              │
│  ├─ portfolio_uploads                                   │
│  ├─ portfolio_holdings                                  │
│  ├─ portfolio_summary                                   │
│  └─ annual_performance                                  │
└──────────────────────────────────────────────────────────┘
```

---

## Implemented Components

### 1. Exception Handling (5 Custom Exceptions)

**Location:** `src/main/java/com/example/CapitalX/exceptions/`

| Exception | HTTP Status | Usage |
|-----------|------------|-------|
| `CustomerNotFoundException` | 404 | Customer ID not found |
| `PortfolioSnapshotNotFoundException` | 404 | Portfolio upload/snapshot not found |
| `AnnualPerformanceNotAvailableException` | 404 | Annual performance data missing |
| `InvalidPeriodException` | 400 | Invalid period type/parameters |
| `DataConsistencyException` | 409 | Data integrity violation |

**GlobalExceptionHandler:** Centralized exception handling with clean API responses.

---

### 2. Data Transfer Objects (7 DTOs)

**Location:** `src/main/java/com/example/CapitalX/dto/`

```java
CustomerOverviewDTO           // Customer profile data
PortfolioSnapshotDTO          // Portfolio snapshot (upload + summary combined)
AssetPerformanceDTO           // Individual holding performance
AnnualReportDTO              // Annual performance report
PortfolioAnalyticsDTO        // Manager-level aggregated analytics
CustomerPerformanceDTO       // Customer-level performance metrics
AssetExposureDTO            // Asset distribution & risk analysis
```

---

### 3. Mapper Layer (4 Mappers)

**Location:** `src/main/java/com/example/CapitalX/mapper/`

| Mapper | Purpose |
|--------|---------|
| `CustomerMapper` | Customer → CustomerOverviewDTO |
| `PortfolioMapper` | PortfolioUpload + PortfolioSummary → PortfolioSnapshotDTO |
| `AssetPerformanceMapper` | PortfolioHolding → AssetPerformanceDTO (with calculations) |
| `AnnualPerformanceMapper` | AnnualPerformance → AnnualReportDTO (with % calculations) |

**Key Features:**
- Pure transformation (no business logic)
- Includes calculated fields (e.g., profit/loss percentages)
- Null-safe operations
- Status determination (ACTIVE vs EXITED)

---

### 4. Service Layer (6 Services)

**Location:** `src/main/java/com/example/CapitalX/service/`

#### 4.1 CustomerReadService
```java
getAllCustomers()              // List all customers
getCustomerById(Long)          // Fetch by ID
getCustomerByCode(String)      // Fetch by code
customerExists(Long)           // Validation check
```

#### 4.2 PortfolioSnapshotService
```java
getLatestSnapshot(Long)                // Latest portfolio for customer
getSnapshotByUploadId(Long)            // Specific upload
getSnapshotsByCustomer(Long)           // All snapshots
getSnapshotsByPeriod(Long, Period)     // Filter by period type
getSnapshotsByYear(Long, Year)         // Filter by year
```

#### 4.3 PortfolioHoldingReadService
```java
getHoldingsByUpload(Long)              // Holdings in specific upload
getHoldingsByCustomerLatest(Long)      // Latest holdings
getActiveHoldings(Long)                // Not exited
getExitedHoldings(Long)                // Sold/exited
getProfitableHoldings(Long)            // Profit > 0
getLossHoldings(Long)                  // Profit < 0
getHoldingsByAssetType(Long, Type)     // Filter by STOCK/CRYPTO/etc
```

#### 4.4 PortfolioSummaryReadService
```java
getPortfolioSummary(Long)              // Summary for upload
compareSnapshots(Long)                 // Compare all snapshots
```

#### 4.5 AnnualPerformanceReadService
```java
getAnnualPerformance(Long, Integer)    // By customer & year
getAllAnnualPerformance(Long)          // All years for customer
getAnnualPerformanceByYear(Integer)    // All customers for year
```

#### 4.6 PortfolioAnalyticsService (Manager-Level)
```java
getPortfolioManagerOverview()          // Complete manager view
getManagerOverviewByYear(Integer)      // Filtered by year
getTopPerformingCustomers(limit)       // Best performing clients
getBottomPerformingCustomers(limit)    // Worst performing clients
getTopAssets(limit)                    // Best performing assets
getRiskyAssets(limit)                  // Underperforming assets
```

---

### 5. Controllers (2 RESTful Controllers)

**Location:** `src/main/java/com/example/CapitalX/controller/`

#### 5.1 CustomerPortfolioController
**Base Path:** `/api/portfolio/customer/{customerId}`

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/profile` | GET | Customer details |
| `/portfolio/latest` | GET | Current portfolio value |
| `/portfolio/history` | GET | All historical snapshots |
| `/portfolio/snapshot/{uploadId}` | GET | Specific snapshot |
| `/portfolio/by-period?periodType=QUARTERLY` | GET | Filter by period |
| `/portfolio/by-year/{year}` | GET | Filter by year |
| `/holdings/latest` | GET | Current holdings |
| `/holdings/active` | GET | Active investments |
| `/holdings/exited` | GET | Sold/exited holdings |
| `/holdings/profitable` | GET | Profitable assets |
| `/holdings/losses` | GET | Loss-making assets |
| `/holdings/by-type/{assetType}` | GET | STOCK/CRYPTO/etc |
| `/annual-performance/{year}` | GET | Annual P&L statement |
| `/annual-performance/all` | GET | All annual reports |
| `/summary` | GET | Portfolio value & P&L |

**Example Requests:**

```bash
# Get latest portfolio
curl http://localhost:8080/api/portfolio/customer/1/portfolio/latest

# Get profitable holdings
curl http://localhost:8080/api/portfolio/customer/1/holdings/profitable

# Get annual performance for FY-2025
curl http://localhost:8080/api/portfolio/customer/1/annual-performance/2025

# Get stock holdings
curl http://localhost:8080/api/portfolio/customer/1/holdings/by-type/STOCK
```

#### 5.2 PortfolioManagerOverviewController
**Base Path:** `/api/portfolio/manager`

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/overview` | GET | Bird's-eye view of entire book |
| `/overview/by-year/{year}` | GET | Filtered by year |
| `/top-customers?limit=5` | GET | Top performing clients |
| `/bottom-customers?limit=5` | GET | Dragging returns clients |
| `/top-assets?limit=5` | GET | Best assets |
| `/risky-assets?limit=5` | GET | Risky/underperforming |
| `/total-portfolio-value` | GET | All customers combined |
| `/asset-exposure?limit=10` | GET | Asset distribution |
| `/annual-pnl` | GET | Annual P&L across all |
| `/concentration-analysis` | GET | Customer concentration |

**Example Requests:**

```bash
# Get manager overview
curl http://localhost:8080/api/portfolio/manager/overview

# Top 10 performing customers
curl http://localhost:8080/api/portfolio/manager/top-customers?limit=10

# Risky assets (underperforming)
curl http://localhost:8080/api/portfolio/manager/risky-assets?limit=5
```

---

## Response Examples

### CustomerPortfolioController Response (Latest Portfolio)
```json
{
  "uploadId": 5,
  "periodLabel": "Q4-2025",
  "periodType": "QUARTERLY",
  "uploadDate": "2026-01-31T21:31:45.270Z",
  "fileName": "portfolio_q4_2025.xlsx",
  "totalInvestedValue": 1000000.00,
  "totalCurrentValue": 1250000.00,
  "totalProfitLoss": 250000.00,
  "numberOfAssets": 25,
  "numberOfProfitableAssets": 20,
  "numberOfLossAssets": 5
}
```

### AssetPerformanceDTO Response (Holdings)
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
  }
]
```

### AnnualReportDTO Response (Annual Performance)
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

### PortfolioAnalyticsDTO Response (Manager Overview)
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

## Key Questions Answered by Controllers

### Customer Portfolio Controller
✅ "What is Client A's portfolio worth today?"  
✅ "How did Client A perform in FY-2025?"  
✅ "Which assets of Client A are losing money?"  
✅ "How many stocks vs crypto does Client A hold?"  

### Portfolio Manager Overview Controller
✅ "How is my entire book performing?"  
✅ "Which clients are dragging returns?"  
✅ "Which assets are risky across portfolios?"  
✅ "What's my total exposure to Bitcoin?"  
✅ "Which customers are my top performers?"  

---

## Error Handling Examples

### 404 - Customer Not Found
```json
{
  "timestamp": "2026-01-31T21:40:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Customer not found with id: 999"
}
```

### 404 - Portfolio Snapshot Not Found
```json
{
  "timestamp": "2026-01-31T21:40:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "No portfolio snapshot found for customer: 5"
}
```

### 400 - Invalid Period
```json
{
  "timestamp": "2026-01-31T21:40:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid period type: INVALID_PERIOD"
}
```

---

## Design Patterns & Best Practices Applied

### 1. Layered Architecture
- **Clear separation of concerns** (Controller → Service → Mapper → Repository)
- **Single Responsibility Principle** (each layer has one job)
- **Testability** (easy to mock at each layer)

### 2. Service Pattern
- **Interface-driven design** (each service has interface + impl)
- **Dependency injection** (constructor injection via Spring)
- **Stateless services** (can be scaled horizontally)

### 3. Mapper Pattern
- **No business logic** in mappers
- **Pure transformation** (entity → DTO)
- **Calculated fields** properly handled

### 4. DTO Pattern
- **API contracts** decoupled from entity structure
- **Flexibility** to change entities without breaking API
- **Security** (hide internal fields)

### 5. Exception Handling
- **Centralized** via GlobalExceptionHandler
- **Consistent** HTTP status codes
- **Clean** error messages (no stack traces to clients)

### 6. Read-Only Operations
- **No mutation** of data
- **No transaction boundaries** needed
- **Optimized for performance** (read-heavy workload)

---

## Configuration Required

### 1. Ensure MySQL is Running
```sql
USE capitalx_db;
-- Tables automatically created by Hibernate (ddl-auto=update)
```

### 2. Add Spring Boot DevTools (Optional but Recommended)
Already in pom.xml - enables hot reload during development.

### 3. Enable CORS (If Needed)
```java
// Add to CapitalXApplication.java if frontend is separate
@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**").allowedOrigins("*");
            }
        };
    }
}
```

---

## Testing the APIs

### Postman Collection Summary

**Customer Portfolio Tests:**
- GET `/api/portfolio/customer/1/profile`
- GET `/api/portfolio/customer/1/portfolio/latest`
- GET `/api/portfolio/customer/1/holdings/profitable`
- GET `/api/portfolio/customer/1/annual-performance/2025`

**Manager Analytics Tests:**
- GET `/api/portfolio/manager/overview`
- GET `/api/portfolio/manager/top-customers?limit=5`
- GET `/api/portfolio/manager/risky-assets?limit=5`

---

## What's NOT Implemented (Out of Scope)

❌ Write operations (Create, Update, Delete)  
❌ Excel file upload & parsing  
❌ Caching & performance optimization  
❌ Audit logging  
❌ Authentication & Authorization  
❌ API rate limiting  
❌ Swagger/OpenAPI documentation  

These can be added in future phases following the same architectural patterns.

---

## Alignment with FinDemyApp_Server

✅ Same package structure (beans/, repository/, service/, controller/, dto/, mapper/, exceptions/)  
✅ Same naming conventions (ServiceImpl, DTO suffixes)  
✅ Same Lombok usage (no boilerplate)  
✅ Same constructor injection pattern  
✅ Same exception handling approach  
✅ Same mapper strategy  
✅ Same controller REST patterns  

---

## Next Steps

1. **API Testing** - Verify endpoints with real data
2. **Performance Tuning** - Add database indexes & caching if needed
3. **Frontend Integration** - Connect Angular/React frontend to APIs
4. **Documentation** - Generate Swagger/OpenAPI docs
5. **Unit Tests** - Add JUnit tests for services & mappers
6. **Integration Tests** - End-to-end API tests
7. **Excel Upload** - Implement data import layer (future)

---

*Implementation Complete: February 1, 2026*  
*Analytics Layer: Read-Only Portfolio Management System*  
*Architecture: Fully Aligned with FinDemyApp_Server Reference Project*
