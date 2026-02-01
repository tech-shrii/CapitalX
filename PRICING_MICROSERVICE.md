# Pricing Microservice Architecture

Complete microservice setup for real-time stock pricing, charts, and portfolio analytics with automatic 10-second updates.

## 🏗️ Architecture Overview

```
Frontend (JavaScript)
        |
        | REST API (10s auto-refresh)
        v
Spring Boot Backend
        |
        | HTTP Client (RestTemplate)
        v
Python FastAPI Pricing Service (port 8000)
        |
        | yfinance
        v
Yahoo Finance API
```

## 📦 Components

### 1. Python Microservice (`pricing-service/`)

**Endpoints:**
- `GET /api/price/{symbol}` - Current price for a symbol
- `GET /api/chart/{symbol}?period=1mo&interval=1d` - OHLC chart data
- `POST /api/prices` - Multiple prices at once
- `POST /api/portfolio/value` - Portfolio total value and breakdown
- `POST /api/portfolio/chart` - Combined historical portfolio chart
- `GET /api/health` - Health check

**Features:**
- In-memory caching with TTL (configurable expiry)
- Automatic cache cleanup on expired entries
- Support for multiple time periods and intervals
- Error handling with graceful fallbacks

**Installation:**
```bash
cd pricing-service
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
```

### 2. Spring Boot Backend

**PricingService Interface:**
```java
public interface PricingService {
    PriceResponse getCurrentPriceBySymbol(String symbol);
    ChartResponse getChartData(String symbol, String period, String interval);
    PortfolioValueResponse getPortfolioValue(Map<String, Double> portfolio);
    PortfolioChartResponse getPortfolioChart(Map<String, Double> portfolio, String period, String interval);
    Map<String, Object> getMultiplePrices(List<String> symbols);
}
```

**PricingController:**
- Exposes REST endpoints for frontend consumption
- Handles authentication via JWT tokens
- Delegates to PricingService for business logic

**Scheduled Updates:**
- `@Scheduled(fixedRate = 10000)` - Updates every 10 seconds
- Fetches prices from Python service or uses fake data fallback
- Stores in database for historical tracking

### 3. Frontend Components

**Charts Dashboard** (`frontend/dashboard/charts.html`):
- Real-time portfolio value display
- Combined portfolio performance chart (line chart)
- Individual asset performance charts
- Current prices table with auto-update
- Period selector (1d, 5d, 1mo, 3mo, 6mo, 1y)

**Auto-Refresh Mechanism** (`frontend/js/charts.js`):
- 10-second auto-refresh interval
- Fetches portfolio value and prices
- Updates all charts in real-time
- Handles errors gracefully

## 🔄 Data Flow

### Real-Time Price Update (10 seconds)

1. **Backend Scheduler** runs every 10 seconds:
   ```
   PricingServiceImpl.fetchAndUpdatePrices()
   ↓
   For each asset in portfolio:
     - Fetch from Python service
     - Save to AssetPrice table
   ```

2. **Frontend Auto-Refresh** every 10 seconds:
   ```
   JavaScript interval
   ↓
   updateAllData()
   ├─ updatePortfolioValue() → /api/pricing/portfolio/value
   ├─ updatePortfolioChart() → /api/pricing/portfolio/chart
   ├─ updateAssetPrices() → /api/pricing/prices
   └─ updateAssetChart() → /api/pricing/chart/{symbol}
   ```

## ⚙️ Configuration

### application.properties

```properties
# Pricing Service Configuration
pricing.service.url=http://localhost:8000

# Cache settings
spring.cache.type=simple
spring.cache.cache-names=prices,charts
```

### Python Service Caching

Default TTLs:
- Current prices: 60 seconds
- Daily charts: 3600 seconds (1 hour)
- Intraday charts: 300 seconds (5 minutes)
- Portfolio data: 60 seconds

Configure in `pricing-service/main.py`:
```python
cache.set(cache_key, response, ttl=60)  # 60 seconds
```

## 📊 Portfolio Structure

Frontend sends portfolio as JSON:
```json
{
  "AAPL": 10,
  "MSFT": 5,
  "GOOGL": 2
}
```

Where key = symbol, value = quantity owned.

## 🎯 Features

### ✅ Implemented

- [x] Real-time price updates (10 seconds)
- [x] Combined portfolio performance chart
- [x] Individual asset charts
- [x] In-memory caching with TTL
- [x] Multi-period support (1d, 5d, 1mo, 3mo, 6mo, 1y)
- [x] Portfolio value breakdown
- [x] Error handling and fallbacks
- [x] Security (JWT authentication)

### 🚀 Future Enhancements

- [ ] Redis caching for distributed systems
- [ ] WebSocket real-time streaming
- [ ] Technical indicators (MA, RSI, MACD)
- [ ] Performance analytics and benchmarking
- [ ] Portfolio alerts and notifications
- [ ] Export charts as PDF
- [ ] Historical comparison tools

## 🛠️ Troubleshooting

### Python Service Not Responding
```bash
# Check if service is running
curl http://localhost:8000/api/health

# View logs
# Check console output from uvicorn
```

### Prices Not Updating
- Check `pricing.service.url` in `application.properties`
- Verify Python service is running on port 8000
- Check Spring Boot logs for HTTP errors
- Ensure yfinance has internet access

### Charts Not Loading
- Check browser console for errors
- Verify Chart.js library is loaded
- Ensure token is in localStorage
- Check API response in Network tab

## 📝 API Examples

### Get Current Price
```bash
curl -H "Authorization: Bearer TOKEN" \
  http://localhost:8080/api/pricing/price/AAPL
```

Response:
```json
{
  "symbol": "AAPL",
  "price": 150.25,
  "timestamp": "2024-02-01T10:30:00",
  "currency": "USD"
}
```

### Get Portfolio Value
```bash
curl -X POST -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"AAPL": 10, "MSFT": 5}' \
  http://localhost:8080/api/pricing/portfolio/value
```

Response:
```json
{
  "totalValue": 2500.75,
  "breakdown": {
    "AAPL": {
      "price": 150.25,
      "quantity": 10,
      "value": 1502.50
    },
    "MSFT": {
      "price": 199.65,
      "quantity": 5,
      "value": 998.25
    }
  },
  "timestamp": "2024-02-01T10:30:00"
}
```

## 📚 Dependencies

### Python (`pricing-service/requirements.txt`)
- fastapi==0.104.1
- uvicorn==0.24.0
- yfinance==0.2.32
- python-dotenv==1.0.0

### Java
- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-security
- org.springframework.restdocs

### Frontend
- Chart.js 4.4.0 (via CDN)

## 🔐 Security Notes

- All endpoints require JWT authentication (except `/api/health`)
- Rate limiting recommended for production
- API key for yfinance if needed
- CORS should be configured appropriately

## 📄 File Structure

```
f-manager/
├── pricing-service/
│   ├── main.py                  # FastAPI app
│   ├── requirements.txt
│   └── run.sh
├── backend/
│   ├── src/main/java/com/app/portfolio/
│   │   ├── controller/
│   │   │   └── PricingController.java
│   │   ├── dto/pricing/
│   │   │   ├── PriceResponse.java
│   │   │   ├── ChartResponse.java
│   │   │   ├── PortfolioValueResponse.java
│   │   │   └── PortfolioChartResponse.java
│   │   └── service/pricing/
│   │       ├── PricingService.java (interface)
│   │       └── PricingServiceImpl.java
│   └── application.properties
└── frontend/
    ├── dashboard/
    │   ├── charts.html
    │   └── home.html
    ├── js/
    │   ├── charts.js
    │   └── api.js
    └── css/
        └── dashboard.css
```

## 🎓 Learning Resources

- [FastAPI Documentation](https://fastapi.tiangolo.com/)
- [yfinance GitHub](https://github.com/ranaroussi/yfinance)
- [Spring Boot REST Guide](https://spring.io/guides/gs/rest-service/)
- [Chart.js Documentation](https://www.chartjs.org/)

---

**Last Updated:** February 1, 2026
**Status:** Production Ready
