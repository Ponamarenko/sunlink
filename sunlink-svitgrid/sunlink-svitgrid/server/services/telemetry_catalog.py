"""Stage 48 — единый каталог параметров телеметрии.

Описания берутся из существующей register map; никаких новых адресов
и никаких фактических значений каталог не содержит.
"""

from __future__ import annotations

from typing import Any, Dict, List

from domain.extended_register_map import EXTENDED_REGISTERS
from domain.register_metadata import REGISTER_MAP
from services.history_service import KNOWN_PARAMETERS


def build_catalog() -> Dict[str, Any]:
    items: List[Dict[str, Any]] = []
    for address in sorted(set(REGISTER_MAP) | set(EXTENDED_REGISTERS)):
        ext = EXTENDED_REGISTERS.get(address)
        base = REGISTER_MAP.get(address)
        name = ext.name if ext else base.name  # type: ignore[union-attr]
        items.append({
            "api_name": name,
            "group": ext.group if ext else "battery",
            "address": address,
            "register_count": ext.register_count if ext else 1,
            "function_code": ext.function_code if ext else 3,
            "data_type": ext.data_type if ext else base.data_type,  # type: ignore[union-attr]
            "signed": ext.is_signed if ext else base.is_signed,  # type: ignore[union-attr]
            "scale": ext.scale if ext else base.scale,  # type: ignore[union-attr]
            "unit": ext.unit if ext else base.unit,  # type: ignore[union-attr]
            "source": ext.source if ext else "project register map",
            "confidence": ext.confidence if ext else base.confidence.value,  # type: ignore[union-attr]
            "verified": bool(ext.physically_verified) if ext else address == 184,
            "production_allowed": bool(ext.production_allowed) if ext else address == 184,
            "historical": name in KNOWN_PARAMETERS,
        })
    return {
        "count": len(items),
        "verified_count": sum(1 for item in items if item["verified"]),
        "production_allowed_count": sum(1 for item in items if item["production_allowed"]),
        "items": items,
    }


__all__ = ["build_catalog"]
