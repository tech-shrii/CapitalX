# Pricing Service Setup Guide

## Quick Start

The pricing service is a Python FastAPI microservice that provides real-time stock prices and chart data using Yahoo Finance.

## Prerequisites

- Python 3.8 or higher
- pip (Python package manager)

## Installation & Running

### Option 1: Using the provided script (Linux/Mac)

```bash
cd pricing-service
chmod +x run.sh
./run.sh
```

### Option 2: Manual setup (Windows/Linux/Mac)

1. **Navigate to the pricing-service directory:**
   ```bash
   cd pricing-service
   ```

2. **Install dependencies:**
   ```bash
   pip install -r requirements.txt
   ```

3. **Run the service:**
   ```bash
   uvicorn main:app --reload --port 8000
   ```

### Option 3: Using Python directly

```bash
cd pricing-service
pip install -r requirements.txt
python main.py
```

## Verify It's Running

Once started, you should see output like:
```
INFO:     Uvicorn running on http://0.0.0.0:8000 (Press CTRL+C to quit)
INFO:     Started reloader process
INFO:     Started server process
INFO:     Waiting for application startup.
INFO:     Application startup complete.
```

Test the health endpoint:
```bash
curl http://localhost:8000/api/health
```

Expected response:
```json
{
  "status": "healthy",
  "timestamp": "2026-02-01T10:30:00",
  "service": "Pricing Service"
}
```

## Service Endpoints

The pricing service runs on **port 8000** and provides:

- `GET /api/health` - Health check
- `GET /api/price/{symbol}` - Current price for a symbol (e.g., AAPL, MSFT)
- `GET /api/chart/{symbol}?period=1mo&interval=1d` - Chart data
- `POST /api/prices` - Multiple prices at once
- `POST /api/portfolio/value` - Portfolio total value
- `POST /api/portfolio/chart` - Combined portfolio chart

## Configuration

The service uses in-memory caching with TTL:
- Current prices: 60 seconds cache
- Daily charts: 3600 seconds (1 hour)
- Intraday charts: 300 seconds (5 minutes)

## Troubleshooting

### Port Already in Use
If port 8000 is already in use:
```bash
# Find the process using port 8000
# Windows:
netstat -ano | findstr :8000

# Linux/Mac:
lsof -i :8000

# Then kill the process or use a different port:
uvicorn main:app --reload --port 8001
```

### Python Not Found
Make sure Python is installed and in your PATH:
```bash
python --version
# or
python3 --version
```

### Dependencies Installation Fails
Try upgrading pip first:
```bash
pip install --upgrade pip
pip install -r requirements.txt
```

### Service Not Responding
1. Check if the service is running (look for uvicorn output)
2. Verify the port: `curl http://localhost:8000/api/health`
3. Check firewall settings
4. Ensure no other service is using port 8000

## Integration with Backend

The Spring Boot backend expects the pricing service at:
```
http://localhost:8000
```

This is configured in `backend/src/main/resources/application.properties`:
```properties
pricing.service.url=http://localhost:8000
```

## Running in Production

For production, use a process manager like:
- **systemd** (Linux)
- **supervisor**
- **PM2** (Node.js process manager)
- **Docker** (containerized)

Example with gunicorn (production WSGI server):
```bash
pip install gunicorn
gunicorn main:app -w 4 -k uvicorn.workers.UvicornWorker --bind 0.0.0.0:8000
```

## Notes

- The service uses Yahoo Finance API (yfinance library)
- Requires internet connection to fetch real-time data
- Free tier has rate limits - consider caching for high traffic
- Some symbols may not be available depending on exchange

---

**Status:** Ready to use
**Last Updated:** February 1, 2026
