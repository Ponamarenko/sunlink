#!/bin/bash

# Скрипт развертывания Deye Gateway

set -e

echo "🚀 Запуск развертывания Deye Gateway..."

# Проверка зависимостей
if ! command -v python3 &> /dev/null; then
    echo "❌ Python3 не найден"
    exit 1
fi

if ! command -v pip3 &> /dev/null; then
    echo "❌ pip3 не найден"
    exit 1
fi

# Создание виртуального окружения
echo "📦 Создание виртуального окружения..."
python3 -m venv venv
source venv/bin/activate

# Установка зависимостей
echo "📥 Установка зависимостей..."
pip install -r requirements.txt

# Проверка конфигурации
echo "⚙️ Проверка конфигурации..."
if [ ! -f config.yaml ]; then
    echo "❌ Файл config.yaml не найден"
    exit 1
fi

# Создание директорий
echo "📁 Создание директорий..."
mkdir -p logs
mkdir -p data

# Копирование веб-интерфейса
echo "🌐 Копирование веб-интерфейса..."
cp web_interface.html /var/www/html/deye-gateway/ 2>/dev/null || {
    echo "⚠️  Не удалось скопировать веб-интерфейс. Запустите вручную:"
    echo "   sudo cp web_interface.html /var/www/html/deye-gateway/"
}

# Настройка прав
echo "🔐 Настройка прав..."
chmod +x deye_gateway.py
chmod +x deploy.sh

# Запуск теста соединения
echo "🔗 Тестирование соединения с инвертором..."
python3 -c "
import asyncio
import sys
sys.path.append('.')
from deye_gateway import DeyeGateway

async def test_connection():
    gateway = DeyeGateway({
        'modbus': {'host': '192.168.1.100', 'port': 502, 'timeout': 5, 'slave_id': 1},
        'server': {'max_concurrent_writes': 2, 'write_cooldown': 5}
    })
    
    if await gateway.connect():
        print('✅ Соединение с инвертором установлено')
        await gateway.disconnect()
    else:
        print('❌ Не удалось подключиться к инвертору')
        print('   Проверьте IP адрес в config.yaml')
        print('   Убедитесь, что NA111-E подключен и работает')

asyncio.run(test_connection())
"

if [ $? -eq 0 ]; then
    echo "✅ Тест соединения пройден"
else
    echo "❌ Тест соединения не пройден"
    exit 1
fi

echo ""
echo "🎉 Развертывание завершено!"
echo ""
echo "📋 Следующие шаги:"
echo "1. Отредактируйте config.yaml с вашими настройками"
echo "2. Запустите сервер:"
echo "   source venv/bin/activate"
echo "   python deye_gateway.py"
echo ""
echo "🌐 Веб-интерфейс доступен по адресу:"
echo "   http://localhost:8000"
echo ""
echo "📊 API документация:"
echo "   http://localhost:8000/docs"
echo ""
echo "🔧 Docker развертывание:"
echo "   docker build -t deye-gateway ."
echo "   docker run -p 8000:8000 deye-gateway"