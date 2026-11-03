#!/bin/bash

# SSH Setup Script for SunLink
# Настройка SSH доступа для удаленного управления

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

# Создание SSH ключей
create_ssh_keys() {
    log_info "Creating SSH keys..."
    
    # Директория для SSH ключей
    SSH_DIR="/root/.ssh"
    mkdir -p "$SSH_DIR"
    
    # Генерация SSH ключей
    ssh-keygen -t ed25519 -f "$SSH_DIR/sunlink_key" -N "" -q
    
    log_info "SSH keys created successfully"
    log_info "Private key: $SSH_DIR/sunlink_key"
    log_info "Public key: $SSH_DIR/sunlink_key.pub"
}

# Настройка SSH сервера
setup_ssh_server() {
    log_info "Setting up SSH server..."
    
    # Резервная копия конфигурации
    cp /etc/ssh/sshd_config /etc/ssh/sshd_config.backup
    
    # Настройка SSH сервера
    cat > /etc/ssh/sshd_config << EOF
# SunLink SSH Configuration
Port 22
Protocol 2
HostKey /etc/ssh/ssh_host_rsa_key
HostKey /etc/ssh/ssh_host_ecdsa_key
HostKey /etc/ssh/ssh_host_ed25519_key
SyslogFacility AUTHPRIV
PermitRootLogin yes
PasswordAuthentication yes
ChallengeResponseAuthentication no
UsePAM yes
X11Forwarding yes
PrintMotd no
PrintLastLog yes
TCPKeepAlive yes
ClientAliveInterval 300
ClientAliveCountMax 3
AcceptEnv LANG LC_*
Subsystem sftp /usr/lib/openssh/sftp-server
EOF
    
    # Перезапуск SSH сервиса
    systemctl restart sshd
    
    log_info "SSH server configured successfully"
}

# Настройка SSH клиента
setup_ssh_client() {
    log_info "Setting up SSH client..."
    
    # Директория для SSH ключей пользователя
    USER_SSH_DIR="/home/suadmin/.ssh"
    mkdir -p "$USER_SSH_DIR"
    chown suadmin:suadmin "$USER_SSH_DIR"
    chmod 700 "$USER_SSH_DIR"
    
    # Копирование SSH ключей
    cp "$SSH_DIR/sunlink_key.pub" "$USER_SSH_DIR/authorized_keys"
    chown suadmin:suadmin "$USER_SSH_DIR/authorized_keys"
    chmod 600 "$USER_SSH_DIR/authorized_keys"
    
    log_info "SSH client configured successfully"
}

# Настройка брандмауэра
setup_firewall() {
    log_info "Setting up firewall..."
    
    # Установка ufw если не установлен
    if ! command -v ufw &> /dev/null; then
        apt-get update
        apt-get install -y ufw
    fi
    
    # Настройка брандмауэра
    ufw --force reset
    ufw default deny incoming
    ufw default allow outgoing
    
    # Разрешение SSH
    ufw allow 22/tcp
    
    # Разрешение веб портов
    ufw allow 80/tcp
    ufw allow 443/tcp
    
    # Разрешение MQTT
    ufw allow 1883/tcp
    
    # Разрешение InfluxDB
    ufw allow 8086/tcp
    
    # Разрешение Prometheus
    ufw allow 9090/tcp
    
    # Включение брандмауэра
    ufw --force enable
    
    log_info "Firewall configured successfully"
}

# Настройка мониторинга
setup_monitoring() {
    log_info "Setting up monitoring..."
    
    # Установка logrotate
    apt-get install -y logrotate
    
    # Настройка ротации логов SSH
    cat > /etc/logrotate.d/sshd << EOF
/var/log/auth.log {
    daily
    missingok
    rotate 7
    compress
    delaycompress
    notifempty
    create 644 root root
    postrotate
        systemctl reload sshd
    endscript
}
EOF
    
    log_info "Monitoring configured successfully"
}

# Создание скриптов для управления
create_management_scripts() {
    log_info "Creating management scripts..."
    
    # Скрипт для подключения
    cat > /usr/local/bin/sunlink-ssh << 'EOF'
#!/bin/bash
# SSH connection script for SunLink

SSH_KEY="/root/.ssh/sunlink_key"
SSH_USER="suadmin"
SSH_HOST="192.168.25.20"

if [ ! -f "$SSH_KEY" ]; then
    echo "SSH key not found. Please run setup-ssh.sh first."
    exit 1
fi

ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no "$SSH_USER@$SSH_HOST"
EOF
    
    chmod +x /usr/local/bin/sunlink-ssh
    
    # Скрипт для копирования файлов
    cat > /usr/local/bin/sunlink-scp << 'EOF'
#!/bin/bash
# SCP script for SunLink

SSH_KEY="/root/.ssh/sunlink_key"
SSH_USER="suadmin"
SSH_HOST="192.168.25.20"

if [ ! -f "$SSH_KEY" ]; then
    echo "SSH key not found. Please run setup-ssh.sh first."
    exit 1
fi

# Использование: sunlink-scp <local_file> <remote_path>
if [ $# -ne 2 ]; then
    echo "Usage: $0 <local_file> <remote_path>"
    exit 1
fi

scp -i "$SSH_KEY" -o StrictHostKeyChecking=no "$1" "$SSH_USER@$SSH_HOST:$2"
EOF
    
    chmod +x /usr/local/bin/sunlink-scp
    
    log_info "Management scripts created successfully"
}

# Основная функция
main() {
    log_info "Starting SSH setup for SunLink..."
    
    # Проверка прав
    check_permissions
    
    # Создание SSH ключей
    create_ssh_keys
    
    # Настройка SSH сервера
    setup_ssh_server
    
    # Настройка SSH клиента
    setup_ssh_client
    
    # Настройка брандмауэра
    setup_firewall
    
    # Настройка мониторинга
    setup_monitoring
    
    # Создание скриптов для управления
    create_management_scripts
    
    log_info "SSH setup completed successfully!"
    log_info "You can now connect using: sunlink-ssh"
    log_info "Or manually: ssh -i /root/.ssh/sunlink_key suadmin@192.168.25.20"
}

# Обработка аргументов
case "${1:-}" in
    "setup")
        main
        ;;
    "key")
        create_ssh_keys
        ;;
    "server")
        setup_ssh_server
        ;;
    "client")
        setup_ssh_client
        ;;
    "firewall")
        setup_firewall
        ;;
    *)
        echo "Usage: $0 {setup|key|server|client|firewall}"
        exit 1
        ;;
esac