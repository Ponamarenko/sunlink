#!/bin/bash

# Скрипт для створення GitHub Release для SunPortal Mobile App v1.0.6

set -e

# Налаштування
REPO_URL="https://github.com/Ponamarenko/SunLink.git"
TAG_NAME="v1.0.7"
RELEASE_TITLE="SunLink Mobile App v1.0.7"
RELEASE_NOTES_FILE="RELEASE_NOTES.md"

# Перевірка наявності необхідних файлів
echo "🔍 Перевірка файлів для релізу..."

if [ ! -f "sunlink-mobile-v1.0.6.apk" ]; then
    echo "❌ APK файл не знайдено!"
    exit 1
fi

if [ ! -f "$RELEASE_NOTES_FILE" ]; then
    echo "❌ Файл нотаток релізу не знайдено!"
    exit 1
fi

# Створення директорії для релізу
echo "📁 Створення директорії для релізу..."
mkdir -p release-v1.0.6

# Копіювання файлів
echo "📋 Копіювання файлів для релізу..."
cp sunlink-mobile-v1.0.6.apk release-v1.0.6/
cp README.md release-v1.0.6/
cp RELEASE_NOTES.md release-v1.0.6/
cp INSTALLATION.md release-v1.0.6/
cp ARCHITECTURE.md release-v1.0.6/
cp API.md release-v1.0.6/
cp CONTRIBUTING.md release-v1.0.6/
cp CODE_OF_CONDUCT.md release-v1.0.6/
cp SECURITY.md release-v1.0.6/
cp CHANGELOG.md release-v1.0.6/
cp LICENSE release-v1.0.6/

# Копіювання документації
echo "📚 Копіювання документації..."
mkdir -p release-v1.0.6/docs
cp docs/DEVELOPMENT.md release-v1.0.6/docs/
cp docs/TESTING.md release-v1.0.6/docs/
cp docs/DEPLOYMENT.md release-v1.0.6/docs/

# Створення ZIP архіву
echo "📦 Створення ZIP архіву..."
cd release-v1.0.6
zip -r ../sunportal-mobile-v1.0.6.zip .
cd ..

# Перевірка створених файлів
echo "✅ Створено наступні файли:"
echo "  - sunlink-mobile-v1.0.6.apk"
echo "  - sunportal-mobile-v1.0.6.zip"
echo "  - release-v1.0.6/ (директорія з усіма файлами)"

# Створення Git тега
echo "🏷️ Створення Git тега..."
git tag -a "$TAG_NAME" -m "SunPortal Mobile App v1.0.6 - Complete release with Material Design 3"

# Відправка тега на GitHub
echo "📤 Відправка тега на GitHub..."
git push origin "$TAG_NAME"

echo ""
echo "🎉 Підготовка до релізу завершена!"
echo ""
echo "📝 Наступні кроки:"
echo "1. Перейдіть до GitHub: $REPO_URL"
echo "2. Натисніть на 'Releases'"
echo "3. Натисніть 'Create a new release'"
echo "4. Виберіть тег: $TAG_NAME"
echo "5. Заповніть назву: $RELEASE_TITLE"
echo "6. Вставте вміст файлу: $RELEASE_NOTES_FILE"
echo "7. Завантажте всі файли з директорії release-v1.0.6/"
echo "8. Натисніть 'Publish release'"

echo ""
echo "📦 Файли для завантаження:"
echo "  - sunlink-mobile-v1.0.6.apk"
echo "  - README.md"
echo "  - RELEASE_NOTES.md"
echo "  - INSTALLATION.md"
echo "  - ARCHITECTURE.md"
echo "  - API.md"
echo "  - CONTRIBUTING.md"
echo "  - CODE_OF_CONDUCT.md"
echo "  - SECURITY.md"
echo "  - CHANGELOG.md"
echo "  - LICENSE"
echo "  - docs/DEVELOPMENT.md"
echo "  - docs/TESTING.md"
echo "  - docs/DEPLOYMENT.md"

echo ""
echo "🔗 Посилання на реліз буде створено автоматично після публікації."
echo "📞 У разі питань звертайтеся до GitHub Issues."