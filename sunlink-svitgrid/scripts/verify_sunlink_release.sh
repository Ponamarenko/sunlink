#!/usr/bin/env bash
# SunLink — release verification (Stage 13).
#
# Проверяет, что репозиторий собирается и проходит тесты как единая система:
#   backend unit tests -> Android unit tests -> debug APK -> метаданные APK.
#
# Безопасность: скрипт не читает production .env, не обращается к инвертору,
# Ebyte, Home Assistant, Redis или production URL. Использует только локальный
# репозиторий и установленный Android SDK.
set -euo pipefail

GIT_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
# Android-проект может лежать в подкаталоге git-репозитория (здесь: sunlink-svitgrid/).
if [[ -f "$GIT_ROOT/gradlew" ]]; then
  ROOT="$GIT_ROOT"
elif [[ -f "$GIT_ROOT/sunlink-svitgrid/gradlew" ]]; then
  ROOT="$GIT_ROOT/sunlink-svitgrid"
else
  ROOT="$(find "$GIT_ROOT" -maxdepth 3 -name gradlew -type f -print -quit 2>/dev/null | xargs -r dirname)"
fi
[[ -n "$ROOT" && -f "$ROOT/gradlew" ]] || { echo "FAIL: Android проект (gradlew) не найден" >&2; exit 1; }
cd "$ROOT"

STEP=0
log()  { printf '\n[%d] %s\n' "$STEP" "$*"; }
next() { STEP=$((STEP + 1)); }
die()  { printf 'FAIL: %s\n' "$*" >&2; exit 1; }

log "SunLink release verification"
echo "GIT_ROOT=$GIT_ROOT"
echo "PROJECT=$ROOT"

next
log "Проверка необходимых компонентов"
for f in \
  "gradlew" \
  "app/build.gradle.kts" \
  "app/src/main/AndroidManifest.xml" \
  "sunlink-svitgrid/server/deye_gateway.py" \
  "sunlink-svitgrid/server/demo_backend.py" \
  "sunlink-svitgrid/server/tests/test_stage12_contract.py"; do
  [[ -f "$ROOT/$f" ]] || die "отсутствует файл: $f"
  echo "  OK  $f"
done
command -v python3 >/dev/null || die "python3 не найден"
command -v java >/dev/null || die "java не найден (нужен для Gradle)"

next
log "Backend unit tests"
BACKEND_LOG=/tmp/sunlink-release-backend-tests.log
( cd "$ROOT/sunlink-svitgrid/server" && python3 -m unittest discover -s tests ) \
  >"$BACKEND_LOG" 2>&1 || { tail -40 "$BACKEND_LOG" >&2; die "backend tests failures"; }
BACKEND_COUNT="$(grep -oE 'Ran [0-9]+ tests' "$BACKEND_LOG" | tail -1 | grep -oE '[0-9]+' || echo '?')"
tail -3 "$BACKEND_LOG"

next
log "Android unit tests"
ANDROID_LOG=/tmp/sunlink-release-android-tests.log
./gradlew :app:testDebug --console=plain >"$ANDROID_LOG" 2>&1 \
  || { tail -60 "$ANDROID_LOG" >&2; die "android tests failures"; }
tail -3 "$ANDROID_LOG"
ANDROID_COUNT="$(python3 - "$ROOT" <<'PY' 2>/dev/null || echo '?'
import glob, re, sys
root = sys.argv[1]
total = 0
for path in glob.glob(root + "/app/build/test-results/testDebugUnitTest/TEST-*.xml"):
    with open(path, encoding="utf-8") as f:
        m = re.search(r'<testsuite[^>]*\btests="(\d+)"', f.read())
    if m:
        total += int(m.group(1))
print(total)
PY
)"

next
log "Сборка debug APK"
BUILD_LOG=/tmp/sunlink-release-build.log
./gradlew assembleDebug --console=plain >"$BUILD_LOG" 2>&1 \
  || { tail -60 "$BUILD_LOG" >&2; die "assembleDebug failure"; }
tail -3 "$BUILD_LOG"

APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
next
log "Проверка APK"
[[ -f "$APK" ]] || die "APK не найден: $APK"
BYTES="$(stat -c '%s' "$APK")"
MB="$(python3 -c "print(f'{$BYTES / (1024*1024):.1f}')")"
echo "  APK      : ${APK#$ROOT/}"
echo "  size     : $BYTES bytes (~$MB MiB)"
# sanity-диапазон: проверяем только, что APK реалистичного размера (ориентир ~13 MB)
python3 - "$BYTES" <<'PY' || die "неправдоподобный размер APK"
import sys
size = int(sys.argv[1])
sys.exit(0 if 1_000_000 <= size <= 200_000_000 else 1)
PY

next
log "Метаданные APK"
ANALYZER="$(command -v apkanalyzer || true)"
if [[ -n "$ANALYZER" ]]; then
  PKG="$(apkanalyzer manifest application-id "$APK")"
  VNAME="$(apkanalyzer manifest version-name "$APK")"
  VCODE="$(apkanalyzer manifest version-code "$APK")"
else
  echo "apkanalyzer недоступен — безопасная альтернатива: output-metadata.json + aapt"
  META="$ROOT/app/build/outputs/apk/debug/output-metadata.json"
  [[ -f "$META" ]] || die "нет output-metadata.json и нет apkanalyzer"
  read -r PKG VNAME VCODE < <(python3 - "$META" <<'PY'
import json, sys
d = json.load(open(sys.argv[1], encoding="utf-8"))
e = d["elements"][0]
print(d["applicationId"], e["versionName"], e["versionCode"])
PY
  )
fi

echo "  package  : $PKG"
echo "  version  : $VNAME ($VCODE)"

[[ "$PKG" == "com.sunlink.svitgrid" ]] || die "неверный package: $PKG"
[[ -n "$VNAME" ]] || die "пустой versionName"
[[ "$VCODE" =~ ^[0-9]+$ ]] && (( VCODE > 0 )) || die "неверный versionCode: $VCODE"

# Сверка с output-metadata.json (фактические метаданные сборки)
META="$ROOT/app/build/outputs/apk/debug/output-metadata.json"
if [[ -f "$META" ]]; then
  read -r META_PKG META_NAME META_CODE < <(python3 - "$META" <<'PY'
import json, sys
d = json.load(open(sys.argv[1], encoding="utf-8"))
e = d["elements"][0]
print(d["applicationId"], e["versionName"], e["versionCode"])
PY
  )
  [[ "$META_PKG" == "$PKG" ]] || die "package не совпадает с output-metadata.json"
  [[ "$META_NAME" == "$VNAME" ]] || die "versionName не совпадает с output-metadata.json"
  [[ "$META_CODE" == "$VCODE" ]] || die "versionCode не совпадает с output-metadata.json"
  echo "  metadata : совпадает с output-metadata.json"
fi

# Опциональная проверка конкретной версии (только если задана)
if [[ -n "${EXPECT_VERSION_NAME:-}" ]]; then
  [[ "$VNAME" == "$EXPECT_VERSION_NAME" ]] || die "versionName != EXPECT_VERSION_NAME ($EXPECT_VERSION_NAME)"
fi
if [[ -n "${EXPECT_VERSION_CODE:-}" ]]; then
  [[ "$VCODE" == "$EXPECT_VERSION_CODE" ]] || die "versionCode != EXPECT_VERSION_CODE ($EXPECT_VERSION_CODE)"
fi

printf '\n=== SUMMARY ===\n'
echo "backend tests : $BACKEND_COUNT PASS"
echo "android tests : $ANDROID_COUNT PASS"
echo "APK           : $PKG $VNAME ($VCODE), ~$MB MiB"
echo "RELEASE VERIFICATION: PASS"
exit 0
