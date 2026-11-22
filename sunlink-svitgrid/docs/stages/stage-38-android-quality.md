# Stage 38 — Android Quality, UI Testing & Localization

**Статус: PASS (разработка, unit/lint/сборка, Release); Compose UI tests — COMPILED, NOT RUN (нет устройства/эмулятора). Production не изменялся.**

## 1. Git

Initial HEAD `13ff4478ebe6c8b27e0981a1219522aa5cc8f471` (= `origin/main`). Stage 38 коммитится отдельно; final HEAD — в финальном ответе. Working tree до начала — только untracked мастер-ТЗ.

## 2. Что сделано

### Compose UI testing
- Добавлены зависимости `ui-test-junit4:1.6.8`, `androidx.test.ext:junit`, `espresso-core`; `ui-test-manifest` уже был.
- `SvitGridApp` теперь принимает `repository` для инъекции fake repository в тестах.
- Создан `src/androidTest/.../Stage38ComposeTest.kt` с fake repository и тестами:
  - Dashboard показывает SOC 82 %, не показывает «0 %», блок быстрого доступа;
  - Battery/PV/Grid/Load/Energy Flow рендерятся с отсутствующими значениями без crash;
  - drawer открывается и переключает на Battery;
  - нижняя навигация переключает на Statistics.
- Тесты **скомпилированы** (`assembleDebugAndroidTest` PASS); запуск `connectedDebugAndroidTest` завершился ошибкой `No online devices found` — устройств и AVD в среде нет. Это честно зафиксировано как NOT RUN.

### Localization
- В resources перенесено ~80 пользовательских строк из SvitGridApp, Diagnostics, History, InverterDetails, Forecast, Debug.
- Исправлены русские остатки: `ОТСУТСТВУЄ → відсутній`, `Батерея → Акумулятор`, `Бутова навантаження → Навантаження`.
- Удалены дубликаты ключей uk/en; добавлен автотест паритета и отсутствия пустых переводов.
- Экраны Battery/PV/Grid/Load/Energy Flow и названия периодов локализованы.

### Loading / error / stale / null
- `MainViewModel` получил `fullTelemetryError: StateFlow<String?>`, устанавливается при ошибке загрузки полной телеметрии, сбрасывается при успехе.
- `CategoryScaffold` и `TelemetryScreen` показывают ошибку и кнопку повтора; при отсутствии данных — «Немає даних», при stale — предупреждение.
- Нигде не подставляется 0 вместо null (проверено unit-тестами).

### UI/UX и доступность
- Единые карточки через `ValueCard`, цвета/типографика design system Stage 31, адаптивные строки, content descriptions меню, touch targets FilterChip/кнопок.
- Diagnostics показывает REST/WS/health/version/source; не показывает зелёный без реальной проверки.

## 3. Тесты

| Проверка | Результат |
|---|---|
| Backend `unittest discover` | **330 PASS** |
| Android `testDebugUnitTest` | **130 PASS** (4 новых Stage 38) |
| `assembleDebugAndroidTest` (компиляция UI-тестов) | **PASS** |
| `connectedDebugAndroidTest` | **NOT RUN — No online devices found** |
| Android lint | **PASS** |
| `assembleDebug` | **PASS** |

## 4. APK

- Package `com.sunlink.svitgrid`, versionName **2.38.0**, versionCode **59**.
- Размер **13 282 318 B**, SHA-256 `8f7232e6b4e5b5c18353193b17f894d9e6b4ac5966fb2124a9763e8d7757cbf0`.
- Debug build, без production signing, prerelease.

## 5. Production safety

Ничего не изменялось: active map `{184}`, poller, endpoint, `.env`, systemd, SQLite, TLS/DNS/NAT/firewall, Ebyte, инвертор. Modbus read/write — NOT RUN. Deployment — NOT RUN.

## 6. Ограничения

Compose UI tests написаны и компилируются, но не исполнялись на устройстве. Часть старых технических надписей (format debug) остаётся намеренно. Визуальная проверка на телефоне выполняется пользователем через Release APK.
