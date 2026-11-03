#!/usr/bin/env python3
"""
Simple SunLink API Server for testing
"""

import os
import logging
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('/opt/sunlink/logs/sunlink.log'),
        logging.StreamHandler()
    ]
)

logger = logging.getLogger(__name__)

app = FastAPI(
    title="SunLink API",
    description="Solar Monitoring System API",
    version="1.0.0"
)

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/")
async def root():
    return {"message": "SunLink API is running!"}

@app.get("/health")
async def health_check():
    return {"status": "healthy", "timestamp": os.urandom(4).hex()}

@app.get("/api/v1/inverters")
async def get_inverters():
    return {
        "inverters": [
            {
                "id": "INV001",
                "name": "Main Inverter",
                "status": "online",
                "power": 4.5,
                "temperature": 35.2,
                "efficiency": 94.5
            }
        ]
    }

if __name__ == "__main__":
    logger.info("Starting SunLink simple API server...")
    uvicorn.run(app, host="0.0.0.0", port=5000, log_level="info")