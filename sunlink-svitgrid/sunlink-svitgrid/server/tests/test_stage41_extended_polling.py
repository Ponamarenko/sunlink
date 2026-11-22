"""Stage 41 — extended polling configuration disabled by default and quality API.

Offline only: no physical Modbus, no production changes.
"""

from __future__ import annotations

import os
import sys
import tempfile
import unittest
from unittest.mock import patch

SERVER_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, SERVER_DIR)

import deye_gateway  # noqa: E402
from data_source_config import DataSourceConfig  # noqa: E402
from domain.extended_register_map import EXTENDED_REGISTERS  # noqa: E402
from services.extended_polling import (  # noqa: E402
    ExtendedPollingConfig,
    build_extended_poller,
)
from services.telemetry_sources import DemoTelemetrySource  # noqa: E402
from storage.sqlite_repository import SQLiteMeasurementRepository  # noqa: E402
from domain.models import DataSource  # noqa: E402


class TestExtendedPollingConfig(unittest.TestCase):
    def test_disabled_by_default(self):
        cfg = ExtendedPollingConfig.from_env({})
        self.assertFalse(cfg.enabled)
        self.assertIn(184, cfg.resolve_addresses())
        self.assertIsNone(build_extended_poller(
            DataSourceConfig.from_env({
                "SUNLINK_DATA_SOURCE": "demo",
            }), cfg
        ))

    def test_explicitly_enabled_still_only_184(self):
        cfg = ExtendedPollingConfig.from_env({
            "SUNLINK_EXTENDED_POLLING": "1",
            "SUNLINK_EXTENDED_GROUPS": "battery,pv,grid,load",
            "SUNLINK_EXTENDED_ADDRESSES": "183,186,190",
        })
        self.assertTrue(cfg.enabled)
        self.assertIn(183, cfg.resolve_addresses())
        self.assertNotIn(190, cfg.resolve_addresses())
        plan = cfg.plan()
        self.assertGreaterEqual(len(plan), 1)
        self.assertTrue(all(a in cfg.resolve_addresses() for entry in plan for a in entry["addresses"]))

    def test_cannot_create_poller_for_demo_source(self):
        cfg = ExtendedPollingConfig.from_env({"SUNLINK_EXTENDED_POLLING": "1"})
        with self.assertRaises(RuntimeError):
            build_extended_poller(DataSourceConfig.from_env({"SUNLINK_DATA_SOURCE": "demo"}), cfg)

    def test_invalid_config_rejected(self):
        with self.assertRaises(ValueError):
            ExtendedPollingConfig.from_env({"SUNLINK_EXTENDED_ADDRESSES": "abc"})
        with self.assertRaises(ValueError):
            ExtendedPollingConfig.from_env({"SUNLINK_EXTENDED_MAX_BLOCK": "0"})

    def test_all_register_metadata_present(self):
        self.assertGreaterEqual(len(EXTENDED_REGISTERS), 45)
        allowed = [r for r in EXTENDED_REGISTERS.values() if r.production_allowed]
        self.assertIn(184, [r.address for r in allowed])
        self.assertNotIn(190, [r.address for r in allowed])


class TestQualityApi(unittest.TestCase):
    def test_quality_endpoint_reports_counts_without_fabrication(self):
        tmp = tempfile.NamedTemporaryFile(suffix=".db", delete=False)
        tmp.close()
        repo = SQLiteMeasurementRepository(tmp.name)
        gateway = deye_gateway.gateway
        old = (gateway.source, gateway.physical_modbus_allowed,
               gateway.telemetry_source, deye_gateway._repository)
        try:
            gateway.telemetry_source = DemoTelemetrySource()
            gateway.source = DataSource.DEMO_SIMULATOR
            gateway.physical_modbus_allowed = False
            deye_gateway._repository = repo
            with patch.object(deye_gateway, "ModbusTcpClient", lambda *a, **k: (_ for _ in ()).throw(AssertionError("physical client"))):
                from fastapi.testclient import TestClient
                with TestClient(deye_gateway.app) as client:
                    response = client.get("/api/telemetry/quality")
            self.assertEqual(response.status_code, 200)
            data = response.json()
            self.assertEqual(data["source"], "demo_simulator")
            self.assertEqual(data["active_registers"], [184])
            self.assertGreater(data["known_parameters"], 0)
            self.assertGreaterEqual(data["unavailable_parameters"], 0)
            self.assertIn("pv", data["categories"])
        finally:
            (gateway.source, gateway.physical_modbus_allowed,
             gateway.telemetry_source, deye_gateway._repository) = old
            repo.close()
            os.unlink(tmp.name)

    def test_full_model_has_twelve_groups(self):
        from domain.telemetry import TelemetrySnapshot
        snapshot = TelemetrySnapshot("inv", DataSource.DEYE_MODBUS, "t", True, {})
        categories = snapshot.to_full_dict()["categories"]
        for group in ("battery", "pv", "grid", "load", "inverter", "energy",
                      "status", "warnings", "errors", "system"):
            self.assertIn(group, categories)


if __name__ == "__main__":
    unittest.main()
