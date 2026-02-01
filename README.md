# Fund Manager Portfolio Dashboard

A full-stack web application for fund managers to track, manage, and report on client investments.

## Features

- **Authentication & Security**: JWT-based authentication with email OTP 2FA
- **Client Management**: Add, edit, and manage multiple clients
- **Asset Tracking**: Track stocks, FDs, bonds, crypto, metals, and mutual funds
- **Real-time P&L**: Calculate profit/loss with simulated real-time price updates
- **Financial News**: Integrated news feed from NewsAPI
- **Statement Generation**: Generate and email PDF statements to clients
- **Excel Import/Export**: Import existing data and export reports

## Technology Stack

### Backend
- Java 21
- Spring Boot 3.2.0
- Spring Security (JWT)
- Spring Data JPA
- MySQL
- Maven

### Frontend
- HTML5
- CSS3
- Vanilla JavaScript

## Prerequisites

- Java 21 or higher
- Maven 3.6+
- MySQL 8.0+
- Node.js (for serving frontend, or use any static file server)

## Setup Instructions

### 1. Database Setup

Create a MySQL database:

```sql
CREATE DATABASE portfolio_db;
```

Update `backend/src/main/resources/application.properties` with your database credentials:

```properties
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### 2. Email Configuration

Update email settings in `application.properties`:

```properties
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
```

**Note**: For Gmail, you need to generate an App Password:
1. Go to Google Account settings
2. Enable 2-Step Verification
3. Generate an App Password for "Mail"

### 3. News API Configuration

Get a free API key from [NewsAPI.org](https://newsapi.org/) and update:

```properties
app.news.api-key=your-newsapi-key
```

### 4. JWT Secret

Update the JWT secret in `application.properties`:

```properties
app.jwt.secret=your-secret-key-minimum-32-characters-long
```

### 5. Build and Run Backend

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

The backend will start on `http://localhost:8080`

### 6. Serve Frontend

You can use any static file server. Options:

**Using Python:**
```bash
cd frontend
python -m http.server 5500
```

**Using Node.js (http-server):**
```bash
npm install -g http-server
cd frontend
http-server -p 5500
```

**Using VS Code Live Server:**
- Install Live Server extension
- Right-click on `index.html` and select "Open with Live Server"

The frontend will be available at `http://localhost:5500`

## Project Structure

```
f-manager/
├── backend/
│   ├── src/main/java/com/app/portfolio/
│   │   ├── beans/          # JPA Entities
│   │   ├── repository/     # JPA Repositories
│   │   ├── dto/            # Data Transfer Objects
│   │   ├── mapper/         # DTO ↔ Entity Mappers
│   │   ├── service/        # Business Logic
│   │   ├── controller/     # REST Controllers
│   │   ├── security/       # Security Configuration
│   │   └── exceptions/     # Exception Handlers
│   └── pom.xml
└── frontend/
    ├── index.html          # Landing page
    ├── auth/               # Authentication pages
    ├── dashboard/          # Dashboard pages
    ├── css/                # Stylesheets
    └── js/                 # JavaScript files
```

## API Endpoints

### Authentication
- `POST /api/auth/signup` - Register new user
- `POST /api/auth/verify-signup-otp` - Verify signup OTP
- `POST /api/auth/login` - Login
- `POST /api/auth/verify-login-otp` - Verify login OTP

### Clients
- `GET /api/clients` - Get all clients
- `GET /api/clients/{id}` - Get client by ID
- `POST /api/clients` - Create client
- `PUT /api/clients/{id}` - Update client
- `DELETE /api/clients/{id}` - Delete client

### Assets
- `GET /api/clients/{clientId}/assets` - Get assets for client
- `POST /api/clients/{clientId}/assets` - Add asset
- `PUT /api/assets/{id}` - Update asset
- `DELETE /api/assets/{id}` - Delete asset
- `GET /api/clients/{clientId}/pnl` - Calculate P&L

### Dashboard
- `GET /api/dashboard/summary` - Get dashboard summary

### News
- `GET /api/news` - Get financial news

### Statements
- `POST /api/statements/generate` - Generate and send statement

### Excel
- `POST /api/excel/import` - Import Excel file
- `GET /api/excel/export` - Export to Excel

### Profile
- `GET /api/profile` - Get user profile
- `PUT /api/profile` - Update profile
- `POST /api/profile/reset-password` - Reset password

## Excel Import Format

The Excel file should have the following columns:
1. Client Name
2. Asset Name
3. Category (STOCK, FD, BOND, CRYPTO, METAL, MUTUAL_FUND)
4. Symbol
5. Quantity
6. Buying Rate
7. Purchase Date (YYYY-MM-DD format)

## Pricing Service

The pricing service runs every 6 hours to fetch prices. Currently, it uses fake data for testing. To integrate with yfinance:

1. Create a Python script that uses yfinance
2. Call it from Java using ProcessBuilder
3. Update `PricingServiceImpl.fetchPriceFromYFinance()` method

## Security

- All API endpoints (except `/api/auth/**`) require JWT authentication
- JWT tokens expire after 24 hours (configurable)
- OTP tokens expire after 10 minutes
- Passwords are encrypted using BCrypt
- Data isolation ensures users can only access their own data

## Development Notes

- The application uses Hibernate's `ddl-auto=update` for automatic schema creation
- CORS is configured to allow requests from common frontend ports
- Email sending is asynchronous to avoid blocking requests
- PDF generation uses OpenPDF library

## Troubleshooting

1. **Database connection error**: Check MySQL is running and credentials are correct
2. **Email not sending**: Verify Gmail App Password is correct
3. **CORS errors**: Ensure frontend URL is in `corsConfigurationSource()` allowed origins
4. **JWT errors**: Check JWT secret is at least 32 characters long

## License

This project is for educational/demonstration purposes.
