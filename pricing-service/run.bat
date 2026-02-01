@echo off
echo Installing dependencies...
pip install -r requirements.txt

echo.
echo Starting Pricing Service on port 8000...
echo Press CTRL+C to stop the service
echo.

uvicorn main:app --reload --port 8000

pause
