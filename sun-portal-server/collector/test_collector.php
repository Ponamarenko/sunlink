<?php

// Simple test collector
echo "SunPortal Collector Test\n";
echo "========================\n";

// Simulated data
$data = [
    'pv_power' => 2500,
    'battery_soc' => 75,
    'battery_voltage' => 48.2,
    'battery_power' => 500,
    'battery_temp' => 25.5,
    'grid_power' => 0,
    'load_power' => 2000,
    'source' => 'simulated'
];

echo "Data collected successfully:\n";
foreach ($data as $key => $value) {
    echo "$key: $value\n";
}

echo "\nCollector completed successfully!\n";

?>
