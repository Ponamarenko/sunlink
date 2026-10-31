#!/bin/bash

# Останавливаем все старые процессы
pkill -f "python3.*server" || true
sleep 2

# Запускаем сервер
echo "🚀 Запускаем SunPortal Server..."
python3 simple_server.py > /tmp/sunportal_server.log 2>&1 &

# Сохраняем PID
SERVER_PID=$!
echo "📝 PID сервера: $SERVER_PID"

# Ждем запуска
sleep 3

# Проверяем статус
if curl -s "http://localhost:8080/api/status" > /dev/null; then
    echo "✅ Сервер успешно запущен"
    echo "🌐 API доступен: http://localhost:8080"
else
    echo "❌ Сервер не запустился"
    echo "📋 Логи:"
    tail -10 /tmp/sunportal_server.log
fi