"""
SunLink — единый декодер регистров Deye (этап 24).

Одна функция возвращает ВСЮ необходимую информацию о значении: raw, decoded,
normalized, unit, scale, offset, confidence, physical status, quality, error.
Отрицательные значения сохраняются. Отсутствие/некорректное значение даёт
normalized_value=None и quality=error — никогда 0.0.

Кандидатские преобразования (reg 190 x0.1, temps offset) применяются только
при use_candidate=True и явно помечаются как PROVISIONAL.
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Optional

from .models import ConfidenceLevel, MeasurementQuality
from .register_metadata import RegisterMetadata


# Допустимые инженерные диапазоны для已知的 параметров.
RANGES: dict[str, tuple[Optional[float], Optional[float]]] = {
    "battery_soc": (0.0, 100.0),
    "battery_voltage": (0.0, 100.0),
    "battery_current": (-500.0, 500.0),
    "battery_power": (-15000.0, 15000.0),
    "pv1_power": (0.0, 20000.0),
    "pv2_power": (0.0, 20000.0),
    "pv3_power": (0.0, 20000.0),
    "dc_temperature": (-50.0, 200.0),
    "ac_temperature": (-50.0, 200.0),
}


@dataclass(frozen=True)
class DecodedRegister:
    register_address: int
    parameter_name: str
    raw_value: Optional[int]
    decoded_value: Optional[int]
    normalized_value: Optional[float]
    unit: str
    scale: float
    offset: float
    data_type: str
    confidence: str
    physical_status: str
    quality: str
    used_candidate: bool
    error: Optional[str] = None

    @property
    def ok(self) -> bool:
        return self.normalized_value is not None and self.error is None


def _decode_word(raw: int, is_signed: bool) -> int:
    if not 0 <= raw <= 0xFFFF:
        raise ValueError(f"raw value {raw} out of 16-bit range")
    return raw - 65536 if is_signed and raw > 32767 else raw


def decode_register(
    raw_value: Optional[int],
    metadata: RegisterMetadata,
    *,
    use_candidate: bool = False,
) -> DecodedRegister:
    """Декодирует одно 16-битное register-значение в инженерную величину."""
    scale = metadata.scale
    offset = metadata.offset
    if use_candidate and metadata.candidate_scale is not None:
        scale = metadata.candidate_scale
    if use_candidate and metadata.candidate_offset is not None:
        # candidate_offset вычитается так же, как baseline offset.
        offset = metadata.candidate_offset

    base = DecodedRegister(
        register_address=metadata.address,
        parameter_name=metadata.name,
        raw_value=raw_value,
        decoded_value=None,
        normalized_value=None,
        unit=metadata.unit,
        scale=scale,
        offset=offset,
        data_type=metadata.data_type,
        confidence=(
            ConfidenceLevel.PROVISIONAL.value
            if use_candidate
            else metadata.confidence.value
        ),
        physical_status=metadata.physical_status,
        quality=MeasurementQuality.ERROR.value,
        used_candidate=use_candidate,
    )

    if raw_value is None:
        return DecodedRegister(
            **{**base.__dict__, "quality": MeasurementQuality.UNAVAILABLE.value,
               "error": "register unavailable"}
        )

    try:
        decoded = _decode_word(raw_value, metadata.is_signed)
    except ValueError as exc:
        return DecodedRegister(**{**base.__dict__, "error": str(exc)})

    normalized = decoded * scale - offset
    low, high = RANGES.get(metadata.name, (None, None))
    if low is not None and normalized < low:
        return DecodedRegister(
            **{**base.__dict__, "decoded_value": decoded,
               "error": f"value {normalized} below allowed {low}"}
        )
    if high is not None and normalized > high:
        return DecodedRegister(
            **{**base.__dict__, "decoded_value": decoded,
               "error": f"value {normalized} above allowed {high}"}
        )

    return DecodedRegister(
        **{
            **base.__dict__,
            "decoded_value": decoded,
            "normalized_value": normalized,
            "quality": MeasurementQuality.VALID.value,
        }
    )


__all__ = ["DecodedRegister", "decode_register", "RANGES"]
