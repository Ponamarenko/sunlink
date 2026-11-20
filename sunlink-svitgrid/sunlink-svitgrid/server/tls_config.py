"""
SunLink — подготовка прямого HTTPS в существующем ASGI-сервере (Stage 25B).

Никакого Reverse Proxy. Используется штатная TLS-поддержка Uvicorn:
  ssl_certfile / ssl_keyfile.

Переменные окружения:
  SUNLINK_HTTPS_PORT      — порт слушателя (по умолчанию 8443)
  SUNLINK_TLS_CERTFILE    — путь к fullchain.pem
  SUNLINK_TLS_KEYFILE     — путь к privkey.pem
  SUNLINK_TLS_CA_CERTS    — опционально, цепочка CA

Значения не логируются. Production-сервер/CI не изменяются этим модулем.
"""

from __future__ import annotations

import os
from dataclasses import dataclass
from typing import Dict, Mapping, Optional


class TlsConfigError(ValueError):
    """Понятная ошибка конфигурации TLS (без секретов в сообщении)."""


@dataclass(frozen=True)
class TlsConfig:
    port: int
    certfile: Optional[str] = None
    keyfile: Optional[str] = None
    ca_certs: Optional[str] = None

    @property
    def enabled(self) -> bool:
        return self.certfile is not None and self.keyfile is not None

    def uvicorn_kwargs(self) -> Dict[str, object]:
        """Аргументы для uvicorn.run; при выключенном TLS — пустой dict."""
        if not self.enabled:
            return {}
        kwargs: Dict[str, object] = {
            "ssl_certfile": self.certfile,
            "ssl_keyfile": self.keyfile,
        }
        if self.ca_certs:
            kwargs["ssl_ca_certs"] = self.ca_certs
        return kwargs

    @classmethod
    def from_env(cls, environ: Optional[Mapping[str, str]] = None) -> "TlsConfig":
        env = environ if environ is not None else os.environ
        raw_port = env.get("SUNLINK_HTTPS_PORT", "8443")
        try:
            port = int(raw_port)
        except (TypeError, ValueError) as exc:
            raise TlsConfigError("SUNLINK_HTTPS_PORT must be an integer") from exc
        if not 1 <= port <= 65535:
            raise TlsConfigError("SUNLINK_HTTPS_PORT must be in range 1..65535")

        cert = env.get("SUNLINK_TLS_CERTFILE", "").strip() or None
        key = env.get("SUNLINK_TLS_KEYFILE", "").strip() or None
        ca = env.get("SUNLINK_TLS_CA_CERTS", "").strip() or None

        if bool(cert) != bool(key):
            raise TlsConfigError(
                "SUNLINK_TLS_CERTFILE and SUNLINK_TLS_KEYFILE must be set together"
            )
        for name, path in (("certificate", cert), ("private key", key), ("CA", ca)):
            if path and not os.path.isfile(path):
                raise TlsConfigError(f"TLS {name} file not found")
        return cls(port=port, certfile=cert, keyfile=key, ca_certs=ca)


__all__ = ["TlsConfig", "TlsConfigError"]
