"""
Stage 25B — offline тесты прямого HTTPS-конфигурации backend.

Никаких production-подключений и настоящих сертификатов: только env + temp files.
"""

from __future__ import annotations

import os
import sys
import tempfile
import unittest

SERVER_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, SERVER_DIR)

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

    def test_default_is_https_port_without_tls_files(self):
        cfg = TlsConfig.from_env({})
        self.assertEqual(cfg.port, 8443)
        self.assertFalse(cfg.enabled)
        self.assertEqual(cfg.uvicorn_kwargs(), {})

    def test_valid_tls_pair(self):
        cfg = TlsConfig.from_env({
            "SUNLINK_TLS_CERTFILE": self.cert,
            "SUNLINK_TLS_KEYFILE": self.key,
        })
        self.assertTrue(cfg.enabled)
        kwargs = cfg.uvicorn_kwargs()
        self.assertEqual(kwargs["ssl_certfile"], self.cert)
        self.assertEqual(kwargs["ssl_keyfile"], self.key)
        self.assertNotIn("ssl_ca_certs", kwargs)

    def test_cert_without_key_rejected(self):
        with self.assertRaises(TlsConfigError) as ctx:
            TlsConfig.from_env({"SUNLINK_TLS_CERTFILE": self.cert})
        self.assertIn("together", str(ctx.exception))

    def test_key_without_cert_rejected(self):
        with self.assertRaises(TlsConfigError):
            TlsConfig.from_env({"SUNLINK_TLS_KEYFILE": self.key})

    def test_missing_files_rejected(self):
        with self.assertRaises(TlsConfigError):
            TlsConfig.from_env({
                "SUNLINK_TLS_CERTFILE": "/no/such/cert.pem",
                "SUNLINK_TLS_KEYFILE": self.key,
            })

    def test_invalid_port(self):
        with self.assertRaises(TlsConfigError):
            TlsConfig.from_env({"SUNLINK_HTTPS_PORT": "0"})
        with self.assertRaises(TlsConfigError):
            TlsConfig.from_env({"SUNLINK_HTTPS_PORT": "abc"})

    def test_custom_port(self):
        cfg = TlsConfig.from_env({"SUNLINK_HTTPS_PORT": "9443"})
        self.assertEqual(cfg.port, 9443)

    def test_optional_ca_certs(self):
        ca = os.path.join(self.tmp, "ca.pem")
        with open(ca, "w", encoding="utf-8") as f:
            f.write("placeholder\n")
        cfg = TlsConfig.from_env({
            "SUNLINK_TLS_CERTFILE": self.cert,
            "SUNLINK_TLS_KEYFILE": self.key,
            "SUNLINK_TLS_CA_CERTS": ca,
        })
        self.assertEqual(cfg.uvicorn_kwargs()["ssl_ca_certs"], ca)


class TestTlsSecurityPolicy(unittest.TestCase):
    """Проверки, запрещающие небезопасные режимы в конфигурации."""

    def test_no_trust_all_option(self):
        # В конфигурации нет возможности отключить проверку сертификата.
        self.assertNotIn("verify", " ".join(TlsConfig.__annotations__))
        self.assertFalse(hasattr(TlsConfig, "insecure"))

    def test_no_reverse_proxy_dependency(self):
        # Прямой TLS: конфигурация не содержит proxy-полей.
        for field in ("proxy_pass", "upstream", "reverse_proxy"):
            self.assertNotIn(field, TlsConfig.__annotations__)


if __name__ == "__main__":
    unittest.main()
