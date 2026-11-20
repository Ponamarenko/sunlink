"""
SunLink — два listener ОДНОГО FastAPI application (Stage 25B.1).

HTTP 8800  -> локальный доступ, lifespan ВКЛЮЧЁН (единственный startup/shutdown).
HTTPS 9443 -> публичный доступ, lifespan ВЫКЛЮЧЕН, TLS прямо в Uvicorn.

Это исключает двойной вызов startup/shutdown, двойного PollingService и
конкуренцию за SQLite. При отсутствии сертификата HTTPS не запускается,
но локальный HTTP продолжает работать.
"""

from __future__ import annotations

import asyncio
import logging
from typing import Tuple

import uvicorn

from tls_config import TlsConfig

logger = logging.getLogger(__name__)


def build_configs(app, cfg: TlsConfig) -> Tuple[uvicorn.Config, uvicorn.Config]:
    """Создаёт конфигурации двух серверов; не слушает порты."""
    http = uvicorn.Config(
        app,
        host=cfg.http_host,
        port=cfg.http_port,
        lifespan="on",
        reload=False,
        log_level="info",
    )
    https_kwargs = cfg.https_kwargs() if cfg.tls_enabled else {
        "host": cfg.https_host, "port": cfg.https_port
    }
    https = uvicorn.Config(
        app,
        lifespan="off",
        reload=False,
        log_level="info",
        **https_kwargs,
    )
    return http, https


async def run_listeners(app, cfg: TlsConfig) -> None:
    """Запускает HTTP и (при наличии TLS) HTTPS в одном event loop."""
    http_conf, https_conf = build_configs(app, cfg)
    http_server = uvicorn.Server(http_conf)

    if not cfg.tls_enabled:
        logger.error(
            "HTTPS disabled: valid SUNLINK_TLS_CERTFILE / SUNLINK_TLS_KEYFILE "
            "are required; local HTTP :%s still available",
            cfg.http_port,
        )
        await http_server.serve()
        return

    https_server = uvicorn.Server(https_conf)
    # Signal handlers только у основного сервера — один shutdown.
    https_server.install_signal_handlers = False
    await asyncio.gather(http_server.serve(), https_server.serve())


__all__ = ["build_configs", "run_listeners"]
