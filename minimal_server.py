#!/usr/bin/env python3
"""
Минимальный сервер для тестирования
"""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import uvicorn

app = FastAPI(title="Deye Gateway API", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/")
async def root():
    return {"message": "Deye Gateway API", "version": "1.0.0"}

@app.get("/status")
async def get_status():
    return {
        "connection_status": False,
        "last_update": "2026-10-31T10:35:00",
        "active_writes": 0,
        "last_write_time": "1970-01-01T03:00:00",
        "emergency_stop": False,
        "available_registers": 39,
        "message": "Server running - waiting for inverter connection"
    }

if __name__ == "__main__":
    uvicorn.run("minimal_server:app", host="0.0.0.0", port=8800, reload=False)