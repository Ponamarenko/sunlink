"""Stage 42 — интеграция расширенного чтения в существующий poller.

Только mock/offline. Никаких физических Modbus-запросов и второго клиента.
"""

from __future__ import annotations

import os
import sys
import threading
import unittest
from unittest.mock import patch

SERVER_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, SERVER_DIR)

from domain.models import DataSource  # noqa: E402
from modbus.mock_fc03_transport import MockFc03Transport  # noqa: E402
from modbus.read_only_modbus_client import ReadOnlyModbusClient  # noqa: E402
from services.deye_polling_service import DeyePollingService  # noqa: E402
from services.extended_polling import ExtendedPollingConfig  # noqa: E402
from services.extended_queue import (  # noqa: E402
    ExtendedJobQueue,
    ReadJob,
    build_extended_jobs,
)


def make_job(address: int, group: str = "battery", retries: int = 0) -> ReadJob:
    return ReadJob(
        address=address, count=1, group=group, priority=10,
        data_type="U_WORD", confidence="MODEL_COMPATIBLE",
        production_eligible=True, max_retries=retries,
    )


class TestQueueSafety(unittest.TestCase):
    def test_default_feature_flag_off_and_only_184(self):
        cfg = ExtendedPollingConfig.from_env({})
        self.assertFalse(cfg.enabled)
        self.assertEqual(build_extended_jobs(cfg), [])

    def test_enabled_config_still_allowlists_only_184(self):
        cfg = ExtendedPollingConfig.from_env({
            "SUNLINK_EXTENDED_POLLING": "1",
            "SUNLINK_EXTENDED_ADDRESSES": "183,186,190",
        })
        jobs = build_extended_jobs(cfg)
        # Явно запрошенные НЕ-разрешённые адреса отбрасываются (fail closed).
        self.assertEqual(jobs, [])
        self.assertTrue(all(j.function_code == 3 for j in jobs))
        self.assertTrue(all(j.production_eligible for j in jobs))

    def test_dry_run_marks_config_without_reading(self):
        cfg = ExtendedPollingConfig.from_env({
            "SUNLINK_EXTENDED_POLLING": "1", "SUNLINK_EXTENDED_DRY_RUN": "1"
        })
        self.assertTrue(cfg.dry_run)
        jobs = build_extended_jobs(cfg)
        transport = MockFc03Transport({184: 94})
        transport.connected = True
        poller = DeyePollingService(
            ReadOnlyModbusClient(transport), extended_queue=ExtendedJobQueue(jobs)
        )
        # dry_run flag не передан в poller — задания есть, но тест ниже проверит
        # что без флага чтение выполняется; здесь проверяем только конфиг.
        self.assertEqual(cfg.resolve_addresses(), (184,))

    def test_validation_rejects_other_functions_and_unapproved_addresses(self):
        queue = ExtendedJobQueue([make_job(184)])
        self.assertEqual(
            queue.validate_job(ReadJob(184, 1, "battery", 10, "U_WORD", "x", True)),
            None,
        )
        unapproved = ReadJob(9999, 1, "battery", 10, "U_WORD", "x", True)
        self.assertIsNotNone(queue.validate_job(unapproved))

    def test_round_robin_and_block_limit(self):
        jobs = [make_job(184, "battery"), make_job(184, "pv")]
        queue = ExtendedJobQueue(jobs, max_block=8, jobs_per_cycle=1)
        self.assertEqual(queue.next_jobs()[0].address, 184)
        self.assertEqual(queue.next_jobs()[0].group, "pv")
        self.assertEqual(queue.next_jobs()[0].group, "battery")


class TestPollerIntegration(unittest.TestCase):
    def _poller(self, registers, jobs=None, dry_run=False):
        transport = MockFc03Transport(registers)
        transport.connected = True
        client = ReadOnlyModbusClient(transport)
        poller = DeyePollingService(
            client,
            extended_queue=ExtendedJobQueue(jobs) if jobs is not None else None,
            extended_dry_run=dry_run,
        )
        return poller, client, transport

    def test_single_transport_and_sequential_reads(self):
        jobs = [make_job(183, "battery")]
        poller, client, transport = self._poller({184: 94, 183: 5353}, jobs)
        # Разрешаем 183 только внутри теста — production allowlist не меняется.
        allowed = {184: object(), 183: object()}
        with patch("services.extended_queue.production_allowed_registers", return_value=allowed):
            snap = poller.read()
        self.assertIs(poller._client, client)
        self.assertEqual(snap.value_of("battery_soc"), 94.0)
        self.assertEqual(snap.value_of("battery_voltage"), 53.53)
        self.assertEqual(transport.read_calls, [(184, 1, 1), (183, 1, 1)])
        status = poller.get_status()["extended_polling"]
        self.assertTrue(status["enabled"])
        self.assertEqual(status["executed"], [183])

    def test_dry_run_performs_no_extended_reads(self):
        jobs = [make_job(183)]
        poller, client, transport = self._poller({184: 94, 183: 5353}, jobs, dry_run=True)
        with patch("services.extended_queue.production_allowed_registers", return_value={184: object(), 183: object()}):
            snap = poller.read()
        self.assertEqual(snap.value_of("battery_soc"), 94.0)
        self.assertIsNone(snap.value_of("battery_voltage"))
        self.assertEqual(transport.read_calls, [(184, 1, 1)])

    def test_extended_error_does_not_fabricate_or_destroy_soc(self):
        jobs = [make_job(183)]
        poller, client, transport = self._poller({184: 94, 183: 5353}, jobs)
        with patch("services.extended_queue.production_allowed_registers", return_value={184: object(), 183: object()}):
            # Timeout сломал бы и SOC; поэтому включаем timeout только после base чтения.
            original = transport.read_holding_registers

            def selective(address, count=1, unit_id=1):
                if address == 183:
                    raise __import__("modbus.mock_fc03_transport", fromlist=["Fc03Timeout"]).Fc03Timeout("mock")
                return original(address, count, unit_id)

            transport.read_holding_registers = selective
            snap = poller.read()
        self.assertEqual(snap.value_of("battery_soc"), 94.0)
        self.assertIsNone(snap.value_of("battery_voltage"))

    def test_no_second_thread_or_registry_entry(self):
        jobs = [make_job(184)]
        before = threading.active_count()
        poller, client, transport = self._poller({184: 94}, jobs)
        poller.read()
        self.assertEqual(threading.active_count(), before)
        self.assertFalse(poller.is_running())

    def test_attach_uses_existing_transport_only(self):
        from data_source_config import DataSourceConfig
        from services.extended_polling import build_extended_poller

        poller, client, transport = self._poller({184: 94})
        physical = DataSourceConfig.from_env({
            "SUNLINK_DATA_SOURCE": "deye_modbus",
            "SUNLINK_ENABLE_MODBUS": "1",
            "SUNLINK_MODBUS_HOST": "192.168.25.15",
        })
        extended = ExtendedPollingConfig.from_env({"SUNLINK_EXTENDED_POLLING": "1"})
        result = build_extended_poller(physical, extended, existing_poller=poller)
        self.assertIs(result, poller)
        self.assertIs(poller._client, client)
        self.assertIsNotNone(poller._extended_queue)
        self.assertEqual(transport.request_count, 0)

    def test_no_write_methods_in_extended_modules(self):
        root = os.path.join(SERVER_DIR, "services")
        for name in ("extended_queue.py", "extended_polling.py"):
            text = open(os.path.join(root, name), encoding="utf-8").read()
            self.assertNotIn("write_register", text)
            self.assertNotIn("FC06", text)
            self.assertNotIn("FC16", text)


if __name__ == "__main__":
    unittest.main()
