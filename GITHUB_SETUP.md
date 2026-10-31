# GitHub Setup Instructions

## 🚀 Создание репозитория на GitHub

### 1. Создайте репозиторий на GitHub

1. Зайдите на [GitHub](https://github.com)
2. Нажмите "New repository"
3. Заполните поля:
   - **Repository name**: `deye-gateway-server`
   - **Description**: "Полнофункциональный сервер для управления инверторами Deye с поддержкой Modbus RTU/TCP, real-time мониторингом и мобильным приложением"
   - **Public/Private**: Выберите Public
   - **Add README**: Не ставьте галочку (у нас уже есть)
   - **Add .gitignore**: Выберите Python
   - **Add license**: Выберите MIT
4. Нажмите "Create repository"

### 2. Свяжите локальный репозиторий с GitHub

После создания репозитория GitHub выполните следующие команды:

```bash
# Добавьте удаленный репозиторий
git remote add origin https://github.com/ВАШ-ИМЯ-ПОЛЬЗОВАТЕЛЯ/deye-gateway-server.git

# Проверьте настройки
git remote -v

# Сделайте первую отправку
git push -u origin master
```

### 3. Структура проекта

Проект содержит:

#### 🖥️ Серверная часть
- `deye_gateway.py` - Основной сервер на FastAPI
- `sunportal_integration.py` - Интеграция с SunPortal
- `config.yaml` - Конфигурация сервера
- `web_interface.html` - Веб-интерфейс
- `requirements.txt` - Зависимости
- `Dockerfile` - Docker контейнеризация

#### 📱 Мобильное приложение
- `mobile_app/` - Полная структура Android приложения
- `test_server_connection.py` - Тестирование сервера

#### 📄 Документация
- `README.md` - Основная документация
- `DEVELOPMENT_LOG.md` - Лог разработки
- `MIGRATION_REPORT.md` - Отчет о миграции
- `MOBILE_APP_REPORT.md` - Отчет о мобильном приложении
- `GITHUB_SETUP.md` - Инструкции по GitHub

### 4. Команды для работы с GitHub

```bash
# Посмотреть историю коммитов
git log --oneline

# Посмотреть статус
git status

# Добавить изменения
git add .

# Сделать коммит
git commit -m "Описание изменений"

# Отправить на GitHub
git push origin master

# Создать новую ветку
git checkout -b feature/new-feature

# Отправить ветку на GitHub
git push origin feature/new-feature

# Создать Pull Request
# Зайдите на GitHub и создайте PR из ветки feature/new-feature в master
```

### 5. GitHub Pages (опционально)

Для документации можно включить GitHub Pages:

1. Зайдите в Settings репозитория
2. Найдите раздел "GitHub Pages"
3. Выберите ветку `master` и папку `/ (root)`
4. Нажмите "Save"

Документация будет доступна по адресу: `https://ВАШ-ИМЯ-ПОЛЬЗОВАТЕЛЯ.github.io/deye-gateway-server`

### 6. Issues и Discussions

Используйте GitHub для:
- **Issues** - Отслеживание багов и задач
- **Discussions** - Обсуждение функционала
- **Pull Requests** - Внесение изменений

### 7. Автоматическое обновление

Для автоматического обновления документации можно добавить GitHub Actions:

```yaml
# .github/workflows/docs.yml
name: Deploy Documentation

on:
  push:
    branches: [ master ]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v2
    - name: Setup Python
      uses: actions/setup-python@v2
      with:
        python-version: '3.11'
    - name: Install dependencies
      run: |
        pip install mkdocs-material mkdocs
    - name: Build documentation
      run: |
        mkdocs build
    - name: Deploy to GitHub Pages
      uses: peaceiris/actions-gh-pages@v3
      with:
        github_token: ${{ secrets.GITHUB_TOKEN }}
        publish_dir: ./site
```

---

**Готово!** Теперь ваш проект готов к загрузке на GitHub и совместной разработке.