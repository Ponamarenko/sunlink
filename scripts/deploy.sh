#!/bin/bash

# SunLink Deployment Script
# Автоматическое развертывание SunLink системы

set -e

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Логирование
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Проверка прав
check_permissions() {
    if [ "$EUID" -ne 0 ]; then
        log_error "This script must be run as root"
        exit 1
    fi
}

# Проверка зависимостей
check_dependencies() {
    log_info "Checking dependencies..."
    
    # Docker
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed"
        exit 1
    fi
    
    # Docker Compose
    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose is not installed"
        exit 1
    fi
    
    # Curl
    if ! command -v curl &> /dev/null; then
        log_error "Curl is not installed"
        exit 1
    fi
    
    log_info "All dependencies are installed"
}

# Создание директорий
create_directories() {
    log_info "Creating directories..."
    
    mkdir -p logs
    mkdir -p ssl
    mkdir -p mosquitto/config
    mkdir -p mosquitto/data
    mkdir -p mosquitto/log
    mkdir -p influxdb/config
    mkdir -p monitoring
    
    log_info "Directories created successfully"
}

# Генерация SSL сертификатов
generate_ssl_certificates() {
    log_info "Generating SSL certificates..."
    
    # Проверка существующих сертификатов
    if [ -f "ssl/cert.pem" ] && [ -f "ssl/key.pem" ]; then
        log_warn "SSL certificates already exist"
        return
    fi
    
    # Генерация самоподписанных сертификатов
    openssl req -x509 -newkey rsa:4096 -keyout ssl/key.pem -out ssl/cert.pem -days 365 -nodes \
        -subj "/C=UA/ST=Kiev/L=Kiev/O=SunLink/OU=IT/CN=sunlink.pp.ua"
    
    log_info "SSL certificates generated successfully"
}

# Копирование конфигурационных файлов
copy_config_files() {
    log_info "Copying configuration files..."
    
    # Копирование .env файла
    if [ ! -f ".env" ]; then
        cp .env.example .env
        log_warn ".env file created from template. Please review and update with your settings."
    fi
    
    # Копирование конфигурации Mosquitto
    cp mosquitto/config/mosquitto.conf mosquitto/config/
    cp mosquitto/config/password.txt mosquitto/config/
    cp mosquitto/config/acl.conf mosquitto/config/
    
    # Копирование конфигурации InfluxDB
    cp influxdb/config/config.yml influxdb/config/
    
    # Копирование конфигурации Prometheus
    cp monitoring/prometheus.yml monitoring/
    
    log_info "Configuration files copied successfully"
}

# Настройка прав
setup_permissions() {
    log_info "Setting up permissions..."
    
    chmod +x scripts/deploy.sh
    chmod 600 mosquitto/config/password.txt
    chmod 600 ssl/key.pem
    chmod 644 ssl/cert.pem
    
    # Права для директорий
    chown -R 1000:1000 mosquitto/data
    chown -R 1000:1000 mosquitto/log
    chown -R 1000:1000 influxdb/config
    chown -R 1000:1000 logs
    
    log_info "Permissions set successfully"
}

# Проверка Docker Compose
check_docker_compose() {
    log_info "Checking Docker Compose configuration..."
    
    if [ ! -f "docker-compose.yml" ]; then
        log_error "docker-compose.yml not found"
        exit 1
    fi
    
    # Проверка синтаксиса
    if ! docker-compose config -q > /dev/null 2>&1; then
        log_error "Docker Compose configuration is invalid"
        exit 1
    fi
    
    log_info "Docker Compose configuration is valid"
}

# Остановка существующих сервисов
stop_services() {
    log_info "Stopping existing services..."
    
    docker-compose down --remove-orphans
    
    log_info "Services stopped successfully"
}

# Сборка образов
build_images() {
    log_info "Building Docker images..."
    
    docker-compose build --no-cache
    
    log_info "Docker images built successfully"
}

# Запуск сервисов
start_services() {
    log_info "Starting services..."
    
    docker-compose up -d
    
    log_info "Services started successfully"
}

# Проверка здоровья сервисов
check_health() {
    log_info "Checking service health..."
    
    # Ожидание запуска сервисов
    sleep 30
    
    # Проверка FastAPI
    if curl -f http://localhost:8000/api/v1/health > /dev/null 2>&1; then
        log_info "FastAPI is healthy"
    else
        log_error "FastAPI is not healthy"
        return 1
    fi
    
    # Проверка MQTT
    if docker-compose exec -T mosquitto mosquitto -h localhost > /dev/null 2>&1; then
        log_info "MQTT is healthy"
    else
        log_error "MQTT is not healthy"
        return 1
    fi
    
    # Проверка InfluxDB
    if curl -f http://localhost:8086/ping > /dev/null 2>&1; then
        log_info "InfluxDB is healthy"
    else
        log_error "InfluxDB is not healthy"
        return 1
    fi
    
    log_info "All services are healthy"
}

# Настройка автоматического запуска
setup_autostart() {
    log_info "Setting up autostart..."
    
    # Создание systemd сервиса
    cat > /etc/systemd/system/sunlink.service << EOF
[Unit]
Description=SunLink Service
After=docker.service
Requires=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=$(pwd)
ExecStart=$(which docker-compose) up -d
ExecStop=$(which docker-compose) down
TimeoutStartSec=0
KillMode=mixed
KillSignal=SIGTERM
TimeoutStopSec=120

[Install]
WantedBy=multi-user.target
EOF
    
    # Включение сервиса
    systemctl daemon-reload
    systemctl enable sunlink
    
    log_info "Autostart configured successfully"
}

# Настройка ротации логов
setup_logrotate() {
    log_info "Setting up log rotation..."
    
    cat > /etc/logrotate.d/sunlink << EOF
$(pwd)/logs/*.log {
    daily
    missingok
    rotate 7
    compress
    delaycompress
    notifempty
    create 644 root root
    postrotate
        docker-compose restart fastapi-app
    endscript
}
EOF
    
    log_info "Log rotation configured successfully"
}

# Очистка
cleanup() {
    log_info "Cleaning up..."
    
    # Удаление старых образов
    docker image prune -f
    
    # Удаление неиспользуемых контейнеров
    docker container prune -f
    
    # Удаление неиспользуемых сетей
    docker network prune -f
    
    log_info "Cleanup completed"
}

# Основная функция
main() {
    log_info "Starting SunLink deployment..."
    
    # Проверка прав
    check_permissions
    
    # Проверка зависимостей
    check_dependencies
    
    # Создание директорий
    create_directories
    
    # Генерация SSL сертификатов
    generate_ssl_certificates
    
    # Копирование конфигурационных файлов
    copy_config_files
    
    # Настройка прав
    setup_permissions
    
    # Проверка Docker Compose
    check_docker_compose
    
    # Остановка существующих сервисов
    stop_services
    
    # Сборка образов
    build_images
    
    # Запуск сервисов
    start_services
    
    # Проверка здоровья сервисов
    if check_health; then
        # Настройка автозапуска
        setup_autostart
        
        # Настройка ротации логов
        setup_logrotate
        
        # Очистка
        cleanup
        
        log_info "SunLink deployment completed successfully!"
        log_info "Access the system at: https://sunlink.pp.ua"
        log_info "API documentation: https://sunlink.pp.ua/docs"
    else
        log_error "Service health check failed"
        exit 1
    fi
}

# Обработка аргументов
case "${1:-}" in
    "install")
        main
        ;;
    "start")
        docker-compose up -d
        ;;
    "stop")
        docker-compose down
        ;;
    "restart")
        docker-compose restart
        ;;
    "logs")
        docker-compose logs -f
        ;;
    "status")
        docker-compose ps
        ;;
    "update")
        docker-compose pull
        docker-compose up -d
        ;;
    "clean")
        cleanup
        ;;
    *)
        echo "Usage: $0 {install|start|stop|restart|logs|status|update|clean}"
        exit 1
        ;;
esac