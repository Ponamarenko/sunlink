<?php
/**
 * ha_collector_v2.php — SUN PORTAL Collector v12
 * Пакетное чтение + расширенная карта регистров + починка battery_temp
 */


use ModbusTcpClient\Composer\Read\ReadRegistersBuilder;
use ModbusTcpClient\Network\BinaryStreamConnection;

$interval = (int)(getenv('COLLECT_INTERVAL') ?: 30);

function logMsg(string $msg): void {
    global $logFile;
    $line = date('Y-m-d H:i:s') . ' ' . $msg . "\n";
    @file_put_contents($logFile, $line, FILE_APPEND);
    echo $line;
}

logMsg('===========================================');
logMsg('SUN PORTAL Collector v12 (PACKET READING)');
try { logEvent('info', 'collector', 'Колектор запущено v12'); } catch (Exception $e) {}
logMsg("Modbus: $modbusHost:$modbusPort (slave $modbusSlave)");

try {
    initSchema();
    initUsers();
} catch (Exception $e) {
    logMsg('INIT ERROR: ' . $e->getMessage());
    sleep(10);
}

$pdo = getDB();
$ha = new HomeAssistant();

// ============================================
// РАСШИРЕННАЯ КАРТА РЕГИСТРОВ (пакетное чтение)
// ============================================
$registers = [
    // Grid (пакет 150-172)
    'grid_frequency'    => [79, 'u16', 0.01, 0],
    'grid_voltage_l1'   => [150, 'u16', 0.1, 0],
    'grid_power'        => [172, 's16', 10, 0],  // grid_ct_power
    'load_power'        => [173, 's16', 1, 0],
    'grid_connected'    => [194, 'u16', 1, 0],
    
    // Battery (пакет 182-194)
    'battery_voltage'   => [183, 'u16', 0.01, 0],
    'battery_soc'       => [184, 'u16', 1, 0],
    'battery_power'     => [190, 's16', 1, 0],
    'battery_current'   => [191, 's16', 0.01, 0],
    'battery_temp'      => [182, 'u16', 0.1, -100], // ИСПРАВЛЕНО: offset -100, не мусор!
    'battery_temp_inverter' => [207, 'u16', 0.1, 0], // AC temperature (новое)
    
    // PV (пакет 109-114)
    'pv1_voltage'       => [109, 'u16', 0.1, 0],
    'pv1_current'       => [110, 'u16', 0.1, 0],
    'pv2_voltage'       => [111, 'u16', 0.1, 0],
    'pv2_current'       => [112, 'u16', 0.1, 0],
    'pv3_voltage'       => [113, 'u16', 0.1, 0],
    'pv3_current'       => [114, 'u16', 0.1, 0],
    'pv1_power'         => [186, 's16', 1, 0],
    'pv2_power'         => [187, 's16', 1, 0],
    'pv3_power'         => [188, 's16', 1, 0],
    
    // Дневные счётчики инвертора (новые!)
    'pv_today'          => [205, 'u16', 0.01, 0],
    'load_today'        => [206, 'u16', 0.01, 0],
    'grid_import_today' => [207, 'u16', 0.01, 0],
    'grid_export_today' => [208, 'u16', 0.01, 0],
    'battery_charge_today' => [209, 'u16', 0.01, 0],
    'battery_discharge_today' => [210, 'u16', 0.01, 0],
    
    // Тотальные счётчики (новые!)
    'pv_total'          => [211, 'u16', 0.01, 0],
    'battery_charge_total' => [212, 'u16', 0.01, 0],
    'battery_discharge_total' => [213, 'u16', 0.01, 0],
    'grid_import_total' => [214, 'u16', 0.01, 0],
    'grid_export_total' => [215, 'u16', 0.01, 0],
    
    // Генератор (новое!)
    'gen_power'         => [216, 's16', 1, 0],
    'gen_connected'     => [217, 'u16', 1, 0],
    
    // Дополнительно (пакет 248-279)
    'tou_mask'          => [248, 'u16', 1, 0],
    'tou_times'         => [250, 'u16', 1, 0, 6], // 6 registers
    'tou_power'         => [256, 'u16', 1, 0, 6],
    'tou_voltage'       => [262, 'u16', 1, 0, 6],
    'tou_soc'           => [268, 'u16', 1, 0, 6],
    'tou_flags'         => [274, 'u16', 1, 0, 6],
];

// ============================================
// ПАКЕТНОЕ ЧТЕНИЕ MODBUS
// ============================================
function readAllRegisters(string $host, int $port, int $slave, array $registers): array {
    $result = [];
    $ok = 0;
    $packets = [];
    
    // Группируем регистры в пакеты по соседним адресам
    $currentPacket = [];
    $lastAddr = null;
    
    foreach ($registers as $field => $reg) {
            $addr = $reg[0]; $type = $reg[1]; $scale = $reg[2]; $offset = $reg[3]; $count = $reg[4] ?? 1;
        if ($lastAddr !== null && $addr > $lastAddr + 1) {
            // Новый пакет
            if (!empty($currentPacket)) {
                $packets[] = $currentPacket;
                $currentPacket = [];
            }
        }
        $currentPacket[] = [$field, $addr, $type, $scale, $offset, $count];
        $lastAddr = $addr + $count - 1;
    }
    if (!empty($currentPacket)) {
        $packets[] = $currentPacket;
    }
    
    logMsg("Modbus packets: " . count($packets));
    
    try {
        $conn = BinaryStreamConnection::getBuilder()
            ->setHost($host)->setPort($port)
            ->setConnectTimeoutSec(5)->setReadTimeoutSec(5)
            ->build();
        
        foreach ($packets as $packet) {
            $builder = ReadRegistersBuilder::newReadHoldingRegisters("tcp://{$host}:{$port}", $slave);
            $packetFields = [];
            
            foreach ($packet as [$field, $addr, $type, $scale, $offset, $count]) {
                $endAddr = $addr + $count - 1;
                switch ($type) {
                    case 'u16': $builder->uint16Range($addr, $endAddr, "v_{$field}"); break;
                    case 's16': $builder->int16Range($addr, $endAddr, "v_{$field}"); break;
                    default:    $builder->uint16Range($addr, $endAddr, "v_{$field}");
                }
                $packetFields[$field] = [$addr, $type, $scale, $offset, $count];
            }
            
            $reqs = $builder->build();
            foreach ($reqs as $req) {
                $data = $conn->connect()->sendAndReceive($req);
                $parsed = $req->parse($data);
                
                foreach ($packetFields as $field => [$addr, $type, $scale, $offset, $count]) {
                    $values = [];
                    for ($i = 0; $i < $count; $i++) {
                        $key = "v_{$field}_{$i}";
                        $v = $parsed[$key] ?? null;
                        if ($v !== null) {
                            if ($type === 's16' && $v > 32767) $v -= 65536;
                            $values[] = round((float)$v * $scale + $offset, 2);
                        } else {
                            $values[] = null;
                        }
                    }
                    $result[$field] = $count === 1 ? $values[0] : $values;
                    if ($values[0] !== null) $ok++;
                }
            }
        }
        
        $conn->close();
    } catch (\Exception $e) {
        return ['__error' => $e->getMessage(), '__ok' => 0];
    }
    
    $result['__ok'] = $ok;
    return $result;
}

$lastAlert = 0;
$modbusFailStreak = 0;

while (true) {
    $start = microtime(true);
    
    $modbusData = readAllRegisters($modbusHost, $modbusPort, $modbusSlave, $registers);
    $ok = $modbusData['__ok'] ?? 0;
    unset($modbusData['__ok'], $modbusData['__error']);
    
    // Считаем PV total
    if ($ok >= 5) {
        $data = $modbusData;
        $data['pv_power'] = ($modbusData['pv1_power'] ?? 0) + ($modbusData['pv2_power'] ?? 0) + ($modbusData['pv3_power'] ?? 0);
        
        // Батарея: теперь берём температуру из Modbus (исправлено!)
        if (isset($modbusData['battery_temp']) && $modbusData['battery_temp'] !== null) {
            $data['battery_temp'] = $modbusData['battery_temp'];
        } else {
            // Фолбэк на HA, если Modbus не дал
            try {
                $bt = $ha->getFloat('sensor.ss_battery_temperature');
                if ($bt !== null && $bt > 5 && $bt < 50) {
                    $data['battery_temp'] = $bt;
                }
            } catch (\Exception $e) {}
        }
        
        $source = 'MODBUS';
        $modbusFailStreak = 0;
    } else {
        $source = 'HA';
        $modbusFailStreak++;
        if ($modbusFailStreak >= 3) {
            logMsg("WATCHDOG: restart modbus-proxy");
            shell_exec('docker restart modbus-proxy 2>&1');
            $modbusFailStreak = 0;
            sleep(15);
        }
    }
    
    // Fallback на HA
    if (empty($data) || ($data['battery_soc'] ?? null) === null) {
        $haData = [];
        foreach (getSensorMap() as $field => $entityId) {
            $haData[$field] = $ha->getFloat($entityId);
        }
        $data = array_merge($haData, $data);
        $source .= '+HA';
    }
    
    logMsg('DEBUG before if: data keys=' . implode(',', array_keys($data)) . ', empty=' . (empty($data) ? 'yes' : 'no'));
    if (!empty($data)) {
        try {
            // Считаем суточные энергии через интегрирование мощностей (если нет счётчиков)
            $dailyEnergy = integrateDaily($pdo, $data);
            $data = array_merge($data, $dailyEnergy);

            saveReading($pdo, $data);
            updateDailySummary($pdo, $data);

            // === Логирование событий ===
            static $lastEvents = [];

            // SOC
            $soc = (float)($data['battery_soc'] ?? 0);
            if ($soc > 0) {
                if ($soc < 20 && (time() - ($lastEvents['low_soc'] ?? 0)) > 1800) {
                    $lastEvents['low_soc'] = time();
                    logEvent('warn', 'battery', 'Низький рівень заряду: ' . $soc . '%');
                }
                if ($soc > 95 && (time() - ($lastEvents['high_soc'] ?? 0)) > 3600) {
                    $lastEvents['high_soc'] = time();
                    logEvent('info', 'battery', 'Батарея заряджена: ' . $soc . '%');
                }
            }

            // Сеть
            $freq = (float)($data['grid_frequency'] ?? 0);
            $gridOk = $freq > 45;
            if (!$gridOk && (time() - ($lastEvents['grid_off'] ?? 0)) > 600) {
                $lastEvents['grid_off'] = time();
                logEvent('warn', 'grid', 'Зникла мережа (частота 0)');
            }
            if ($gridOk && isset($lastEvents['grid_off']) && (time() - ($lastEvents['grid_on'] ?? 0)) > 600) {
                $lastEvents['grid_on'] = time();
                logEvent('info', 'grid', 'Мережа відновилась');
                unset($lastEvents['grid_off']);
            }

            // Обновляем TOU раз в 6 циклов (~1 минута)
            static $touCounter = 0;
            if (++$touCounter >= 6) {
                $touCounter = 0;
                @shell_exec('php /app/tou_reader.php > /dev/null 2>&1 &');
            }

            // Обновляем прогноз раз в 240 циклов (~1 час, цикл ~15 сек)
            static $fcCounter = 0;
            if (++$fcCounter >= 240) {
                $fcCounter = 0;
                @shell_exec('php /app/forecast_fetch.php > /dev/null 2>&1 &');
            }

            // Обновляем погоду раз в 240 циклов (~1 час)
            static $wCounter = 0;
            if (++$wCounter >= 240) {
                $wCounter = 0;
                @shell_exec('php /app/weather_read.php > /dev/null 2>&1 &');
            }
            
            logMsg(sprintf(
                '[%s] PV=%dW SOC=%d%% BATV=%.1fV BATI=%.2fA BAT=%dW Grid=%dW GridV=%.1fV GridC=%d Freq=%.2fHz Load=%dW T=%d°C (ok=%d)',
                $source,
                $data['pv_power'] ?? 0,
                $data['battery_soc'] ?? 0,
                $data['battery_voltage'] ?? 0,
                $data['battery_current'] ?? 0,
                $data['battery_power'] ?? 0,
                $data['grid_power'] ?? 0,
                $data['grid_voltage_l1'] ?? 0,
                $data['grid_connected'] ?? 0,
                $data['grid_frequency'] ?? 0,
                $data['load_power'] ?? 0,
                $data['battery_temp'] ?? 0,
                $ok
            ));
        } catch (PDOException $e) {
            logMsg('DB Write Error: ' . $e->getMessage());
        }
    }
    
    $elapsed = microtime(true) - $start;
    $sleep = max(10, $interval - (int)$elapsed);
    sleep($sleep);
}

function saveReading(PDO $pdo, array $data): void {
    $fields = [
        'ts', 'pv_power', 'pv1_power', 'pv2_power', 'pv3_power',
        'pv1_voltage', 'pv2_voltage', 'pv3_voltage',
        'pv1_current', 'pv2_current', 'pv3_current',
        'load_power', 'grid_power',
        'battery_power', 'battery_soc', 'battery_voltage', 'battery_current', 'battery_temp',
        'battery_remaining_kwh', 'battery_time_remaining',
        'pv_today', 'load_today', 'grid_import_today', 'grid_export_today',
        'battery_charge_today', 'battery_discharge_today',
        'forecast_today', 'forecast_tomorrow', 'forecast_power_now',
        'forecast_peak_today', 'forecast_peak_time',
        'pv_total', 'battery_charge_total', 'battery_discharge_total',
        'gen_power', 'gen_connected',
    ];
    $values = [date('Y-m-d H:i:s')];
    foreach (array_slice($fields, 1) as $f) {
        $values[] = $data[$f] ?? null;
    }
    $placeholders = implode(',', array_fill(0, count($fields), '?'));
    $sql = "INSERT INTO readings (" . implode(',', $fields) . ") VALUES ($placeholders)";
    $pdo->prepare($sql)->execute($values);
}

/**
 * Интегрирование мгновенной мощности (Вт) в суточную энергию (кВт·ч).
 * Хранит накопленные значения в таблице energy_daily.
 * Сбрасывает в 00:00 по Киеву.
 */
function integrateDaily(PDO $pdo, array $data): array {
    logMsg('integrateDaily: CALLED, keys=' . implode(',', array_keys($data)));
    $tz = new DateTimeZone('Europe/Kyiv');
    $now = new DateTime('now', $tz);
    $today = $now->format('Y-m-d');

    // Интервал сбора — примерно 10 секунд (см. $interval в коллекторе)
    // Точный интервал вычислим по времени между вызовами
    static $lastTs = null;
    $dt = 10; // секунд по умолчанию
    if ($lastTs !== null) {
        $dt = max(1, min(60, time() - $lastTs));
    }
    $lastTs = time();

    // Создаём таблицу, если её нет
    $pdo->exec("CREATE TABLE IF NOT EXISTS energy_daily (
        date TEXT PRIMARY KEY,
        pv_kwh REAL DEFAULT 0,
        load_kwh REAL DEFAULT 0,
        bat_charge_kwh REAL DEFAULT 0,
        bat_discharge_kwh REAL DEFAULT 0,
        grid_import_kwh REAL DEFAULT 0,
        grid_export_kwh REAL DEFAULT 0,
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
    )");

    // Читаем текущую запись
    $row = $pdo->prepare("SELECT * FROM energy_daily WHERE date = ?");
    $row->execute([$today]);
    $cur = $row->fetch(PDO::FETCH_ASSOC);

    // Если записи нет — создаём
    if (!$cur) {
        $pdo->prepare("INSERT INTO energy_daily (date) VALUES (?)")->execute([$today]);
        $cur = ['pv_kwh' => 0, 'load_kwh' => 0, 'bat_charge_kwh' => 0,
                'bat_discharge_kwh' => 0, 'grid_import_kwh' => 0, 'grid_export_kwh' => 0];
    }

    // Мощности в Вт → энергия в кВт·ч за интервал dt
    $factor = $dt / 3600 / 1000; // Вт·с → кВт·ч

    $pvW = (float)($data['pv_power'] ?? 0);
    $loadW = (float)($data['load_power'] ?? 0);
    $batW = (float)($data['battery_power'] ?? 0); // + разряд, − заряд
    $gridW = (float)($data['grid_power'] ?? 0);   // + импорт, − экспорт

    $pvKwh    = $cur['pv_kwh']           + max(0, $pvW) * $factor;
    $loadKwh  = $cur['load_kwh']         + max(0, $loadW) * $factor;
    $batChKwh = $cur['bat_charge_kwh']   + ($batW < 0 ? -$batW : 0) * $factor;
    $batDisKwh= $cur['bat_discharge_kwh']+ ($batW > 0 ?  $batW : 0) * $factor;
    $gridImpK = $cur['grid_import_kwh']  + ($gridW > 0 ? $gridW : 0) * $factor;
    $gridExpK = $cur['grid_export_kwh']  + ($gridW < 0 ? -$gridW : 0) * $factor;

    // Обновляем
    $pdo->prepare("UPDATE energy_daily SET
        pv_kwh = ?, load_kwh = ?, bat_charge_kwh = ?, bat_discharge_kwh = ?,
        grid_import_kwh = ?, grid_export_kwh = ?, updated_at = CURRENT_TIMESTAMP
        WHERE date = ?")
        ->execute([$pvKwh, $loadKwh, $batChKwh, $batDisKwh, $gridImpK, $gridExpK, $today]);

    // Возвращаем для daily_summary
    return [
        'pv_today'                => round($pvKwh, 3),
        'load_today'              => round($loadKwh, 3),
        'battery_charge_today'    => round($batChKwh, 3),
        'battery_discharge_today' => round($batDisKwh, 3),
        'grid_import_today'       => round($gridImpK, 3),
        'grid_export_today'       => round($gridExpK, 3),
    ];
}

function updateDailySummary(PDO $pdo, array $data): void {
    $today = date('Y-m-d');
    $pv = $data['pv_today'] ?? 0;
    $load = $data['load_today'] ?? 0;
    $bc = $data['battery_charge_today'] ?? 0;
    $bd = $data['battery_discharge_today'] ?? 0;
    $forecast = $data['forecast_today'] ?? 0;
    
    $settings = $pdo->query("SELECT * FROM settings WHERE id = 1")->fetch();
    $tariff = isWinterSeason() && !empty($settings['el_heat_enabled'])
        ? (float)($settings['tariff_heat'] ?? 2.64)
        : (float)($settings['tariff_base'] ?? 4.32);
    $saved = $pv * $tariff;
    $co2 = $pv * 0.4;
    
        $peak = (float)$pdo->query("SELECT MAX(pv_power) FROM readings WHERE date(ts) = date('now')")->fetchColumn();

    $sql = "INSERT INTO daily_summary
              (date, pv_total, load_total, battery_charged, battery_discharged, saved_grn, co2_kg, forecast_total, pv_peak)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(date) DO UPDATE SET
              pv_total = MAX(pv_total, excluded.pv_total),
              load_total = MAX(load_total, excluded.load_total),
              battery_charged = MAX(battery_charged, excluded.battery_charged),
              battery_discharged = MAX(battery_discharged, excluded.battery_discharged),
              saved_grn = MAX(saved_grn, excluded.saved_grn),
              co2_kg = MAX(co2_kg, excluded.co2_kg),
              forecast_total = MAX(forecast_total, excluded.forecast_total),
              pv_peak = MAX(COALESCE(pv_peak, 0), excluded.pv_peak),
              updated_at = CURRENT_TIMESTAMP";
    $pdo->prepare($sql)->execute([$today, $pv, $load, $bc, $bd, $saved, $co2, $forecast, $peak]);
}
?>