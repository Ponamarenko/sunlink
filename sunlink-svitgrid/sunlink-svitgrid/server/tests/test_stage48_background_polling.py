"""Stage 48 — фоновый polling и каталог телеметрии (offline)."""

from __future__ import annotations

import os
import sys
import tempfile
import threading
import time
import unittest

SERVER_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, SERVER_DIR)

from fastapi.testclient import TestClient  # noqa: E402
import deye_gateway  # noqa: E402
from modbus.mock_fc03_transport import MockFc03Transport  # noqa: E402
from modbus.read_only_modbus_client import ReadOnlyModbusClient  # noqa: E402
from services.deye_polling_service import (  # noqa: E402
    DeyePollingService, PollerAlreadyRunning,
)
from services.telemetry_catalog import build_catalog  # noqa: E402
from storage.sqlite_repository import SQLiteMeasurementRepository  # noqa: E402


def make_poller(path, interval=0.05, transport=None):
    transport = transport or MockFc03Transport({184: 94})
    transport.connected = True
    repo = SQLiteMeasurementRepository(path)
    poller = DeyePollingService(
        ReadOnlyModbusClient(transport), repository=repo,
        poll_interval=interval, reconnect_backoff=0.05, max_backoff=0.1,
    )
    return poller, transport, repo


class TestBackgroundPolling(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.NamedTemporaryFile(suffix=".db", delete=False)
        self.tmp.close()

    def tearDown(self):
        os.unlink(self.tmp.name)

    def test_background_cycle_saves_history_without_http(self):
        poller, transport, repo = make_poller(self.tmp.name)
        try:
            poller.start()
            time.sleep(0.3)
            status = poller.get_status()
            self.assertTrue(status["polling_running"])
            self.assertGreater(status["cycles"], 1)
            self.assertGreater(transport.request_count, 1)
            self.assertGreater(
                len(repo.get_history("deye_sg02lp1_am3", "battery_soc",
                                     __import__("datetime").datetime.min.replace(tzinfo=__import__("datetime").timezone.utc),
                                     __import__("datetime").datetime.max.replace(tzinfo=__import__("datetime").timezone.utc))),
                1,
            )
        finally:
            poller.stop()
            repo.close()

    def test_repeated_start_is_noop_and_second_poller_blocked(self):
        poller, transport, repo = make_poller(self.tmp.name)
        try:
            poller.start()
            poller.start()  # no-op
            self.assertTrue(poller.is_running())
            second, _, repo2 = make_poller(self.tmp.name + ".2")
            with self.assertRaises(PollerAlreadyRunning):
                second.start()
            second.stop()
            repo2.close()
        finally:
            poller.stop()
            repo.close()

    def test_stop_ends_cycles(self):
        poller, transport, repo = make_poller(self.tmp.name)
        poller.start()
        time.sleep(0.15)
        poller.stop()
        count = transport.request_count
        time.sleep(0.15)
        self.assertEqual(transport.request_count, count)
        self.assertFalse(poller.is_running())
        repo.close()

    def test_recovers_after_cycle_error(self):
        transport = MockFc03Transport({184: 94})
        transport.connected = True
        original = transport.read_holding_registers
        calls = {"n": 0}

        def flaky(address, count=1, device_id=1):
            calls["n"] += 1
            if calls["n"] <= 2:
                raise TimeoutError("mock timeout")
            return original(address, count, device_id)

        transport.read_holding_registers = flaky
        poller, _, repo = make_poller(self.tmp.name, transport=transport)
        try:
            poller.start()
            time.sleep(0.4)
            status = poller.get_status()
            self.assertGreater(status["failed_cycles"], 0)
            self.assertEqual(status["polling_state"], "online")
            self.assertEqual(poller.read().value_of("battery_soc"), 94.0)
        finally:
            poller.stop()
            repo.close()


class TestCatalogAndApi(unittest.TestCase):
    def test_catalog_has_all_groups_without_values(self):
        catalog = build_catalog()
        self.assertEqual(catalog["count"], 49)
        self.assertEqual(catalog["production_allowed_count"], 23)
        groups = {item["group"] for item in catalog["items"]}
        self.assertTrue({"battery", "pv", "grid", "load", "inverter"}.issubset(groups))
        self.assertTrue(all("value" not in item for item in catalog["items"]))

    def test_catalog_endpoint(self):
        old = deye_gateway._repository
        tmp = tempfile.NamedTemporaryFile(suffix=".db", delete=False)
        tmp.close()
        try:
            from storage.sqlite_repository import SQLiteMeasurementRepository
            deye_gateway._repository = SQLiteMeasurementRepository(tmp.name)
            with TestClient(deye_gateway.app) as client:
                response = client.get("/api/telemetry/catalog")
            self.assertEqual(response.status_code, 200)
            self.assertEqual(response.json()["count"], 49)
        finally:
            deye_gateway._repository = old
            os.unlink(tmp.name)


if __name__ == "__main__":
    unittest.main()
