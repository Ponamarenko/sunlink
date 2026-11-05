# Розгортання SunPortal Mobile App

Цей посібник описує процес розгортання SunPortal Mobile App на різних платформах.

---

## 🚀 Розгортання на пристроях Android

### Встановлення через ADB
```bash
# Перевірка підключених пристроїв
adb devices

# Копіювання APK на пристрій
adb install sunlink-mobile-v1.0.6.apk

# Перевірка встановлення
adb shell pm list packages | grep sunportal
```

### Встановлення вручну
1. Завантажте APK файл на пристрій
2. Відкрийте файл менеджер
3. Натисніть на файл APK
4. Натисніть "Встановити"
5. Дочекайтеся завершення встановлення

### Перевірка встановлення
```bash
# Перевірка логів
adb logcat | grep SunPortal

# Перевірка використання пам'яті
adb shell dumpsys meminfo com.sunportal.mobile
```

---

## 📦 Розгортання через Google Play Store

### Підготовка до публікації
1. Створіть Google Play Console акаунт
2. Підготуйте APK файл
3. Створіть listing інформацію
4. Завантажьте APK

### Процедура публікації
```bash
# Підписання APK
jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -keystore sunportal-release.keystore app/build/outputs/apk/release/app-release.apk sunportal

# Zipalign APK
zipalign -v 4 app/build/outputs/apk/release/app-release.apk app-release-aligned.apk

# Завантаження в Google Play Console
```

### Вимоги Google Play
- APK підписаний
- Використовує правильні дозволи
- Має опис та скріншоти
- Відповідає політикам Google Play

---

## 🚀 Розгортання через Firebase App Distribution

### Налаштування Firebase
```bash
# Ініціалізація Firebase
firebase init

# Налаштування App Distribution
firebase appdistribution:distribute app/build/outputs/apk/release/app-release.apk --app <your-app-id>
```

### Процедура розгортання
1. Створіть Firebase проект
2. Додайте додаток до Firebase
3. Налаштуйте App Distribution
4. Запустіть команду розгортання

### Керування тестувальниками
```bash
# Додавання тестувальників
firebase appdistribution:add-testers --app <your-app-id> --tester-file testers.csv

# Перегляд тестувальників
firebase appdistribution:list-testers --app <your-app-id>
```

---

## 🌐 Розгортання через веб-сайт

### Створення веб-сторінки
```html
<!-- index.html -->
<!DOCTYPE html>
<html lang="uk">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>SunPortal Mobile App</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .download-btn { background: #00668A; color: white; padding: 10px 20px; text-decoration: none; }
    </style>
</head>
<body>
    <h1>SunPortal Mobile App</h1>
    <p>Завантажте мобільний додаток для моніторингу сонячної енергетики</p>
    <a href="sunlink-mobile-v1.0.6.apk" class="download-btn">Завантажити APK</a>
</body>
</html>
```

### Розміщення файлів
```bash
# Копіювання файлів на сервер
scp sunlink-mobile-v1.0.6.apk user@server:/var/www/sunportal/
scp index.html user@server:/var/www/sunportal/

# Налаштування прав доступу
chmod 644 /var/www/sunportal/*
chown -R www-data:www-data /var/www/sunportal/
```

---

## 🐳 Розгортання через Docker

### Створення Dockerfile
```dockerfile
# Dockerfile
FROM openjdk:11-jdk-slim

WORKDIR /app

# Копіювання APK
COPY sunlink-mobile-v1.0.6.apk /app/sunlink-mobile-v1.0.6.apk

# Налаштування сервера
COPY server.jar /app/server.jar

# Відкриття портів
EXPOSE 8000

# Запуск сервера
CMD ["java", "-jar", "server.jar"]
```

### Збірка та запуск Docker
```bash
# Збірка Docker образу
docker build -t sunportal-mobile .

# Запуск контейнера
docker run -d -p 8000:8000 --name sunportal-app sunportal-mobile

# Перевірка роботи
docker logs sunportal-app
```

---

## 🔧 Розгортання на сервері

### Налаштування сервера
```bash
# Оновлення системи
sudo apt update && sudo apt upgrade

# Встановлення необхідних пакетів
sudo apt install -y openjdk-11-jdk nginx

# Створення директорії для додатку
sudo mkdir -p /var/www/sunportal
sudo chown -R $USER:$USER /var/www/sunportal

# Копіювання файлів
cp sunlink-mobile-v1.0.6.apk /var/www/sunportal/
cp index.html /var/www/sunportal/
```

### Налаштування Nginx
```nginx
# /etc/nginx/sites-available/sunportal
server {
    listen 80;
    server_name sunlink.pp.ua;
    
    root /var/www/sunportal;
    index index.html;
    
    location / {
        try_files $uri $uri/ =404;
    }
    
    location /api {
        proxy_pass http://localhost:8000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

### Запуск сервісів
```bash
# Активування Nginx
sudo ln -s /etc/nginx/sites-available/sunportal /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx

# Налаштування автозапуску
sudo systemctl enable nginx
```

---

## 📊 Моніторинг розгортання

### Логування
```bash
# Перегляд логів Nginx
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log

# Перегляд логів додатку
tail -f /var/log/sunportal/app.log
```

### Моніторинг ресурсів
```bash
# Моніторинг використання CPU
top
htop

# Моніторинг використання пам'яті
free -h
vmstat

# Моніторинг мережі
netstat -tuln
ss -tuln
```

### Аналітика
```bash
# Аналіз логів
awk '{print $1}' /var/log/nginx/access.log | sort | uniq -c | sort -nr

# Моніторинг помилок
grep "ERROR" /var/log/nginx/error.log | tail -20
```

---

## 🔒 Безпека розгортання

### HTTPS налаштування
```bash
# Встановлення Let's Encrypt
sudo apt install certbot python3-certbot-nginx

# Отримання сертифіката
sudo certbot --nginx -d sunlink.pp.ua

# Автоматичне оновлення сертифікатів
sudo crontab -e
# 0 12 * * * /usr/bin/certbot renew --quiet
```

### Захист від DDoS
```nginx
# Обмеження кількості запитів
limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;
limit_req zone=api burst=20 nodelay;

# Захист від брутфорсу
limit_req_zone $binary_remote_addr zone=login:10m rate=5r/m;
```

### Захист файлів
```bash
# Налаштування прав доступу
sudo chmod 644 /var/www/sunportal/*
sudo chmod 755 /var/www/sunportal/

# Відключення виконання PHP
sudo nano /etc/nginx/sites-available/sunportal
# add: location ~ \.php$ { return 404; }
```

---

## 🔄 Оновлення розгортання

### Оновлення додатку
```bash
# Завантаження нової версії
wget https://github.com/Ponamarenko/SunPortal/releases/download/v1.0.7/sunlink-mobile-v1.0.7.apk

# Заміна файлу
sudo cp sunlink-mobile-v1.0.7.apk /var/www/sunportal/sunlink-mobile-v1.0.6.apk

# Оновлення Docker образу
docker build -t sunportal-mobile:v1.0.7 .
docker tag sunportal-mobile:v1.0.7 sunportal-mobile:latest
docker-compose up -d
```

### Відкат оновлення
```bash
# Відкат до попередньої версії
docker tag sunportal-mobile:v1.0.6 sunportal-mobile:latest
docker-compose up -d

# Перевірка роботи
docker logs sunportal-app
```

---

## 📈 Масштабування

### Горизонтальне масштабування
```yaml
# docker-compose.yml
version: '3.8'
services:
  sunportal-app:
    image: sunportal-mobile:latest
    deploy:
      replicas: 3
    ports:
      - "8000:8000"
    environment:
      - ENVIRONMENT=production
      - DATABASE_URL=postgres://user:pass@db:5432/sunportal
    depends_on:
      - db
      - redis

  db:
    image: postgres:13
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:6-alpine

volumes:
  postgres_data:
```

### Балансування навантаження
```nginx
# /etc/nginx/nginx.conf
upstream sunportal_backend {
    server 192.168.1.10:8000;
    server 192.168.1.11:8000;
    server 192.168.1.12:8000;
}

server {
    location / {
        proxy_pass http://sunportal_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

---

## 📞 Контакти

- **GitHub**: [Створити issue](https://github.com/Ponamarenko/SunPortal/issues)
- **Email**: [ponamarenko@example.com](mailto:ponamarenko@example.com)
- **Discord**: [SunPortal Server](http://sunlink.pp.ua:8000)

---

*Останнє оновлення: 2026-11-04*