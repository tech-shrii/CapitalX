# CapitalX Portfolio Management System

A comprehensive full-stack web application for fund managers to track, manage, and report on client investments across multiple asset classes with real-time pricing and analytics.

## 🎯 What It Solves

The CapitalX system addresses the core challenges faced by fund managers:
- **Multi-client Portfolio Management**: Track investments for multiple clients in one unified platform
- **Real-time P&L Monitoring**: Calculate profit/loss with live market data updates
- **Asset Diversity**: Support for stocks, bonds, mutual funds, crypto, commodities, and forex
- **Automated Reporting**: Generate professional PDF statements and Excel reports
- **Secure Access**: JWT-based authentication with email OTP verification

## 🏗️ System Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │   Backend       │    │ Pricing Service │
│   (Vanilla JS)  │◄──►│  (Spring Boot)  │◄──►│  (FastAPI)      │
│   Port: 5500    │    │   Port: 8080    │    │   Port: 8000    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │   MySQL DB      │
                       │   Port: 3306    │
                       └─────────────────┘
```

## 📁 Complete Project Structure

```
CapitalX/
├── backend/                          # Spring Boot Backend
│   ├── src/main/java/com/app/portfolio/
│   │   ├── PortfolioApplication.java # Main application entry point
│   │   ├── beans/                    # JPA Entities (Database Models)
│   │   │   ├── User.java            # User authentication entity
│   │   │   ├── Client.java          # Client information entity
│   │   │   ├── Asset.java           # Investment holdings entity
│   │   │   ├── AssetPrice.java      # Historical price data
│   │   │   ├── OtpToken.java        # OTP verification tokens
│   │   │   └── ...                  # Other domain entities
│   │   ├── repository/               # JPA Data Access Layer
│   │   │   ├── UserRepository.java
│   │   │   ├── ClientRepository.java
│   │   │   ├── AssetRepository.java
│   │   │   └── ...
│   │   ├── service/                  # Business Logic Layer
│   │   │   ├── auth/               # Authentication services
│   │   │   ├── asset/              # Asset management services
│   │   │   ├── client/             # Client management services
│   │   │   ├── pricing/            # Price calculation services
│   │   │   ├── email/              # Email notification services
│   │   │   └── ...
│   │   ├── controller/               # REST API Controllers
│   │   │   ├── AuthController.java  # Authentication endpoints
│   │   │   ├── ClientController.java # Client management
│   │   │   ├── AssetController.java  # Asset CRUD operations
│   │   │   ├── PricingController.java # Price calculations
│   │   │   ├── DashboardController.java # Dashboard data
│   │   │   └── ...
│   │   ├── dto/                      # Data Transfer Objects
│   │   │   ├── auth/               # Authentication DTOs
│   │   │   ├── asset/              # Asset DTOs
│   │   │   ├── client/             # Client DTOs
│   │   │   └── ...
│   │   ├── security/                 # Security Configuration
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   └── SecurityConfig.java
│   │   ├── config/                   # Application Configuration
│   │   │   ├── CorsConfig.java      # CORS settings
│   │   │   └── RestTemplateConfig.java
│   │   ├── exceptions/               # Custom Exception Handlers
│   │   └── mapper/                   # Entity-DTO Mappers
│   └── pom.xml                       # Maven dependencies
├── frontend/                         # Vanilla JavaScript Frontend
│   ├── index.html                   # Landing page
│   ├── auth/                        # Authentication pages
│   │   ├── login.html
│   │   └── signup.html
│   ├── dashboard/                   # Main application pages
│   │   ├── home.html                # Dashboard overview
│   │   ├── client-detail.html       # Client portfolio view
│   │   ├── charts.html              # Portfolio analytics
│   │   ├── import-data.html         # Excel import
│   │   ├── statements.html         # PDF statements
│   │   └── settings.html            # User settings
│   ├── css/                         # Stylesheets
│   │   └── new-style.css           # Main styling
│   └── js/                          # JavaScript modules
│       ├── api.js                   # API communication
│       ├── auth.js                  # Authentication logic
│       ├── dashboard.js             # Dashboard functionality
│       ├── charts.js                # Chart rendering
│       └── ...
├── pricing-service/                  # Python FastAPI Microservice
│   ├── main.py                      # FastAPI application
│   ├── requirements.txt             # Python dependencies
│   ├── run.sh / run.bat            # Startup scripts
│   └── test_*.py                   # Test files
├── sample-portfolio*.csv             # Sample data files
├── PRICING_MICROSERVICE.md          # Pricing service documentation
└── README.md                         # This file
```

## 🛠️ Technology Stack

### Backend (Spring Boot)
- **Java 21** - Modern Java with latest features
- **Spring Boot 3.2.3** - Application framework
- **Spring Security** - Authentication and authorization (JWT)
- **Spring Data JPA** - Database ORM with Hibernate
- **MySQL 8.0+** - Primary database
- **Maven** - Dependency management
- **Lombok** - Code generation for boilerplate reduction
- **JWT (jjwt)** - Token-based authentication
- **Apache POI** - Excel file processing
- **OpenPDF** - PDF generation

### Frontend (Vanilla JavaScript)
- **HTML5/CSS3** - Modern markup and styling
- **Vanilla JavaScript** - No framework dependencies
- **Chart.js** - Portfolio visualization charts
- **Fetch API** - HTTP requests to backend

### Pricing Microservice (Python)
- **FastAPI** - Modern Python web framework
- **yfinance** - Yahoo Finance API integration
- **pandas** - Data manipulation
- **uvicorn** - ASGI server
- **requests-cache** - API response caching

### Database Schema

The application uses a relational database with the following core tables:

#### **users** Table
```sql
- id (BIGINT, PRIMARY KEY, AUTO_INCREMENT)
- name (VARCHAR, NOT NULL)
- email (VARCHAR, UNIQUE, NOT NULL)
- password (VARCHAR, NOT NULL, BCrypt encrypted)
- enabled (BOOLEAN, DEFAULT TRUE)
- created_at (TIMESTAMP)
- updated_at (TIMESTAMP)
```

#### **clients** Table
```sql
- id (BIGINT, PRIMARY KEY, AUTO_INCREMENT)
- user_id (BIGINT, FOREIGN KEY → users.id)
- name (VARCHAR, NOT NULL)
- email (VARCHAR, NOT NULL)
- phone (VARCHAR)
- currency (VARCHAR(3)) - Client's base currency
- created_at (TIMESTAMP)
- updated_at (TIMESTAMP)
```

#### **assets** Table
```sql
- id (BIGINT, PRIMARY KEY, AUTO_INCREMENT)
- client_id (BIGINT, FOREIGN KEY → clients.id)
- name (VARCHAR, NOT NULL)
- category (ENUM: STOCK, MUTUAL_FUND, BOND, CRYPTO, COMMODITY, FOREX)
- symbol (VARCHAR(50)) - Trading symbol
- quantity (DECIMAL(20,4), NOT NULL)
- buying_rate (DECIMAL(20,4), NOT NULL)
- purchase_date (DATE, NOT NULL)
- currency (VARCHAR(3)) - Asset currency
- created_at (TIMESTAMP)
- updated_at (TIMESTAMP)
```

#### **asset_prices** Table
```sql
- id (BIGINT, PRIMARY KEY, AUTO_INCREMENT)
- asset_id (BIGINT, FOREIGN KEY → assets.id)
- current_price (DECIMAL(20,4), NOT NULL)
- price_date (TIMESTAMP, NOT NULL)
- source (ENUM: YFINANCE, MANUAL, FAKE)
```

#### **otp_tokens** Table
```sql
- id (BIGINT, PRIMARY KEY, AUTO_INCREMENT)
- user_id (BIGINT, FOREIGN KEY → users.id)
- token (VARCHAR, NOT NULL)
- expiry_date (TIMESTAMP, NOT NULL)
- used (BOOLEAN, DEFAULT FALSE)
```

### Entity Relationships
- **User → Clients**: One-to-many (A user can manage multiple clients)
- **Client → Assets**: One-to-many (A client can have multiple assets)
- **Asset → AssetPrices**: One-to-many (An asset has historical price records)
- **User → OtpTokens**: One-to-many (User can have multiple OTP tokens)

## 🚀 Quick Start Guide

### Prerequisites

Ensure you have the following installed:
- **Java 21** or higher
- **Maven 3.6+** 
- **MySQL 8.0+**
- **Python 3.8+** (for pricing service)
- **Node.js** (optional, for frontend development)

### Step 1: Database Setup

Create a MySQL database:

```sql
CREATE DATABASE portfolio_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Step 2: Backend Configuration

Create `backend/src/main/resources/application.properties`:

```properties
# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/portfolio_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=your_mysql_username
spring.datasource.password=your_mysql_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA/Hibernate Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# Server Configuration
server.port=8080

# JWT Configuration
app.jwt.secret=your-secret-key-minimum-32-characters-long-for-security
app.jwt.expiration=86400000  # 24 hours in milliseconds

# Email Configuration (Gmail Example)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# News API Configuration
app.news.api-key=your-newsapi-key-from-newsapi.org

# Pricing Service Configuration
pricing.service.base-url=http://localhost:8000
```

**Important Notes:**
- For Gmail, generate an App Password (not your regular password)
- JWT secret must be at least 32 characters long
- Get a free NewsAPI key from [newsapi.org](https://newsapi.org/)

### Step 3: Start Backend

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

The backend will start on `http://localhost:8080`

### Step 4: Start Pricing Service

```bash
cd pricing-service

# Option 1: Using provided script
./run.sh          # Linux/Mac
run.bat           # Windows

# Option 2: Manual setup
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000
```

The pricing service will start on `http://localhost:8000`

### Step 5: Start Frontend

```bash
cd frontend

# Option 1: Python
python -m http.server 5500

# Option 2: Node.js
npx http-server -p 5500

# Option 3: VS Code Live Server
# Right-click index.html → "Open with Live Server"
```

The frontend will be available at `http://localhost:5500`

### Step 6: Verify Setup

1. Open `http://localhost:5500` in your browser
2. Click "Sign Up" to create a new account
3. Check your email for OTP verification
4. Login and start adding clients and assets

## 🏛️ Backend Architecture

### Layer Structure

The backend follows a clean, layered architecture:

#### **Controller Layer** (`controller/`)
- **AuthController**: User registration, login, OTP verification
- **ClientController**: CRUD operations for client management
- **AssetController**: Asset CRUD, P&L calculations, bulk operations
- **DashboardController**: Portfolio summaries and analytics
- **PricingController**: Real-time price fetching and calculations
- **NewsController**: Financial news integration
- **StatementController**: PDF statement generation
- **ExcelController**: Data import/export functionality

#### **Service Layer** (`service/`)
- **auth/**: Authentication business logic, OTP management
- **asset/**: Asset management, P&L calculations
- **client/**: Client management operations
- **pricing/**: Price fetching from external APIs, caching
- **email/**: Email notifications, statement delivery
- **dashboard/**: Portfolio analytics and summaries

#### **Repository Layer** (`repository/`)
- **UserRepository**: User data access
- **ClientRepository**: Client data access with user filtering
- **AssetRepository**: Asset operations with client scoping
- **AssetPriceRepository**: Historical price data
- **OtpTokenRepository**: OTP token management

#### **DTO Layer** (`dto/`)
- **auth/**: Authentication request/response objects
- **asset/**: Asset management DTOs
- **client/**: Client data transfer objects
- **pricing/**: Price calculation DTOs
- **profile/**: User profile management

### Security Architecture

- **JWT Authentication**: Stateless token-based auth
- **Email OTP**: Two-factor authentication via email
- **Data Isolation**: Users can only access their own data
- **CORS Configuration**: Secure cross-origin requests
- **Password Encryption**: BCrypt hashing for passwords

### Exception Handling

- **Global Exception Handler**: Centralized error processing
- **Custom Exceptions**: Domain-specific error handling
- **Validation**: Input validation with meaningful error messages

## 🌐 Frontend Architecture

### Page Structure

The frontend is built with vanilla JavaScript and follows a modular structure:

#### **Authentication Pages** (`auth/`)
- **login.html**: User login with OTP verification
- **signup.html**: New user registration with email verification

#### **Dashboard Pages** (`dashboard/`)
- **home.html**: Main dashboard with portfolio overview
- **client-detail.html**: Detailed client portfolio view
- **charts.html**: Portfolio analytics and visualizations
- **client-charts.html**: Client-specific charts and metrics
- **import-data.html**: Excel file import interface
- **statements.html**: PDF statement generation and history
- **news.html**: Financial news feed
- **settings.html**: User profile and application settings

#### **JavaScript Modules** (`js/`)
- **api.js**: Centralized API communication with backend
- **auth.js**: Authentication logic and token management
- **dashboard.js**: Dashboard functionality and data rendering
- **charts.js**: Chart.js integration for portfolio visualization
- **client.js**: Client management operations
- **asset.js**: Asset CRUD operations
- **import-data.js**: Excel file processing and upload
- **statements.js**: PDF statement generation
- **news.js**: News feed integration

### API Integration

The frontend communicates with the backend through RESTful APIs:

```javascript
// Example API call structure
const api = {
    baseURL: 'http://localhost:8080/api',
    headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
    }
};

// Authentication flow
POST /api/auth/signup → POST /api/auth/verify-signup-otp → POST /api/auth/login → POST /api/auth/verify-login-otp

// Data operations
GET /api/clients → POST /api/clients → GET /api/clients/{id}/assets → POST /api/clients/{id}/assets
```

### State Management

- **JWT Tokens**: Stored in localStorage for session persistence
- **User Data**: Cached in memory for dashboard rendering
- **Real-time Updates**: Auto-refresh pricing data every 10 seconds
- **Error Handling**: Centralized error display and user feedback

## 📊 Application Flow

### Request Flow

```
Frontend (Browser)
        │
        │ HTTP Request (JWT Auth)
        ▼
Spring Boot Backend
        │
        │ Business Logic
        ▼
┌─────────────────┐    ┌─────────────────┐
│   Database      │    │ Pricing Service │
│   (MySQL)       │    │   (FastAPI)      │
└─────────────────┘    └─────────────────┘
```

### Authentication Flow

1. **User Registration**: 
   - Submit email/password → Backend creates user → Send OTP email
   - User enters OTP → Backend verifies → Account activated

2. **User Login**:
   - Submit credentials → Backend validates → Send OTP email
   - User enters OTP → Backend verifies → Return JWT token
   - Store token in localStorage for subsequent requests

### Portfolio Calculation Workflow

1. **Asset Addition**: User adds asset with purchase details
2. **Price Fetching**: Backend calls pricing service for current market prices
3. **P&L Calculation**: 
   ```
   Current Value = Quantity × Current Price
   Cost Basis = Quantity × Buying Rate
   P&L = Current Value - Cost Basis
   P&L % = (P&L / Cost Basis) × 100
   ```
4. **Portfolio Summary**: Aggregate all assets for client totals
5. **Real-time Updates**: Prices refresh every 10 seconds automatically

### Pricing Service Integration

The pricing microservice provides:
- **Current Prices**: Real-time market data from Yahoo Finance
- **Historical Data**: OHLCV data for charting
- **Multiple Symbols**: Batch price fetching for efficiency
- **Caching**: Reduces API calls and improves performance

## 🔧 Configuration & Environment Setup

### Application Properties Breakdown

```properties
# Database Connection
spring.datasource.url=jdbc:mysql://localhost:3306/portfolio_db
spring.datasource.username=your_db_user
spring.datasource.password=your_db_password

# JPA Settings
spring.jpa.hibernate.ddl-auto=update  # Auto-create/update schema
spring.jpa.show-sql=true               # Show SQL in logs (dev mode)
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# Security
app.jwt.secret=minimum-32-character-secret-key
app.jwt.expiration=86400000  # 24 hours

# Email (Gmail Example)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password  # Use App Password, not regular password

# External Services
app.news.api-key=your-newsapi-key
pricing.service.base-url=http://localhost:8000

# Server
server.port=8080
```

### Environment-Specific Configurations

#### Development Environment
- Enable SQL logging: `spring.jpa.show-sql=true`
- Use H2 in-memory database for testing
- Enable hot reload for frontend development

#### Production Environment
- Disable SQL logging: `spring.jpa.show-sql=false`
- Use production MySQL with connection pooling
- Enable SSL for database connections
- Configure proper logging levels

### Common Configuration Mistakes to Avoid

1. **Database Connection**: 
   - ❌ Using `localhost` in Docker containers
   - ✅ Use proper service names or environment variables

2. **JWT Security**:
   - ❌ Using short or predictable secrets
   - ✅ Use minimum 32-character random strings

3. **Email Configuration**:
   - ❌ Using regular Gmail password
   - ✅ Generate App Password from Google Account settings

4. **CORS Issues**:
   - ❌ Frontend on different port without CORS config
   - ✅ Update `CorsConfig.java` with frontend URL

## 📡 API Overview

### Core Endpoints

#### Authentication
- `POST /api/auth/signup` - Register new user
- `POST /api/auth/verify-signup-otp` - Verify registration OTP
- `POST /api/auth/login` - User login
- `POST /api/auth/verify-login-otp` - Verify login OTP

#### Client Management
- `GET /api/clients` - List all clients for authenticated user
- `POST /api/clients` - Create new client
- `GET /api/clients/{id}` - Get client details
- `PUT /api/clients/{id}` - Update client information
- `DELETE /api/clients/{id}` - Delete client

#### Asset Management
- `GET /api/clients/{clientId}/assets` - List client assets
- `POST /api/clients/{clientId}/assets` - Add new asset
- `PUT /api/assets/{id}` - Update asset details
- `DELETE /api/assets/{id}` - Delete asset
- `GET /api/clients/{clientId}/pnl` - Calculate P&L for client

#### Pricing & Analytics
- `GET /api/pricing/price/{symbol}` - Get current price for symbol
- `POST /api/prices` - Get multiple prices (batch request)
- `GET /api/dashboard/summary` - Portfolio summary statistics
- `GET /api/charts/{clientId}` - Chart data for visualizations

#### Data Import/Export
- `POST /api/excel/import` - Import Excel file with assets
- `GET /api/excel/export` - Export portfolio to Excel
- `POST /api/statements/generate` - Generate PDF statement

#### News & External Data
- `GET /api/news` - Get financial news feed

### Response Format

All API responses follow a consistent format:

```json
{
  "success": true,
  "data": { /* Response data */ },
  "message": "Operation completed successfully",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

Error responses:
```json
{
  "success": false,
  "error": "Validation failed",
  "details": "Email is required",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

## 👥 Development Workflow

### Team Collaboration

#### Frontend Development
1. **Setup**: Install Node.js and use any static file server
2. **Development**: Use VS Code Live Server for hot reload
3. **API Mocking**: Use mock data when backend is unavailable
4. **Testing**: Test responsive design and cross-browser compatibility

#### Backend Development
1. **Setup**: Install Java 21, Maven, and MySQL
2. **Database**: Use local MySQL or Docker container
3. **Testing**: Write unit tests for service layer
4. **API Testing**: Use Postman or curl for endpoint testing

#### Pricing Service Development
1. **Setup**: Python 3.8+ with pip
2. **Dependencies**: Install from `requirements.txt`
3. **Testing**: Use provided test scripts
4. **Monitoring**: Check API rate limits and caching

### Integration Workflow

1. **API Contract**: Define endpoints and data structures first
2. **Parallel Development**: Frontend and backend can work independently
3. **Integration Testing**: Test full request-response cycles
4. **Performance Testing**: Load test pricing service and database queries

### Code Organization Best Practices

#### Backend
- Follow Spring Boot conventions and package structure
- Use DTOs for API communication, not entities directly
- Implement proper exception handling with global exception handler
- Write unit tests for service layer business logic

#### Frontend
- Keep JavaScript modular and reusable
- Use consistent error handling across all API calls
- Implement proper loading states and user feedback
- Follow responsive design principles

### Deployment Considerations

#### Development Deployment
- Use embedded H2 database for quick testing
- Enable debug logging and hot reload
- Use local file storage for uploads

#### Production Deployment
- Use MySQL with proper connection pooling
- Configure proper logging and monitoring
- Use cloud storage for file uploads
- Enable SSL/TLS for all communications
- Set up proper backup and recovery procedures

## 📋 Excel Import Format

The system supports bulk data import through Excel files. Use the following format:

### Required Columns
1. **Client Name** - Name of the client (must match existing client)
2. **Asset Name** - Description of the asset
3. **Category** - Asset type: `STOCK`, `MUTUAL_FUND`, `BOND`, `CRYPTO`, `COMMODITY`, `FOREX`
4. **Symbol** - Trading symbol (e.g., AAPL, GOOGL, BTC-USD)
5. **Quantity** - Number of units owned (decimal format supported)
6. **Buying Rate** - Purchase price per unit
7. **Purchase Date** - Purchase date in `YYYY-MM-DD` format

### Sample CSV Format
```csv
Client Name,Asset Name,Category,Symbol,Quantity,Buying Rate,Purchase Date
John Doe,Apple Inc.,STOCK,AAPL,10,150.25,2023-01-15
John Doe,Microsoft Corp.,STOCK,MSFT,5,320.50,2023-02-20
Jane Smith,Bitcoin,CRYPTO,BTC-USD,0.5,45000.00,2023-03-10
```

### Import Process
1. Navigate to **Import Data** in the dashboard
2. Upload your Excel/CSV file
3. System validates data and shows preview
4. Confirm import to add assets to client portfolios

## 🐛 Troubleshooting

### Common Issues and Solutions

#### Database Connection Errors
**Problem**: `Connection refused` or `Access denied`
**Solutions**:
- Ensure MySQL server is running: `sudo systemctl start mysql`
- Verify database exists: `SHOW DATABASES;`
- Check credentials in `application.properties`
- Confirm user has privileges: `GRANT ALL ON portfolio_db.* TO 'user'@'localhost';`

#### Email Not Sending
**Problem**: OTP emails not received
**Solutions**:
- Use Gmail App Password (not regular password)
- Check spam/junk folders
- Verify SMTP settings in `application.properties`
- Test with different email provider if needed

#### CORS Errors
**Problem**: Frontend cannot connect to backend
**Solutions**:
- Update `CorsConfig.java` with frontend URL
- Ensure backend is running on correct port (8080)
- Check browser console for specific CORS errors

#### JWT Authentication Issues
**Problem**: Token expired or invalid
**Solutions**:
- JWT secret must be at least 32 characters
- Check token expiration time (default 24 hours)
- Clear browser localStorage and re-login

#### Pricing Service Not Responding
**Problem**: No real-time price updates
**Solutions**:
- Ensure pricing service is running on port 8000
- Check `pricing.service.base-url` configuration
- Verify Python dependencies: `pip install -r requirements.txt`
- Monitor API rate limits from Yahoo Finance

#### Excel Import Failures
**Problem**: File upload or parsing errors
**Solutions**:
- Verify file format (CSV or Excel)
- Check column headers match required format
- Ensure dates are in `YYYY-MM-DD` format
- Validate numeric fields don't contain special characters

### Debug Mode

Enable debug logging by adding to `application.properties`:
```properties
logging.level.com.app.portfolio=DEBUG
spring.jpa.show-sql=true
logging.level.org.springframework.web=DEBUG
```

### Performance Issues

#### Slow Database Queries
- Add database indexes on frequently queried columns
- Use connection pooling: `spring.datasource.hikari.maximum-pool-size=20`
- Enable query caching: `spring.jpa.properties.hibernate.cache.use_second_level_cache=true`

#### Frontend Performance
- Enable browser caching for static assets
- Optimize image sizes and formats
- Use lazy loading for large datasets
- Implement pagination for client lists

## 📚 Additional Resources

### Documentation Files
- **PRICING_MICROSERVICE.md** - Detailed pricing service setup and API documentation
- **PRICING_SERVICE_SETUP.md** - Step-by-step pricing service installation guide

### Sample Data Files
- **sample-portfolio.csv** - Sample portfolio data for testing
- **sample-portfolio-multicurrency.csv** - Multi-currency portfolio example
- **new_assets.csv** - Additional sample assets

### External Dependencies
- **Spring Boot Documentation** - [https://spring.io/projects/spring-boot](https://spring.io/projects/spring-boot)
- **MySQL Documentation** - [https://dev.mysql.com/doc/](https://dev.mysql.com/doc/)
- **FastAPI Documentation** - [https://fastapi.tiangolo.com/](https://fastapi.tiangolo.com/)
- **yfinance Documentation** - [https://pypi.org/project/yfinance/](https://pypi.org/project/yfinance/)

## 🤝 Contributing Guidelines

### Code Style
- **Backend**: Follow Spring Boot conventions and Java best practices
- **Frontend**: Use ES6+ features and maintain consistent formatting
- **Database**: Use descriptive table and column names
- **API**: Follow RESTful principles and consistent response formats

### Testing
- Write unit tests for business logic
- Test API endpoints with various scenarios
- Validate frontend functionality across browsers
- Test pricing service with different symbols

### Security Considerations
- Never commit secrets or passwords to version control
- Use environment variables for sensitive configuration
- Validate all user inputs
- Implement proper error handling without information leakage

## 📄 License

This project is for educational and demonstration purposes. Please refer to the LICENSE file for specific usage terms.

---

## 🎉 Summary

The CapitalX Portfolio Management System provides a complete solution for fund managers to:

- **Manage multiple clients** and their investment portfolios
- **Track real-time P&L** with automatic price updates
- **Generate professional reports** in PDF and Excel formats
- **Ensure security** with JWT authentication and OTP verification
- **Scale efficiently** with microservices architecture

The system is built with modern technologies and follows best practices for maintainability, security, and performance. Whether you're a student learning full-stack development or a professional building portfolio management tools, CapitalX provides a solid foundation to build upon.

For technical support or questions, refer to the troubleshooting section or explore the additional documentation files in the project repository.
