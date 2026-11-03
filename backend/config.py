import os
from datetime import timedelta

class Config:
    # Basic configuration
    SECRET_KEY = os.environ.get('SECRET_KEY') or 'dev-secret-key-change-in-production'
    DEBUG = os.environ.get('DEBUG', 'False').lower() == 'true'
    
    # Database configuration
    INFLUXDB_URL = os.environ.get('INFLUXDB_URL') or 'http://localhost:8086'
    INFLUXDB_TOKEN = os.environ.get('INFLUXDB_TOKEN') or 'your-token-here'
    INFLUXDB_ORG = os.environ.get('INFLUXDB_ORG') or 'sunlink'
    INFLUXDB_BUCKET = os.environ.get('INFLUXDB_BUCKET') or 'telemetry'
    
    # MQTT configuration
    MQTT_BROKER = os.environ.get('MQTT_BROKER') or 'localhost'
    MQTT_PORT = int(os.environ.get('MQTT_PORT') or 1883)
    MQTT_USERNAME = os.environ.get('MQTT_USERNAME') or 'sunlink'
    MQTT_PASSWORD = os.environ.get('MQTT_PASSWORD') or 'sunlink123'
    MQTT_TOPIC_PREFIX = os.environ.get('MQTT_TOPIC_PREFIX') or 'sunlink/'
    
    # JWT configuration
    JWT_SECRET_KEY = os.environ.get('JWT_SECRET_KEY') or 'jwt-secret-string'
    JWT_ACCESS_TOKEN_EXPIRES = timedelta(hours=1)
    JWT_REFRESH_TOKEN_EXPIRES = timedelta(days=30)
    
    # API configuration
    API_HOST = os.environ.get('API_HOST') or '0.0.0.0'
    API_PORT = int(os.environ.get('API_PORT') or 5000)
    API_WORKERS = int(os.environ.get('API_WORKERS') or 1)
    
    # WebSocket configuration
    WS_HOST = os.environ.get('WS_HOST') or '0.0.0.0'
    WS_PORT = int(os.environ.get('WS_PORT') or 5001)
    
    # CORS configuration
    CORS_ORIGINS = os.environ.get('CORS_ORIGINS') or '*'
    
    # Logging configuration
    LOG_LEVEL = os.environ.get('LOG_LEVEL') or 'INFO'
    LOG_FILE = os.environ.get('LOG_FILE') or '/opt/sunlink/logs/sunlink.log'
    
    # Inverter connection settings
    INVERTER_TIMEOUT = int(os.environ.get('INVERTER_TIMEOUT') or 30)
    INVERTER_RECONNECT_DELAY = int(os.environ.get('INVERTER_RECONNECT_DELAY') or 60)
    
    # Data retention settings
    DATA_RETENTION_DAYS = int(os.environ.get('DATA_RETENTION_DAYS') or 365)
    
    # Security settings
    RATELIMIT_STORAGE_URL = os.environ.get('RATELIMIT_STORAGE_URL') or 'redis://localhost:6379'
    RATELIMIT_DEFAULT = os.environ.get('RATELIMIT_DEFAULT') or '200 per hour'
    
    # Mobile app configuration
    MOBILE_APP_URL = os.environ.get('MOBILE_APP_URL') or 'http://localhost:3000'
    MOBILE_APP_WS_URL = os.environ.get('MOBILE_APP_WS_URL') or 'ws://localhost:3001'
    
    # Email configuration (for notifications)
    SMTP_SERVER = os.environ.get('SMTP_SERVER') or 'localhost'
    SMTP_PORT = int(os.environ.get('SMTP_PORT') or 587)
    SMTP_USERNAME = os.environ.get('SMTP_USERNAME') or ''
    SMTP_PASSWORD = os.environ.get('SMTP_PASSWORD') or ''
    FROM_EMAIL = os.environ.get('FROM_EMAIL') or 'noreply@sunlink.com'
    
    # Push notification configuration
    FCM_SERVER_KEY = os.environ.get('FCM_SERVER_KEY') or ''
    APNS_CERT_PATH = os.environ.get('APNS_CERT_PATH') or ''
    
    # Webhook configuration
    WEBHOOK_URL = os.environ.get('WEBHOOK_URL') or ''
    WEBHOOK_SECRET = os.environ.get('WEBHOOK_SECRET') or ''
    
    # Performance monitoring
    ENABLE_METRICS = os.environ.get('ENABLE_METRICS', 'True').lower() == 'true'
    METRICS_PORT = int(os.environ.get('METRICS_PORT') or 9090)
    
    # Development settings
    TESTING = os.environ.get('TESTING', 'False').lower() == 'true'
    PROPAGATE_EXCEPTIONS = os.environ.get('PROPAGATE_EXCEPTIONS', 'True').lower() == 'true'

class DevelopmentConfig(Config):
    DEBUG = True
    
class ProductionConfig(Config):
    DEBUG = False
    
class TestingConfig(Config):
    TESTING = True
    INFLUXDB_BUCKET = 'telemetry_test'

# Configuration dictionary
config = {
    'development': DevelopmentConfig,
    'production': ProductionConfig,
    'testing': TestingConfig,
    'default': DevelopmentConfig
}

def get_config():
    env = os.environ.get('FLASK_ENV', 'default')
    return config[env]