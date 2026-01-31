# CapitalX - Portfolio Management System

## Project Overview

**CapitalX** is a Spring Boot-based Portfolio Management system designed to help portfolio managers track and analyze client portfolios over time. The system is built using a clean, append-only database design that preserves historical data for performance analysis and annual P&L reporting.

### Key Characteristics
- **Portfolio-Manager Centric**: Not a trading system; focused on portfolio snapshots and performance tracking
- **Excel-Driven**: Data ingestion via Excel uploads representing complete portfolio snapshots
- **Historical Design**: Append-only architecture preserves all historical data
- **Performance Tracking**: Supports quarterly, annual, and custom period analysis
- **Dynamic Assets**: Assets are automatically added when first encountered in Excel uploads

---

## Project Structure

```
CapitalX/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/CapitalX/
│   │   │       ├── beans/                    # JPA Entity Classes
│   │   │       │   ├── Customer.java
│   │   │       │   ├── Asset.java
│   │   │       │   ├── PortfolioUpload.java
│   │   │       │   ├── PortfolioHolding.java
│   │   │       │   ├── PortfolioSummary.java
│   │   │       │   └── AnnualPerformance.java
│   │   │       ├── repository/               # Spring Data JPA Repositories
│   │   │       │   ├── CustomerRepository.java
│   │   │       │   ├── AssetRepository.java
│   │   │       │   ├── PortfolioUploadRepository.java
│   │   │       │   ├── PortfolioHoldingRepository.java
│   │   │       │   ├── PortfolioSummaryRepository.java
│   │   │       │   └── AnnualPerformanceRepository.java
│   │   │       ├── CapitalXApplication.java
│   │   │       └── PortfolioManagerApplication.java
│   │   └── resources/
│   │       └── application.properties         # Database & JPA Configuration
│   └── test/
│       └── java/
│           └── CapitalXApplicationTests.java
├── pom.xml                                   # Maven Configuration
└── README.md                                 # This file
```

---

## Database Schema

### 6 Core Entities

#### 1. **Customers** (`customers` table)
Identifies each client handled by the portfolio manager.

**Fields:**
- `customer_id` (PK, Auto-Increment)
- `customer_code` (Unique, Stable identifier from Excel/filename)
- `customer_name`
- `created_at` (Timestamp, auto-set on creation)

**Purpose:** Single source of truth for all clients. All portfolio data links back to this table.

---

#### 2. **Assets** (`assets` table)
Master list of all investment instruments (stocks, crypto, commodities, etc.).

**Fields:**
- `asset_id` (PK, Auto-Increment)
- `asset_code` (Unique, e.g., TCS, BTC, GOLD)
- `asset_name`
- `asset_type` (Enum: STOCK, CRYPTO, COMMODITY, ETF, OTHER)
- `exchange_or_market` (e.g., NSE, NYSE, BINANCE)
- `created_at` (Timestamp, auto-set on creation)

**Purpose:** Dynamic universe of all assets. Assets are added automatically when first seen in Excel uploads. Reused across all customers.

---

#### 3. **Portfolio_Uploads** (`portfolio_uploads` table)
Backbone of the historical design. Each Excel upload creates one row (never deleted or updated).

**Fields:**
- `upload_id` (PK, Auto-Increment)
- `customer_id` (FK → Customers)
- `period_type` (Enum: QUARTERLY, ANNUAL, CUSTOM)
- `period_label` (e.g., Q1-2026, FY-2025)
- `upload_date` (Timestamp, auto-set on creation)
- `file_name` (Original Excel filename)

**Purpose:** Historical backbone for tracking portfolio snapshots, growth trends, and annual P&L calculations.

---

#### 4. **Portfolio_Holdings** (`portfolio_holdings` table)
Core data layer storing exact portfolio data as received from Excel (never updated, only inserted).

**Fields:**
- `holding_id` (PK, Auto-Increment)
- `upload_id` (FK → Portfolio_Uploads)
- `customer_id` (FK → Customers)
- `asset_id` (FK → Assets)
- `quantity` (Decimal 18,4)
- `buy_price` (Decimal 18,6)
- `current_price` (Decimal 18,6)
- `invested_value` (Decimal 18,2)
- `current_value` (Decimal 18,2)
- `profit_loss` (Decimal 18,2)
- `investment_start_date` (Date)
- `investment_end_date` (Date, NULL if active)

**Purpose:** Immutable record of each investment line item from Excel. One row = one investment. Same customer/asset can appear in multiple uploads.

---

#### 5. **Portfolio_Summary** (`portfolio_summary` table)
User-level aggregated metrics per upload for quick portfolio comparisons and dashboards.

**Fields:**
- `summary_id` (PK, Auto-Increment)
- `upload_id` (FK → Portfolio_Uploads, Unique)
- `customer_id` (FK → Customers)
- `total_invested_value` (Decimal 18,2)
- `total_current_value` (Decimal 18,2)
- `total_profit_loss` (Decimal 18,2)
- `number_of_assets` (Integer)
- `number_of_profitable_assets` (Integer)
- `number_of_loss_assets` (Integer)

**Purpose:** Derived data stored at upload time for quick dashboard queries. One row per upload per customer.

---

#### 6. **Annual_Performance** (`annual_performance` table)
Annual P&L statement per customer per financial year.

**Fields:**
- `annual_perf_id` (PK, Auto-Increment)
- `customer_id` (FK → Customers)
- `financial_year` (Integer, e.g., 2025, 2026)
- `opening_value` (Decimal 18,2)
- `closing_value` (Decimal 18,2)
- `total_invested_during_year` (Decimal 18,2)
- `total_profit_loss` (Decimal 18,2)
- `best_performing_asset` (String)
- `worst_performing_asset` (String)

**Purpose:** One row per customer per financial year. Used for annual P&L statements requested by portfolio managers.

---

## JPA Entities

All entities are located in `src/main/java/com/example/CapitalX/beans/` and follow Spring Boot / Lombok conventions:

### Common Annotations Used
- `@Entity` - JPA entity mapping
- `@Table(name = "...")` - Database table name
- `@Id` - Primary key
- `@GeneratedValue(strategy = GenerationType.IDENTITY)` - Auto-increment
- `@Column(...)` - Column constraints (nullable, unique, precision, scale)
- `@ManyToOne` / `@JoinColumn` - Foreign key relationships
- `@PrePersist` - Auto-set timestamps on creation
- `@Enumerated(EnumType.STRING)` - Enum column mapping
- `@Data` (Lombok) - Auto-generate getters, setters, equals, hashCode, toString
- `@NoArgsConstructor` (Lombok) - No-arg constructor
- `@AllArgsConstructor` (Lombok) - All-args constructor

### Entity Relationships
```
Customer (1) ──┬──→ PortfolioUpload (*)
               ├──→ PortfolioHolding (*)
               ├──→ PortfolioSummary (*)
               └──→ AnnualPerformance (*)

PortfolioUpload (1) ──→ PortfolioHolding (*)
                  ──→ PortfolioSummary (1)

Asset (1) ──→ PortfolioHolding (*)
```

---

## Spring Data JPA Repositories

All repositories extend `JpaRepository<Entity, Long>` and are located in `src/main/java/com/example/CapitalX/repository/`:

### CustomerRepository
```java
findByCustomerCode(String customerCode) → Optional<Customer>
```

### AssetRepository
```java
findByAssetCode(String assetCode) → Optional<Asset>
```

### PortfolioUploadRepository
```java
findByCustomerId(Long customerId) → List<PortfolioUpload>
findByCustomerIdOrderByUploadDateDesc(Long customerId) → List<PortfolioUpload>
```

### PortfolioHoldingRepository
```java
findByUploadId(Long uploadId) → List<PortfolioHolding>
findByCustomerId(Long customerId) → List<PortfolioHolding>
findByUploadUploadIdAndCustomerCustomerId(Long uploadId, Long customerId) → List<PortfolioHolding>
```

### PortfolioSummaryRepository
```java
findByUploadId(Long uploadId) → Optional<PortfolioSummary>
findByUploadIdAndCustomerId(Long uploadId, Long customerId) → Optional<PortfolioSummary>
```

### AnnualPerformanceRepository
```java
findByCustomerIdAndFinancialYear(Long customerId, Integer financialYear) → Optional<AnnualPerformance>
findByCustomerId(Long customerId) → List<AnnualPerformance>
findByCustomerIdOrderByFinancialYearDesc(Long customerId) → List<AnnualPerformance>
```

---

## Database Configuration

**File:** `src/main/resources/application.properties`

```properties
spring.application.name=CapitalX

server.port=8080

# MySQL Database Connection
spring.datasource.url=jdbc:mysql://localhost:3306/capitalx_db
spring.datasource.username=root
spring.datasource.password=n3u3da!
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA / Hibernate Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
```

### Database Setup Instructions

1. **Create MySQL Database:**
   ```sql
   CREATE DATABASE capitalx_db;
   ```

2. **Ensure MySQL is running** on `localhost:3306`

3. **Update credentials** in `application.properties` if needed

4. **Hibernate auto-creates tables** on first run (with `ddl-auto=update`)

---

## Maven Dependencies

**Key Dependencies** (from `pom.xml`):
- Spring Boot 4.0.2
- Spring Data JPA
- MySQL Connector J
- Lombok (code generation)
- Spring Boot DevTools (hot reload)
- Spring Boot Validation

---

## Technology Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 4.0.2 |
| ORM | JPA / Hibernate |
| Database | MySQL 8.0+ |
| Build Tool | Maven |
| Code Generation | Lombok |

---

## Build & Run

### Build the Project
```bash
cd CapitalX
mvn clean compile
```

### Run the Application
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080` and automatically create all database tables.

---

## What's Implemented ✅

- **6 JPA Entities** with proper annotations and relationships
- **6 Spring Data Repositories** with derived query methods
- **MySQL Database Configuration** with connection pooling
- **Append-Only Design** ensuring historical data preservation
- **Auto-timestamping** for creation timestamps
- **Enum Support** for AssetType and PeriodType
- **Decimal Precision** for financial calculations (18,2 and 18,6 scales)

---

## What's Pending ⏳

The following layers are **NOT YET implemented** and will be added in future phases:

1. **Services** - Business logic layer (e.g., `CustomerService`, `PortfolioService`, `ExcelImportService`)
2. **Controllers** - REST API endpoints (POST/GET/PUT/DELETE)
3. **DTOs** - Data Transfer Objects for request/response mapping
4. **Mappers** - Entity ↔ DTO conversion utilities
5. **Exception Handlers** - Global exception handling and custom exceptions
6. **Excel Parser** - Excel file upload and parsing logic
7. **Validators** - Input validation and business rule enforcement
8. **Tests** - Unit and integration tests

---

## Design Principles

1. **Append-Only**: Historical data is never overwritten or deleted
2. **Immutability**: Portfolio holdings are insert-only, never updated
3. **Denormalization**: Portfolio summary is pre-calculated and stored for performance
4. **Consistency**: Lombok ensures consistent getters/setters; JPA ensures data consistency
5. **Traceability**: All records are timestamped for audit trail
6. **Flexibility**: Dynamic asset addition supports any instrument type

---

## Next Steps

1. Implement **Service layer** for business logic
2. Create **Controllers** with REST endpoints
3. Build **DTO classes** for API contracts
4. Add **Exception handling** and global error responses
5. Implement **Excel parsing** for portfolio uploads
6. Add **unit tests** and **integration tests**
7. Create **API documentation** (Swagger/OpenAPI)

---

## Architecture Consistency

This project follows the **same architectural pattern** as **FinDemyApp_Server**:

✓ Same package structure (`beans/`, `repository/`)  
✓ Same Lombok usage for entity generation  
✓ Same JpaRepository pattern for data access  
✓ Same Spring Boot configuration approach  
✓ Same MySQL + Hibernate setup  

**Note:** The database schema is optimized for **portfolio management** (not training/courses), but the architectural style and coding patterns remain consistent with the reference project.

---

## Author Notes

- This implementation focuses **strictly on the database layer** (entities + repositories)
- All design decisions follow the requirements document (6 tables, append-only, historical tracking)
- The system is ready for **service and controller layer development**
- No external APIs or third-party integrations are included at this stage

---

*Last Updated: January 31, 2026*  
*Project: CapitalX Portfolio Manager*  
*Status: Database Layer Complete ✓*
