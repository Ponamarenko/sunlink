"""Global audit fixes: telemetry mapping and unsafe direct-read endpoint."""

from __future__ import annotations

import os
import sys
import unittest
from unittest.mock import patch

SERVER_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, SERVER_DIR)

import deye_gateway  # noqa: E402
from domain.telemetry import TelemetrySnapshot, TelemetryValue  # noqa: E402
from domain.models import DataSource  # noqa: E402


class TestTelemetryMapping(unittest.TestCase):
    def test_real_register_names_reach_full_model(self):
        ts = "2026-11-22T10:00:00Z"
        values = {}
        for name, raw, unit in (
            ("total_battery_charge_low", 5608, "kWh"),
            ("total_battery_discharge_low", 5252, "kWh"),
            ("total_pv_energy_low", 6963, "kWh"),
            ("daily_grid_import", 101, "kWh"),
            ("daily_grid_export", 5, "kWh"),
        ):
            values[name] = TelemetryValue(name, raw / 10.0, unit, ts, "model_compatible", "valid", raw, 70)
        full = TelemetrySnapshot("inv", DataSource.DEYE_MODBUS, ts, True, values).to_full_dict()
        self.assertEqual(full["categories"]["battery"]["total_charge_energy"]["normalized_value"], 560.8)
        self.assertEqual(full["categories"]["battery"]["total_discharge_energy"]["normalized_value"], 525.2)
        self.assertEqual(full["categories"]["pv"]["total_energy"]["normalized_value"], 696.3)
        self.assertEqual(full["categories"]["grid"]["import_energy"]["normalized_value"], 10.1)
        self.assertEqual(full["categories"]["grid"]["export_energy"]["normalized_value"], 0.5)
        self.assertEqual(full["categories"]["energy"]["total_generation"]["normalized_value"], 696.3)
        self.assertEqual(full["categories"]["energy"]["daily_grid_import"]["normalized_value"], 10.1)

    def test_unmapped_names_do_not_create_phantom_values(self):
        ts = "2026-11-22T10:00:00Z"
        value = TelemetryValue("some_unknown", 1.0, "X", ts, "unknown", "valid", 1, 999)
        full = TelemetrySnapshot("inv", DataSource.DEYE_MODBUS, ts, True, {"some_unknown": value}).to_full_dict()
        self.assertTrue(all(v is None for fields in full["categories"].values() for v in fields.values()))


class TestDirectReadDisabled(unittest.TestCase):
    def test_post_read_is_forbidden_without_creating_client(self):
        from fastapi.testclient import TestClient
        with patch.object(deye_gateway, "ModbusTcpClient", lambda *a, **k: (_ for _ in ()).throw(AssertionError("second master"))):
            with TestClient(deye_gateway.app) as client:
                response = client.post("/read", json={"addresses": [184]})
        self.assertEqual(response.status_code, 403)
        self.assertIn("disabled", response.json()["detail"])


if __name__ == "__main__":
    unittest.main()
