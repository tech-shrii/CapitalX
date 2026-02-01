# CapitalX - Portfolio Management System

## 📋 Table of Contents
- [Project Overview](#project-overview)
- [Getting Started](#getting-started)
- [Project Structure](#project-structure)
- [Database Schema](#database-schema)
- [Architecture & Layers](#architecture--layers)
- [API Endpoints](#api-endpoints)
- [Technology Stack](#technology-stack)
- [Setup Instructions](#setup-instructions)
- [Building & Running](#building--running)
- [Database Initialization](#database-initialization)
- [Testing the Application](#testing-the-application)

---

## Project Overview

**CapitalX** is a comprehensive Spring Boot-based **Portfolio Management System** designed to help portfolio managers track, analyze, and report on client investment portfolios over time. The system uses a **clean, append-only database design** that preserves historical data for detailed performance analysis and annual P&L reporting.

### Key Characteristics
- **Portfolio-Manager Centric**: Not a trading system; focused on portfolio snapshots and performance tracking
- **Excel-Driven**: Data ingestion via Excel uploads representing complete portfolio snapshots
- **Historical Design**: Append-only architecture preserves all historical data for trend analysis
- **Performance Tracking**: Supports quarterly, annual, and custom period analysis
- **Dynamic Assets**: Assets are automatically added when first encountered in Excel uploads
- **REST API**: Full REST endpoints for portfolio queries and analytics
- **Exception Handling**: Global error handling with custom exceptions
- **Type-Safe DTOs**: Data Transfer Objects for clean API contracts

---

## Getting Started

### Prerequisites
Before you begin, ensure you have the following installed:
- **Java 21** or higher
- **Maven 3.8+**
- **MySQL 8.0+** running on `localhost:3306`
- **Git** (optional)

### Initial Database Setup

**IMPORTANT:** Before running the application for the first time, you must initialize your database with starter data.

**Steps:**
1. Create the database:
   ```sql
   CREATE DATABASE capitalx_db;
   ```

2. Open `mysqlquery.md` file in your project root directory

3. Copy all SQL queries from `mysqlquery.md` and execute them in your MySQL client (MySQL Workbench, DBeaver, or mysql CLI)

4. This will create all required tables and populate them with sample data:
   - 3 sample customers
   - Various assets (stocks, crypto, commodities)
   - Portfolio uploads for different periods
   - Holdings and performance data

5. Verify the data was loaded:
   ```sql
   USE capitalx_db;
   SELECT COUNT(*) FROM customers;
   SELECT COUNT(*) FROM assets;
   SELECT COUNT(*) FROM portfolio_holdings;
   ```

This starter data allows you to immediately test all API endpoints without manual data entry.

---

## Project Structure

```
CapitalX/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── PortfolioManagerApplication.java      # Main entry point
│   │   │   ├── CapitalXApplication.java              # Spring Boot app config
│   │   │   └── com/example/CapitalX/
│   │   │       ├── beans/                             # JPA Entity Classes
│   │   │       │   ├── Customer.java                  # Customer entity
│   │   │       │   ├── Asset.java                     # Asset/Instrument entity
│   │   │       │   ├── PortfolioUpload.java           # Upload metadata entity
│   │   │       │   ├── PortfolioHolding.java          # Individual holdings entity
│   │   │       │   ├── PortfolioSummary.java          # Aggregated summary entity
│   │   │       │   └── AnnualPerformance.java         # Annual P&L entity
│   │   │       │
│   │   │       ├── controller/                        # REST Controllers
│   │   │       │   ├── CustomerPortfolioController.java
│   │   │       │   └── PortfolioManagerOverviewController.java
│   │   │       │
│   │   │       ├── service/                           # Business Logic Layer
│   │   │       │   ├── CustomerService.java
│   │   │       │   ├── PortfolioService.java
│   │   │       │   ├── AnalyticsService.java
│   │   │       │   └── ReportService.java
│   │   │       │
│   │   │       ├── repository/                        # Spring Data JPA Repositories
│   │   │       │   ├── CustomerRepository.java
│   │   │       │   ├── AssetRepository.java
│   │   │       │   ├── PortfolioUploadRepository.java
│   │   │       │   ├── PortfolioHoldingRepository.java
│   │   │       │   ├── PortfolioSummaryRepository.java
│   │   │       │   └── AnnualPerformanceRepository.java
│   │   │       │
│   │   │       ├── dto/                               # Data Transfer Objects
│   │   │       │   ├── CustomerOverviewDTO.java
│   │   │       │   ├── PortfolioSnapshotDTO.java
│   │   │       │   ├── AssetPerformanceDTO.java
│   │   │       │   ├── AnnualReportDTO.java
│   │   │       │   ├── PortfolioAnalyticsDTO.java
│   │   │       │   ├── CustomerPerformanceDTO.java
│   │   │       │   ├── AssetExposureDTO.java
│   │   │       │   └── PortfolioSnapshotDTO.java
│   │   │       │
│   │   │       ├── mapper/                            # Entity ↔ DTO Mappers
│   │   │       │   ├── CustomerMapper.java
│   │   │       │   ├── PortfolioMapper.java
│   │   │       │   ├── AnnualPerformanceMapper.java
│   │   │       │   └── AssetPerformanceMapper.java
│   │   │       │
│   │   │       └── exceptions/                        # Custom Exceptions
│   │   │           ├── CustomerNotFoundException.java
│   │   │           ├── PortfolioSnapshotNotFoundException.java
│   │   │           ├── AnnualPerformanceNotAvailableException.java
│   │   │           ├── InvalidPeriodException.java
│   │   │           ├── DataConsistencyException.java
│   │   │           └── GlobalExceptionHandler.java
│   │   │
│   │   └── resources/
│   │       ├── application.properties                 # Configuration file
│   │       ├── static/                                # Static files
│   │       └── templates/                             # Templates
│   │
│   └── test/
│       └── java/
│           └── com/example/CapitalX/
│               └── CapitalXApplicationTests.java
│
├── pom.xml                                            # Maven POM file
├── README.md                                          # This file
├── mysqlquery.md                                      # SQL initialization scripts
├── IMPLEMENTATION_SUMMARY.md                          # Implementation details
├── API_REFERENCE.md                                   # API endpoint documentation
└── ANALYTICS_LAYER_GUIDE.md                          # Analytics guide

```

---

## Database Schema

### Overview
The CapitalX system uses **6 core entities** with a **normalized, append-only design** that preserves historical data:

```
┌─────────────┐
│  Customers  │◄────────────────┐
└─────┬───────┘                 │
      │                         │
      ├──────►┌──────────────────────────┐
      │       │  Portfolio_Uploads       │
      │       └──────┬───────────────────┘
      │              │
      ├──────────┬───┴──────┬─────┐
      │          │          │     │
      │    ┌─────▼──────┐   │     │
      │    │ Portfolio  │   │     │
      │    │ Holdings   │   │     │
      │    └────────────┘   │     │
      │                     │     │
      ├──────────────►┌──────▼──────┐
      │               │Portfolio    │
      │               │Summary      │
      │               └─────────────┘
      │
      └──────────►┌──────────────────────┐
                  │ Annual_Performance   │
                  └──────────────────────┘

┌──────────┐
│  Assets  │◄──────┐
└──────────┘       │
                   │
            ┌──────┴──────┐
            │Portfolio    │
            │Holdings     │
            └─────────────┘
```

### 1. Customers Table (`customers`)

Identifies each client handled by the portfolio manager.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `customer_id` | INT | PK, AUTO_INCREMENT | Unique identifier |
| `customer_code` | VARCHAR(50) | UNIQUE, NOT NULL | Stable identifier from Excel |
| `customer_name` | VARCHAR(255) | NOT NULL | Full customer name |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Record creation time |

**Purpose:** Single source of truth for all clients. All portfolio data links back via foreign key.

---

### 2. Assets Table (`assets`)

Master list of all investment instruments (stocks, crypto, commodities, etc.).

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `asset_id` | INT | PK, AUTO_INCREMENT | Unique identifier |
| `asset_code` | VARCHAR(50) | UNIQUE, NOT NULL | Code (e.g., TCS, BTC, GOLD) |
| `asset_name` | VARCHAR(255) | NOT NULL | Full asset name |
| `asset_type` | ENUM | NOT NULL | STOCK, CRYPTO, COMMODITY, ETF, OTHER |
| `exchange_or_market` | VARCHAR(100) | - | NSE, NYSE, BINANCE, etc. |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Record creation time |

**Purpose:** Dynamic universe of all assets. Assets are auto-added when first seen in uploads.

---

### 3. Portfolio_Uploads Table (`portfolio_uploads`)

Backbone of the historical design. Each Excel upload creates one row (never deleted).

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `upload_id` | INT | PK, AUTO_INCREMENT | Unique identifier |
| `customer_id` | INT | FK → customers | References customer |
| `period_type` | ENUM | NOT NULL | QUARTERLY, ANNUAL, CUSTOM |
| `period_label` | VARCHAR(100) | NOT NULL | e.g., Q1-2026, FY-2025 |
| `upload_date` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Upload timestamp |
| `file_name` | VARCHAR(255) | - | Original Excel filename |

**Purpose:** Historical backbone for tracking portfolio snapshots, growth trends, and P&L calculations.

---

### 4. Portfolio_Holdings Table (`portfolio_holdings`)

Core data layer storing exact portfolio data (append-only, never updated).

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `holding_id` | INT | PK, AUTO_INCREMENT | Unique identifier |
| `upload_id` | INT | FK → portfolio_uploads | References upload |
| `customer_id` | INT | FK → customers | References customer |
| `asset_id` | INT | FK → assets | References asset |
| `quantity` | DECIMAL(18,4) | - | Number of units |
| `buy_price` | DECIMAL(18,6) | - | Original purchase price |
| `current_price` | DECIMAL(18,6) | - | Current market price |
| `invested_value` | DECIMAL(18,2) | - | Quantity × Buy Price |
| `current_value` | DECIMAL(18,2) | - | Quantity × Current Price |
| `profit_loss` | DECIMAL(18,2) | - | Current Value - Invested Value |
| `investment_start_date` | DATE | - | When investment started |
| `investment_end_date` | DATE | NULL | When investment ended (if active) |

**Purpose:** Immutable record of each investment line item. One row = one investment.

---

### 5. Portfolio_Summary Table (`portfolio_summary`)

User-level aggregated metrics per upload for quick comparisons.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `summary_id` | INT | PK, AUTO_INCREMENT | Unique identifier |
| `upload_id` | INT | FK → portfolio_uploads, UNIQUE | References upload |
| `customer_id` | INT | FK → customers | References customer |
| `total_invested_value` | DECIMAL(18,2) | - | Sum of all investments |
| `total_current_value` | DECIMAL(18,2) | - | Sum of all current values |
| `total_profit_loss` | DECIMAL(18,2) | - | Sum of all P&L |
| `number_of_assets` | INT | - | Count of unique assets |
| `number_of_profitable_assets` | INT | - | Assets with profit |
| `number_of_loss_assets` | INT | - | Assets with loss |

**Purpose:** Pre-calculated aggregates for fast dashboard queries.

---

### 6. Annual_Performance Table (`annual_performance`)

Annual P&L statement per customer per financial year.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `annual_perf_id` | INT | PK, AUTO_INCREMENT | Unique identifier |
| `customer_id` | INT | FK → customers | References customer |
| `financial_year` | INT | - | e.g., 2025, 2026 |
| `opening_value` | DECIMAL(18,2) | - | Portfolio value at year start |
| `closing_value` | DECIMAL(18,2) | - | Portfolio value at year end |
| `total_invested_during_year` | DECIMAL(18,2) | - | New investments |
| `total_profit_loss` | DECIMAL(18,2) | - | Year's P&L |
| `best_performing_asset` | VARCHAR(255) | - | Top asset |
| `worst_performing_asset` | VARCHAR(255) | - | Bottom asset |

**Purpose:** One row per customer per financial year for annual reports.

---

## Architecture & Layers

CapitalX follows a **layered architecture** with clear separation of concerns:

### 1. **Bean Layer** (`beans/`)
JPA Entity classes representing database tables. Each entity:
- Maps to a database table via `@Entity` and `@Table` annotations
- Has proper relationships via `@ManyToOne`, `@JoinColumn`
- Uses Lombok for code generation
- Includes timestamps via `@PrePersist`

### 2. **Repository Layer** (`repository/`)
Spring Data JPA repositories providing database access:
- Extends `JpaRepository<Entity, Long>`
- Provides derived query methods
- Handles CRUD operations automatically
- Custom queries for complex business logic

### 3. **Service Layer** (`service/`)
Business logic layer handling:
- **CustomerService**: Customer management
- **PortfolioService**: Portfolio operations
- **AnalyticsService**: Performance calculations
- **ReportService**: Report generation

### 4. **Controller Layer** (`controller/`)
REST API endpoints exposing business logic:
- **CustomerPortfolioController**: Customer portfolio endpoints
- **PortfolioManagerOverviewController**: Manager-level analytics

### 5. **DTO Layer** (`dto/`)
Data Transfer Objects for API contracts:
- Request/Response mapping
- Type-safe data handling
- Prevents direct entity exposure

### 6. **Mapper Layer** (`mapper/`)
Entity ↔ DTO conversion utilities:
- **CustomerMapper**: Customer entity ↔ DTO
- **PortfolioMapper**: Portfolio entity ↔ DTO
- **AnnualPerformanceMapper**: Annual report mapping
- **AssetPerformanceMapper**: Asset performance mapping

### 7. **Exception Layer** (`exceptions/`)
Custom exceptions and global error handling:
- `CustomerNotFoundException`
- `PortfolioSnapshotNotFoundException`
- `AnnualPerformanceNotAvailableException`
- `InvalidPeriodException`
- `DataConsistencyException`
- `GlobalExceptionHandler` for centralized error handling

---

## API Endpoints

### Customer Portfolio Endpoints

#### 1. Get Customer Portfolio Overview
```
GET /api/customers/{customerId}/portfolio/overview
```
**Response:** Customer portfolio snapshot with all holdings and summary.

---

#### 2. Get Customer by ID
```
GET /api/customers/{customerId}
```
**Response:** Customer details with all portfolio metadata.

---

#### 3. Get Portfolio for Specific Period
```
GET /api/customers/{customerId}/portfolio/period/{uploadId}
```
**Response:** Portfolio snapshot for a specific upload period.

---

#### 4. Get Annual Performance Report
```
GET /api/customers/{customerId}/performance/annual/{financialYear}
```
**Response:** Annual P&L report with best/worst performing assets.

---

#### 5. Get Customer Performance Trends
```
GET /api/customers/{customerId}/performance/trends
```
**Response:** Performance data across all periods.

---

### Portfolio Manager Overview Endpoints

#### 1. Get All Customers Overview
```
GET /api/portfolio-manager/overview
```
**Response:** High-level view of all managed portfolios.

---

#### 2. Get Total Assets Under Management
```
GET /api/portfolio-manager/aum
```
**Response:** AUM summary and distribution.

---

#### 3. Get Performance Analytics
```
GET /api/portfolio-manager/analytics
```
**Response:** Portfolio-wide analytics and insights.

---

#### 4. Get Best Performing Customers
```
GET /api/portfolio-manager/top-performers
```
**Response:** Ranked customer performance.

---

See **API_REFERENCE.md** for complete endpoint documentation with request/response examples.

---

## Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| **Language** | Java | 21 LTS |
| **Framework** | Spring Boot | 4.0.2 |
| **ORM** | JPA / Hibernate | Latest |
| **Database** | MySQL | 8.0+ |
| **Build Tool** | Maven | 3.8+ |
| **Code Generation** | Lombok | 1.18+ |
| **Validation** | Spring Validation | 4.0+ |
| **Testing** | JUnit 5 | Latest |

---

## Setup Instructions

### Step 1: Clone or Download Project
```bash
cd c:\Users\Administrator\Desktop\CAPITALX\springboot\CapitalX\CapitalX
```

### Step 2: Verify Prerequisites
```bash
# Check Java version (must be 21+)
java -version

# Check Maven version (must be 3.8+)
mvn -version

# Check MySQL is running
mysql -u root -p -e "SELECT VERSION();"
```

### Step 3: Update Database Credentials
Edit `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/capitalx_db
spring.datasource.username=root
spring.datasource.password=n3u3da!
```

**Replace with your MySQL credentials if different.**

### Step 4: Create Database
```sql
CREATE DATABASE capitalx_db;
```

### Step 5: Initialize Database with Starter Data
Open `mysqlquery.md` and execute all SQL queries:
```bash
# Using MySQL CLI
mysql -u root -p capitalx_db < mysqlquery.sql

# Or copy-paste queries into MySQL Workbench
```

---

## Building & Running

### Build the Project
```bash
mvn clean compile
```

This will:
- Download all dependencies
- Compile Java source code
- Generate Lombok code

### Run the Application
```bash
mvn spring-boot:run
```

Or:
```bash
mvn clean package
java -jar target/CapitalX-0.0.1-SNAPSHOT.jar
```

**Expected Output:**
```
Tomcat started on port(s): 8080 (http) with context path ''
CapitalX has started successfully!
```

---

## Database Initialization

### What is mysqlquery.md?

`mysqlquery.md` is a comprehensive SQL script file that:
1. **Creates all database tables** with proper structure
2. **Populates sample data** for testing
3. **Inserts relationships** between customers, assets, and portfolios

### Contents of mysqlquery.md

The file includes:
- **DDL Statements**: CREATE TABLE statements for all 6 entities
- **Sample Customers**: 3 test customers (ABC Corp, XYZ Ltd, Tech Ventures)
- **Sample Assets**: Stocks, crypto, commodities across exchanges
- **Sample Uploads**: Multiple portfolio snapshots per customer
- **Sample Holdings**: Complete holdings for each upload period
- **Portfolio Summaries**: Pre-calculated aggregates
- **Annual Performance**: Multi-year P&L data

### How to Load mysqlquery.md

**Option 1: MySQL Workbench**
1. Open MySQL Workbench
2. File → Open SQL Script → Select `mysqlquery.md`
3. Execute all queries (Cmd+Return or Ctrl+Enter)

**Option 2: MySQL CLI**
```bash
mysql -u root -p capitalx_db < mysqlquery.md
```

**Option 3: Direct Copy-Paste**
1. Open `mysqlquery.md` in any text editor
2. Copy all SQL statements
3. Paste into MySQL client and execute

### Verify Data Loaded Successfully

```sql
USE capitalx_db;

-- Check customers
SELECT COUNT(*) as customer_count FROM customers;

-- Check assets
SELECT COUNT(*) as asset_count FROM assets;

-- Check holdings
SELECT COUNT(*) as holding_count FROM portfolio_holdings;

-- Check summary
SELECT COUNT(*) as summary_count FROM portfolio_summary;

-- Check annual performance
SELECT COUNT(*) as annual_count FROM annual_performance;

-- Sample query
SELECT c.customer_name, SUM(ps.total_current_value) as portfolio_value
FROM customers c
JOIN portfolio_summary ps ON c.customer_id = ps.customer_id
GROUP BY c.customer_id, c.customer_name;
```

---

## Testing the Application

### Start the Application
```bash
mvn spring-boot:run
```

### Test Base URL
```
http://localhost:8080
```

### Test Endpoints with Sample Data

#### 1. Get Customer 1 Overview
```bash
curl http://localhost:8080/api/customers/1/portfolio/overview
```

#### 2. Get All Customers
```bash
curl http://localhost:8080/api/customers
```

#### 3. Get Portfolio Manager Overview
```bash
curl http://localhost:8080/api/portfolio-manager/overview
```

#### 4. Get Annual Performance for Customer 1, Year 2025
```bash
curl http://localhost:8080/api/customers/1/performance/annual/2025
```

#### 5. Get Performance Analytics
```bash
curl http://localhost:8080/api/portfolio-manager/analytics
```

### Using Postman/Insomnia

1. Import API collection from project
2. Set base URL to `http://localhost:8080`
3. Execute requests with sample customer IDs (1, 2, 3)
4. Verify responses match expected portfolio data

### Example Response

```json
{
  "customerId": 1,
  "customerName": "ABC Corporation",
  "totalInvestedValue": 500000.00,
  "totalCurrentValue": 625000.00,
  "totalProfitLoss": 125000.00,
  "portfolioReturn": 25.00,
  "numberOfAssets": 5,
  "profitableAssets": 4,
  "lossAssets": 1,
  "lastUpdateDate": "2026-02-01"
}
```

---

## Troubleshooting

### Issue: "Connection refused" (MySQL)
**Solution:**
- Ensure MySQL is running: `mysql -u root -p`
- Check port 3306 is open
- Verify credentials in `application.properties`

### Issue: "Table doesn't exist"
**Solution:**
- Load `mysqlquery.md` to create tables
- Check database is created: `SHOW DATABASES;`

### Issue: "No tables loaded"
**Solution:**
1. Delete old database: `DROP DATABASE capitalx_db;`
2. Create new: `CREATE DATABASE capitalx_db;`
3. Load fresh `mysqlquery.md`

### Issue: Port 8080 already in use
**Solution:**
```bash
# Change port in application.properties
server.port=8081

# Or kill process using 8080
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

---

## Project Completion Status

### ✅ Completed Components
- **Database Schema**: 6 normalized entities with relationships
- **JPA Entities**: All beans with Lombok annotations
- **Repositories**: Spring Data JPA repositories with query methods
- **Controllers**: REST endpoints for portfolio queries
- **Services**: Business logic for portfolio management
- **DTOs**: Type-safe data transfer objects
- **Mappers**: Entity ↔ DTO conversion
- **Exception Handling**: Global error handling with custom exceptions
- **Database Initialization**: SQL starter data in `mysqlquery.md`

### ⏳ Future Enhancements
- Excel file upload parser
- Advanced analytics and reporting
- Multi-user authentication
- Dashboard UI
- Email notifications
- Performance optimization indices

---

## File Reference

| File | Purpose |
|------|---------|
| `README.md` | Project documentation (this file) |
| `mysqlquery.md` | SQL initialization scripts |
| `API_REFERENCE.md` | Complete API documentation |
| `IMPLEMENTATION_SUMMARY.md` | Technical implementation details |
| `ANALYTICS_LAYER_GUIDE.md` | Analytics and reporting guide |
| `pom.xml` | Maven configuration and dependencies |

---

## Quick Start Summary

1. **Ensure MySQL running** on `localhost:3306`
2. **Create database**: `CREATE DATABASE capitalx_db;`
3. **Load starter data**: Execute all queries from `mysqlquery.md`
4. **Build project**: `mvn clean compile`
5. **Run application**: `mvn spring-boot:run`
6. **Test API**: `curl http://localhost:8080/api/customers/1/portfolio/overview`

---

## Support & Documentation

- **API Details**: See `API_REFERENCE.md`
- **Implementation**: See `IMPLEMENTATION_SUMMARY.md`
- **Analytics**: See `ANALYTICS_LAYER_GUIDE.md`
- **Issues**: Check troubleshooting section above

---

## Author Notes

- CapitalX follows a **layered architecture** with strict separation of concerns
- Database design is **append-only** to preserve historical data
- All financial values use **DECIMAL** precision for accuracy
- System is ready for **production deployment** with proper testing
- Extensible design supports future features (Excel upload, webhooks, etc.)

---

*Last Updated: February 1, 2026*  
*Project: CapitalX Portfolio Manager*  
*Version: 1.0.0*  
*Status: Fully Operational ✓*
