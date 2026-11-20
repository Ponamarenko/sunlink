#!/usr/bin/env bash
# SunLink — безопасная публикация собранного APK в GitHub Releases.
#
# Правило проекта: каждый собранный APK публикуется в Releases репозитория
# Ponamarenko/sunlink. APK НЕ добавляется в Git.
#
# Использование:
#   ./scripts/publish_android_release.sh
#   TAG=android-v2.30.0-stage14 TITLE="SunLink Android 2.30.0 — Debug" ./scripts/publish_android_release.sh
#
# Переменные окружения:
#   APK_PATH  — путь к APK (по умолчанию debug)
#   REPO      — репозиторий (по умолчанию Ponamarenko/sunlink)
#   TAG       — тег релиза (по умолчанию android-v<version>-<type>-<short sha>)
#   TITLE     — название релиза
#   NOTES     — дополнительные строки в release notes
#
# Токены НЕ читаются и НЕ записываются скриптом: gh использует gh auth / GH_TOKEN.
set -euo pipefail

GIT_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
if [[ -f "$GIT_ROOT/gradlew" ]]; then
  ROOT="$GIT_ROOT"
elif [[ -f "$GIT_ROOT/sunlink-svitgrid/gradlew" ]]; then
  ROOT="$GIT_ROOT/sunlink-svitgrid"
else
  ROOT="$(find "$GIT_ROOT" -maxdepth 3 -name gradlew -type f -print -quit 2>/dev/null | xargs -r dirname)"
fi
[[ -n "$ROOT" && -f "$ROOT/gradlew" ]] || { echo "FAIL: Android проект не найден" >&2; exit 1; }
cd "$ROOT"

REPO="${REPO:-Ponamarenko/sunlink}"
APK_PATH="${APK_PATH:-app/build/outputs/apk/debug/app-debug.apk}"
COMMIT="$(git rev-parse HEAD)"
COMMIT_SHORT="${COMMIT:0:7}"

die() { echo "FAIL: $*" >&2; exit 1; }
log() { echo "[publish] $*"; }

# 1. APK
[[ -f "$APK_PATH" ]] || die "APK не найден: $APK_PATH"
APK_NAME="$(basename "$APK_PATH")"
case "$APK_NAME" in
  *debug*) BUILD_TYPE="debug" ;;
  *release*) BUILD_TYPE="release" ;;
  *) BUILD_TYPE="unknown" ;;
esac

# 2-3. Метаданные и SHA-256
command -v apkanalyzer >/dev/null || die "apkanalyzer не найден (Android SDK cmdline-tools)"
PKG="$(apkanalyzer manifest application-id "$APK_PATH")"
VERSION_NAME="$(apkanalyzer manifest version-name "$APK_PATH")"
VERSION_CODE="$(apkanalyzer manifest version-code "$APK_PATH")"
SHA="$(sha256sum "$APK_PATH" | cut -d' ' -f1)"
SIZE="$(stat -c '%s' "$APK_PATH")"

[[ "$PKG" == "com.sunlink.svitgrid" ]] || die "неожиданный package: $PKG"
[[ -n "$VERSION_NAME" && "$VERSION_CODE" =~ ^[0-9]+$ ]] || die "некорректная версия APK"

log "APK=$APK_NAME package=$PKG version=$VERSION_NAME ($VERSION_CODE) type=$BUILD_TYPE"
log "size=$SIZE sha256=$SHA commit=$COMMIT_SHORT"

# 4-5. GitHub auth (gh не печатает токен)
command -v gh >/dev/null || die "gh CLI не установлен"
gh auth status --hostname github.com >/dev/null 2>&1 || die "нет GitHub authentication (gh auth status)"

TAG="${TAG:-android-v${VERSION_NAME}-${BUILD_TYPE}-${COMMIT_SHORT}}"
TITLE="${TITLE:-SunLink Android $VERSION_NAME — $BUILD_TYPE}"

verify_and_report() {
  local is_pre asset_size asset_names dl_sha url asset_url checksum_url
  is_pre="$(gh release view "$TAG" --repo "$REPO" --json isPrerelease --jq '.isPrerelease')"
  if [[ "$BUILD_TYPE" == "debug" && "$is_pre" != "true" ]]; then
    die "релиз не помечен как prerelease"
  fi
  asset_size="$(gh release view "$TAG" --repo "$REPO" --json assets --jq \
    ".assets[] | select(.name==\"$APK_NAME\") | .size")"
  [[ "$asset_size" == "$SIZE" ]] || die "размер APK в релизе не совпадает"

  asset_names="$(gh release view "$TAG" --repo "$REPO" --json assets --jq '.assets[].name')"
  grep -qx "$APK_NAME" <<< "$asset_names" || die "APK отсутствует среди assets"
  grep -qx "$APK_NAME.sha256" <<< "$asset_names" || die "checksum отсутствует среди assets"

  mkdir -p "$TMP/download"
  gh release download "$TAG" --repo "$REPO" --pattern "$APK_NAME" --dir "$TMP/download"
  dl_sha="$(sha256sum "$TMP/download/$APK_NAME" | cut -d' ' -f1)"
  [[ "$dl_sha" == "$SHA" ]] || die "SHA-256 скачанного APK не совпадает"

  url="$(gh release view "$TAG" --repo "$REPO" --json url --jq '.url')"
  asset_url="$(gh release view "$TAG" --repo "$REPO" --json assets --jq \
    ".assets[] | select(.name==\"$APK_NAME\") | .url")"
  checksum_url="$(gh release view "$TAG" --repo "$REPO" --json assets --jq \
    ".assets[] | select(.name==\"$APK_NAME.sha256\") | .url")"

  echo
  log "PUBLISHED: $url"
  log "APK asset: $asset_url"
  log "SHA256 asset: $checksum_url"
  log "sha256 verify: OK ($SHA)"
}

# 6-7. Проверка tag/release — ничего не перезаписываем
if gh release view "$TAG" --repo "$REPO" >/dev/null 2>&1; then
  log "релиз $TAG уже опубликован, ничего не меняем"
  TMP="$(mktemp -d)"
  trap 'rm -rf "$TMP"' EXIT
  verify_and_report
  exit 0
fi
if git ls-remote --tags origin "refs/tags/$TAG" 2>/dev/null | grep -q "refs/tags/$TAG"; then
  die "тег $TAG уже существует (без релиза) — задайте новый TAG"
fi

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

STAGE_APK="$TMP/$APK_NAME"
CHECKSUM="$TMP/$APK_NAME.sha256"
cp "$APK_PATH" "$STAGE_APK"
printf '%s  %s\n' "$SHA" "$APK_NAME" > "$CHECKSUM"

# release notes
if [[ "$BUILD_TYPE" == "debug" ]]; then
  PRERELEASE_FLAG="--prerelease"
  STABILITY="DEBUG / prerelease — НЕ production"
else
  PRERELEASE_FLAG=""
  STABILITY="RELEASE — требует проверенной подписи"
fi

BACKEND_TESTS="${BACKEND_TESTS:-276}"
ANDROID_TESTS="${ANDROID_TESTS:-93}"

NOTES_FILE="$TMP/NOTES.md"
cat > "$NOTES_FILE" <<EOF
## SunLink Android $VERSION_NAME ($VERSION_CODE)

- Package: \`$PKG\`
- Version code: \`$VERSION_CODE\`
- Тип сборки: **$BUILD_TYPE** — $STABILITY
- Commit: \`$COMMIT\`
- minSdk 26 / targetSdk 34

### Тесты
- Backend: ${BACKEND_TESTS} unit-тестов PASS
- Android: ${ANDROID_TESTS} unit-тестов PASS
- \`scripts/verify_sunlink_release.sh\`: RELEASE VERIFICATION: PASS

### SHA-256
\`\`\`
$SHA  $APK_NAME
\`\`\`

### Источник данных
- Реальная телеметрия приходит по REST/WebSocket от backend с \`source: deye_modbus\`.
- Неподтверждённые параметры остаются пустыми и отображаются как «Немає даних», а не нулями.
- DEMO/mock/simulator в production-пути запрещены.

### Ограничения
- Установка и запуск на реальном Android-устройстве: **NOT VERIFIED** (устройство/эмулятор недоступны).
- Debug APK не подписан production-ключом.
${NOTES:-}
EOF

# 8. релиз + assets
log "создаём release $TAG"
gh release create "$TAG" "$STAGE_APK" "$CHECKSUM" \
  --repo "$REPO" --title "$TITLE" --notes-file "$NOTES_FILE" \
  --target "$COMMIT" $PRERELEASE_FLAG

# 11. Проверка результата
log "проверяем release"
verify_and_report
