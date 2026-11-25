# Stage 59 — Original house weather animation on Home

Статус: PASS (код/тесты/сборка). APK/Release не публикуются до приёмки.

## 1. Изученные ресурсы
- `Icon/Дома.jpg` — эталон: пять фотореалистичных вариантов дома пользователя (солнце/облака/дождь/снег/туман).
- `Icon/house_weather_*.json`, `house_weather_all_in_one.json`, `solar_house_animation.json` — Lottie 5.7.4, 400×400, 60fps, 180 кадров.
- `Icon/battery_charge_discharge.json`, `Icon/deye_12kw_inverter.json` — проверены, не интегрируются.

## 2. Результат сравнения
Lottie-файлы содержат **упрощённый векторный дом**, геометрия/фасад/окна/панели которого не совпадают с фото `Icon/Дома.jpg`. Использовать их как замену дома запрещено. Принято решение: оставить оригинальный дом из JPG (crop верхнего левого варианта) и анимировать только погодные эффекты.

## 3. Реализация
- JPG добавлен в `res/drawable/house_reference.jpg`.
- Из Lottie-json выделены только слои `Weather_*` (`weather_effect_*.json`), дом/панели/окна/основание удалены.
- Home отображает crop оригинального дома под схемой потока и накладывает анимированный Lottie-погодный слой.
- Пять состояний: SUN/CLOUDS/RAIN/SNOW/FOG через preview-параметр `HomeScreen(weather=...)`; production по умолчанию SUN.
- Реального источника погоды в текущей телеметрии нет — в production используется безопасное SUN; погода не выводится из PV.

## 4. Ограничения
Paparazzi snapshot подтверждает статический кадр и загрузку Lottie-композиции, но не доказывает непрерывное движение. Живое воспроизведение `IterateForever` запрограммировано, но видео/визуальная проверка движения на устройстве не выполнялась.

Остальные 11 экранов, backend, REST/WS, Modbus, poller, сеть и TLS не изменяются.


## 5. Проверки
- Unit tests: 276/276 PASS (включая 5Papaprazzi weather renders).
- `lintDebug`, `assembleDebug`, `assembleDebugAndroidTest`: PASS.
- Paparazzi PNG: `home_sun`, `home_clouds`, `home_rain`, `home_snow`, `home_fog`.
