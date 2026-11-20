"""
Stage 25B/25B.1 — offline тесты конфигурации HTTP 8800 + HTTPS 9443.

Только temp files и env; никаких production-подключений.
"""

from __future__ import annotations

import os
import sys
import tempfile
import unittest

SERVER_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, SERVER_DIR)

import deye_gateway  # noqa: E402
from listener_config import build_configs  # noqa: E402
from tls_config import TlsConfig, TlsConfigError  # noqa: E402


class TestTlsConfig(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.mkdtemp()
        self.cert = os.path.join(self.tmp, "fullchain.pem")
        self.key = os.path.join(self.tmp, "privkey.pem")
        for path in (self.cert, self.key):
            with open(path, "w", encoding="utf-8") as f:
                f.write("test-only placeholder\n")
        os.chmod(self.key, 0o600)

    def env(self, **extra):
        return {
            "SUNLINK_TLS_CERTFILE": self.cert,
            "SUNLINK_TLS_KEYFILE": self.key,
            **extra,
        }

    def test_defaults_are_8800_and_9443(self):
        cfg = TlsConfig.from_env({})
        self.assertEqual(cfg.http_port, 8800)
        self.assertEqual(cfg.https_port, 9443)
        self.assertFalse(cfg.tls_enabled)

    def test_valid_tls_pair(self):
        cfg = TlsConfig.from_env(self.env())
        self.assertTrue(cfg.tls_enabled)
        https = cfg.https_kwargs()
        self.assertEqual(https["port"], 9443)
        self.assertEqual(https["ssl_certfile"], self.cert)
        self.assertEqual(https["ssl_keyfile"], self.key)
        self.assertEqual(cfg.http_kwargs(), {"host": "0.0.0.0", "port": 8800})

    def test_cert_without_key_rejected(self):
        with self.assertRaises(TlsConfigError):
            TlsConfig.from_env({"SUNLINK_TLS_CERTFILE": self.cert})

    def test_key_without_cert_rejected(self):
        with self.assertRaises(TlsConfigError):
            TlsConfig.from_env({"SUNLINK_TLS_KEYFILE": self.key})

    def test_missing_files_rejected(self):
        with self.assertRaises(TlsConfigError):
            TlsConfig.from_env({
                "SUNLINK_TLS_CERTFILE": "/no/such/cert.pem",
                "SUNLINK_TLS_KEYFILE": self.key,
            })

    def test_https_kwargs_require_tls(self):
        with self.assertRaises(TlsConfigError):
            TlsConfig.from_env({}).https_kwargs()

    def test_invalid_ports(self):
        with self.assertRaises(TlsConfigError):
            TlsConfig.from_env({"SUNLINK_HTTP_PORT": "0"})
        with self.assertRaises(TlsConfigError):
            TlsConfig.from_env({"SUNLINK_HTTPS_PORT": "abc"})

    def test_equal_ports_rejected(self):
        with self.assertRaises(TlsConfigError):
            TlsConfig.from_env(self.env(SUNLINK_HTTP_PORT="9443"))

    def test_custom_ports_and_hosts(self):
        cfg = TlsConfig.from_env(self.env(
            SUNLINK_HTTP_HOST="127.0.0.1",
            SUNLINK_HTTP_PORT="18800",
            SUNLINK_HTTPS_HOST="0.0.0.0",
            SUNLINK_HTTPS_PORT="19443",
        ))
        self.assertEqual(cfg.http_kwargs(), {"host": "127.0.0.1", "port": 18800})
        self.assertEqual(cfg.https_kwargs()["port"], 19443)


class TestDualListeners(unittest.TestCase):
    def test_two_configs_share_one_fastapi_app(self):
        cfg = TlsConfig.from_env({
            "SUNLINK_TLS_CERTFILE": __file__,   # file exists, enough for config
            "SUNLINK_TLS_KEYFILE": __file__,
        })
        http, https = build_configs(deye_gateway.app, cfg)
        self.assertIs(http.app, deye_gateway.app)
        self.assertIs(https.app, deye_gateway.app)
        self.assertEqual(http.port, 8800)
        self.assertEqual(https.port, 9443)
        self.assertEqual(http.lifespan, "on")
        self.assertEqual(https.lifespan, "off")  # один startup/shutdown

    def test_no_tls_https_config_has_no_ssl(self):
        cfg = TlsConfig.from_env({})
        http, https = build_configs(deye_gateway.app, cfg)
        self.assertIsNone(https.ssl_certfile)
        self.assertIsNone(https.ssl_keyfile)
        self.assertEqual(https.lifespan, "off")

    def test_no_write_endpoints_and_active_map(self):
        paths = [r.path for r in deye_gateway.app.routes if hasattr(r, "path")]
        for forbidden in ("/write", "/set-register", "/control"):
            self.assertNotIn(forbidden, paths)
        from domain.register_metadata import ACTIVE_POLL_REGISTERS
        self.assertEqual(set(ACTIVE_POLL_REGISTERS), {184})


if __name__ == "__main__":
    unittest.main()
