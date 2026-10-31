<?php
/**
 * SunPortal Server Configuration
 * 
 * This is the real configuration for SunPortal server
 * that should work with solar inverter via Modbus TCP
 */

// Server Configuration
$serverConfig = [
    'host' => '0.0.0.0',
    'port' => 80,
    'modbus_host' => '192.168.25.15', // Deye inverter IP
    'modbus_port' => 502,
    'modbus_slave' => 1,
    'collector_interval' => 30,
    'log_file' => '/var/log/sunportal/collector.log'
];

// Inverter Configuration (Deye SUN-12K-SG02LP1-EU-AM3)
$inverterConfig = [
    'model' => 'Deye SUN-12K-SG02LP1-EU-AM3',
    'registers' => [
        // Grid registers (150-172)
        'grid_frequency' => [79, 'u16', 0.01, 0],
        'grid_voltage_l1' => [150, 'u16', 0.1, 0],
        'grid_voltage_l2' => [151, 'u16', 0.1, 0],
        'grid_voltage_l3' => [152, 'u16', 0.1, 0],
        'grid_current_l1' => [153, 'u16', 0.1, 0],
        'grid_current_l2' => [154, 'u16', 0.1, 0],
        'grid_current_l3' => [155, 'u16', 0.1, 0],
        'grid_power_l1' => [156, 'u16', 0.1, 0],
        'grid_power_l2' => [157, 'u16', 0.1, 0],
        'grid_power_l3' => [158, 'u16', 0.1, 0],
        'grid_total_power' => [159, 'u16', 0.1, 0],
        
        // PV registers (200-219)
        'pv_voltage_l1' => [200, 'u16', 0.1, 0],
        'pv_voltage_l2' => [201, 'u16', 0.1, 0],
        'pv_current_l1' => [202, 'u16', 0.1, 0],
        'pv_current_l2' => [203, 'u16', 0.1, 0],
        'pv_power_l1' => [204, 'u16', 0.1, 0],
        'pv_power_l2' => [205, 'u16', 0.1, 0],
        'pv_total_power' => [206, 'u16', 0.1, 0],
        
        // Battery registers (300-319)
        'battery_voltage' => [300, 'u16', 0.1, 0],
        'battery_current' => [301, 'u16', 0.1, 0],
        'battery_power' => [302, 'u16', 0.1, 0],
        'battery_soc' => [303, 'u16', 0.1, 0],
        'battery_temp' => [304, 'u16', 0.1, 0],
        'battery_status' => [305, 'u16', 1, 0],
        
        // Load registers (400-419)
        'load_power_l1' => [400, 'u16', 0.1, 0],
        'load_power_l2' => [401, 'u16', 0.1, 0],
        'load_total_power' => [402, 'u16', 0.1, 0]
    ]
];

// API Endpoints Configuration
$apiEndpoints = [
    'status' => 'api/status',
    'live' => 'api/live/raw',
    'tou' => 'tou_read.php',
    'weather' => 'weather_read.php',
    'forecast' => 'forecast_read.php',
    'history' => 'api/history',
    'blackout' => 'api/blackout',
    'collector' => 'api/collector/status',
    'tou_write' => 'api/tou_write'
];

// Return configuration
return [
    'server' => $serverConfig,
    'inverter' => $inverterConfig,
    'api' => $apiEndpoints
];
?>