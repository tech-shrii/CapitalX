#!/bin/bash
# Install dependencies
pip install -r requirements.txt

# Run the service
uvicorn main:app --reload --port 8000
