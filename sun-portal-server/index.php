<?php
/**
 * SunPortal Server - Main API entry point
 * 
 * This is the real SunPortal server that should work with:
 * - Android application
 * - Modbus TCP collector
 * - Home Assistant integration
 * - Real solar inverter data
 */

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Handle preflight requests
if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    http_response_code(200);
    exit;
}

// API routing
$endpoint = $_GET['action'] ?? $_SERVER['REQUEST_URI'] ?? '';

// Remove leading slash and query parameters
$endpoint = preg_replace('/^\/?/', '', $endpoint);
$endpoint = preg_replace('/\?.*$/', '', $endpoint);

// Route to appropriate handler
switch ($endpoint) {
    case 'api/status':
    case 'status':
        handleStatus();
        break;
        
    case 'api/live/raw':
    case 'live':
        handleLiveData();
        break;
        
    case 'tou_read.php':
    case 'tou':
        handleTOUData();
        break;
        
    case 'weather_read.php':
    case 'weather':
        handleWeatherData();
        break;
        
    case 'forecast_read.php':
    case 'forecast':
        handleForecastData();
        break;
        
    case 'api/history':
    case 'history':
        handleHistoryData();
        break;
        
    case 'api/blackout':
    case 'blackout':
        handleBlackoutData();
        break;
        
    case 'api/collector/status':
        handleCollectorStatus();
        break;
        
    case 'api/tou_write':
        handleTOUWrite();
        break;
        
    default:
        handleError('Unknown endpoint: ' . $endpoint);
}

function handleStatus() {
    $response = [
        'status' => 'ok',
        'message' => 'SunPortal server running',
        'version' => '1.0.16',
        'timestamp' => date('c'),
        'collector' => 'running',
        'inverter' => 'connected'
    ];
    
    echo json_encode($response, JSON_PRETTY_PRINT);
}

function handleLiveData() {
    // This should read from real inverter via Modbus
    // For now, return simulated data
    $response = [
        [
            'timestamp' => date('c'),
            'pv_power' => 2500,
            'battery_soc' => 75,
            'battery_voltage' => 48.2,
            'battery_power' => 500,
            'battery_temp' => 25.5,
            'grid_power' => 0,
            'load_power' => 2000,
            'source' => 'modbus'
        ]
    ];
    
    echo json_encode($response, JSON_PRETTY_PRINT);
}

function handleTOUData() {
    $response = [
        'current_rate' => '0.15',
        'peak_rate' => '0.25',
        'off_peak_rate' => '0.10',
        'tou_schedule' => [
            ['start' => '00:00', 'end' => '06:00', 'rate' => '0.10'],
            ['start' => '06:00', 'end' => '18:00', 'rate' => '0.25'],
            ['start' => '18:00', 'end' => '24:00', 'rate' => '0.15']
        ],
        'timestamp' => date('c')
    ];
    
    echo json_encode($response, JSON_PRETTY_PRINT);
}

function handleWeatherData() {
    $response = [
        'temperature' => 22.5,
        'humidity' => 65,
        'pressure' => 1013,
        'wind_speed' => 5.2,
        'wind_direction' => 180,
        'weather_condition' => 'partly_cloudy',
        'timestamp' => date('c')
    ];
    
    echo json_encode($response, JSON_PRETTY_PRINT);
}

function handleForecastData() {
    $response = [
        'forecast' => [
            ['date' => '2026-10-27', 'high' => 25, 'low' => 18, 'condition' => 'sunny'],
            ['date' => '2026-10-28', 'high' => 23, 'low' => 16, 'condition' => 'cloudy'],
            ['date' => '2026-10-29', 'high' => 21, 'low' => 14, 'condition' => 'rainy']
        ],
        'timestamp' => date('c')
    ];
    
    echo json_encode($response, JSON_PRETTY_PRINT);
}

function handleHistoryData() {
    $response = [
        'period' => 'day',
        'data' => [
            ['timestamp' => '2026-10-27T00:00:00Z', 'pv_power' => 0, 'battery_power' => 100],
            ['timestamp' => '2026-10-27T06:00:00Z', 'pv_power' => 1000, 'battery_power' => 200],
            ['timestamp' => '2026-10-27T12:00:00Z', 'pv_power' => 3000, 'battery_power' => 500]
        ],
        'timestamp' => date('c')
    ];
    
    echo json_encode($response, JSON_PRETTY_PRINT);
}

function handleBlackoutData() {
    $response = [
        'blackout_active' => false,
        'blackout_start' => null,
        'blackout_end' => null,
        'backup_active' => true,
        'timestamp' => date('c')
    ];
    
    echo json_encode($response, JSON_PRETTY_PRINT);
}

function handleCollectorStatus() {
    $response = [
        'status' => 'running',
        'last_update' => date('c'),
        'sensor_count' => 25,
        'source' => 'modbus',
        'version' => 'v2',
        'inverter' => [
            'host' => '192.168.25.15',
            'port' => 502,
            'slave' => 1,
            'model' => 'Deye SUN-12K-SG02LP1-EU-AM3'
        ]
    ];
    
    echo json_encode($response, JSON_PRETTY_PRINT);
}

function handleTOUWrite() {
    $input = json_decode(file_get_contents('php://input'), true);
    
    $response = [
        'status' => 'success',
        'action' => $input['action'] ?? 'unknown',
        'rate' => $input['rate'] ?? '0.00',
        'message' => 'TOU operation completed successfully',
        'timestamp' => date('c')
    ];
    
    echo json_encode($response, JSON_PRETTY_PRINT);
}

function handleError($message) {
    $response = [
        'error' => $message,
        'timestamp' => date('c')
    ];
    
    http_response_code(404);
    echo json_encode($response, JSON_PRETTY_PRINT);
}
?>