"""
SunLink — конфигурация ДВУХ listener одного FastAPI app (Stage 25B.1).

  HTTP  : 0.0.0.0:8800  (локальная сеть, диагностика)  — без TLS
  HTTPS : 0.0.0.0:9443  (публичный доступ)             — TLS в Uvicorn

Reverse Proxy не используется. Один FastAPI application, один startup/shutdown,
один PollingService. Переменные окружения:

  SUNLINK_HTTP_HOST / SUNLINK_HTTP_PORT       (default 0.0.0.0 / 8800)
  SUNLINK_HTTPS_HOST / SUNLINK_HTTPS_PORT     (default 0.0.0.0 / 9443)
  SUNLINK_TLS_CERTFILE / SUNLINK_TLS_KEYFILE  (полный путь к fullchain/privkey)

 backward compatibility: SUNLINK_HTTPS_PORT=8443 из Stage 25B читается, но
 порт 8443 больше не является default и в новых примерах не используется.
"""

from __future__ import annotations

import os
from dataclasses import dataclass
from typing import Dict, Mapping, Optional


class TlsConfigError(ValueError):
    """Понятная ошибка конфигурации (без секретов в сообщении)."""


@dataclass(frozen=True)
class TlsConfig:
    http_host: str = "0.0.0.0"
    http_port: int = 8800
    https_host: str = "0.0.0.0"
    https_port: int = 9443
    certfile: Optional[str] = None
    keyfile: Optional[str] = None

    @property
    def tls_enabled(self) -> bool:
        return self.certfile is not None and self.keyfile is not None

    def https_kwargs(self) -> Dict[str, object]:
        if not self.tls_enabled:
            raise TlsConfigError("HTTPS requires both certificate and private key")
        return {
            "host": self.https_host,
            "port": self.https_port,
            "ssl_certfile": self.certfile,
            "ssl_keyfile": self.keyfile,
        }

    def http_kwargs(self) -> Dict[str, object]:
        return {"host": self.http_host, "port": self.http_port}

    @classmethod
    def from_env(cls, environ: Optional[Mapping[str, str]] = None) -> "TlsConfig":
        env = environ if environ is not None else os.environ

        def port(name: str, default: int) -> int:
            raw = env.get(name, str(default))
            try:
                value = int(raw)
            except (TypeError, ValueError) as exc:
                raise TlsConfigError(f"{name} must be an integer") from exc
            if not 1 <= value <= 65535:
                raise TlsConfigError(f"{name} must be in range 1..65535")
            return value

        http_port = port("SUNLINK_HTTP_PORT", 8800)
        https_port = port("SUNLINK_HTTPS_PORT", 9443)
        if http_port == https_port:
            raise TlsConfigError("HTTP and HTTPS ports must differ")

        cert = env.get("SUNLINK_TLS_CERTFILE", "").strip() or None
        key = env.get("SUNLINK_TLS_KEYFILE", "").strip() or None
        if bool(cert) != bool(key):
            raise TlsConfigError(
                "SUNLINK_TLS_CERTFILE and SUNLINK_TLS_KEYFILE must be set together"
            )
        for name, path in (("certificate", cert), ("private key", key)):
            if path and not os.path.isfile(path):
                raise TlsConfigError(f"TLS {name} file not found")

        return cls(
            http_host=env.get("SUNLINK_HTTP_HOST", "0.0.0.0").strip() or "0.0.0.0",
            http_port=http_port,
            https_host=env.get("SUNLINK_HTTPS_HOST", "0.0.0.0").strip() or "0.0.0.0",
            https_port=https_port,
            certfile=cert,
            keyfile=key,
        )


__all__ = ["TlsConfig", "TlsConfigError"]
