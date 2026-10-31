<?php
header('Content-Type: application/json');

$action = $_GET['action'] ?? '';

if ($action === 'history') {
  $period = $_GET['period'] ?? 'day';
  echo json_encode([
    "period" => $period,
    "data" => [
      ["timestamp" => "2026-10-27T00:00:00Z", "pv_power" => 0, "battery_power" => 100],
      ["timestamp" => "2026-10-27T06:00:00Z", "pv_power" => 1000, "battery_power" => 200],
      ["timestamp" => "2026-10-27T12:00:00Z", "pv_power" => 3000, "battery_power" => 500]
    ]
  ]);
} elseif ($action === 'blackout') {
  echo json_encode([
    "blackout_active" => false,
    "blackout_start" => null,
    "blackout_end" => null,
    "backup_active" => true
  ]);
} else {
  echo json_encode(["error" => "Unknown action"]);
}
?>