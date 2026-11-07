#!/bin/bash

# Скрипт запуска Deye Gateway Server

echo "🚀 Запуск Deye Gateway Server..."

# Остановка предыдущего процесса
pkill -f "deye_gateway.py" 2>/dev/null || true

# Запуск сервера в фоновом режиме
cd /root/opt
source venv/bin/activate
nohup python deye_gateway.py > server.log 2>&1 &

echo "✅ Сервер запущен в фоновом режиме"
echo "📄 Лог файл: /root/opt/server.log"
echo "🌐 API доступен: http://localhost:8000"
echo "📚 Документация: http://localhost:8000/docs"