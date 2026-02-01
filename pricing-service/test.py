import yfinance as yf

# Get data for Apple
aapl = yf.Ticker("AAPL")

# Get historical market data
hist = aapl.history(period="1mo")
print(hist)

# Download data for multiple tickers
data = yf.download("SPY AAPL", start="2025-01-01", end="2025-01-31")
print(data)
