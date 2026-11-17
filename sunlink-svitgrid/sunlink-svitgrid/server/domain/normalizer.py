"""
SunPortal Data Layer — pure normalization functions.

No Modbus requests are made here. Reading registers, normalizing values and
persisting them are separate concerns.
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Optional

from .models import ConfidenceLevel, MeasurementQuality, NormalizedReading
from .register_metadata import RegisterMetadata


# ---------------------------------------------------------------------------
# Primitive decoders (pure functions)
# ---------------------------------------------------------------------------

def decode_u_word(raw: int) -> int:
    """Decode a 16-bit unsigned register value.

    Values 0..65535 are returned unchanged.
    """
    if not 0 <= raw <= 0xFFFF:
        raise ValueError(f"raw value {raw} out of range for U_WORD (0..65535)")
    return raw


def decode_s_word(raw: int) -> int:
    """Decode a 16-bit signed register value (two's complement).

    Values 0..32767 are positive; 32768..65535 decode as -32768..-1.
    """
    if not 0 <= raw <= 0xFFFF:
        raise ValueError(f"raw value {raw} out of range for S_WORD (0..65535)")
    return raw - 65536 if raw > 32767 else raw


def apply_scale(value: int, scale: float) -> float:
    """Apply a linear scale factor to a decoded integer value."""
    return value * scale


def apply_temperature_offset(value: float, offset: float = 100.0) -> float:
    """Apply a temperature offset: value - offset (Sunsynk TempSensor convention)."""
    return value - offset


# ---------------------------------------------------------------------------
# Composite normalization
# ---------------------------------------------------------------------------

@dataclass(frozen=True)
class NormalizationResult:
    """Outcome of normalizing one raw register."""

    register_address: int
    parameter_name: str
    raw_value: int
    decoded_value: int
    normalized_value: Optional[float]
    unit: str
    formula: str
    confidence: ConfidenceLevel
    quality: MeasurementQuality
    error: Optional[str] = None

    @property
    def ok(self) -> bool:
        return self.error is None and self.normalized_value is not None


def normalize_register(
    register_address: int,
    raw_value: int,
    metadata: RegisterMetadata,
    *,
    quality: MeasurementQuality = MeasurementQuality.VALID,
) -> NormalizationResult:
    """Normalize one raw Modbus register into an engineering value.

    Pure function: no I/O, no Modbus, no persistence.
    """
    # 1. Decode raw word according to signedness.
    try:
        if metadata.is_signed:
            decoded = decode_s_word(raw_value)
        else:
            decoded = decode_u_word(raw_value)
    except ValueError as exc:
        return NormalizationResult(
            register_address=register_address,
            parameter_name=metadata.name,
            raw_value=raw_value,
            decoded_value=raw_value,
            normalized_value=None,
            unit=metadata.unit,
            formula=metadata.formula,
            confidence=metadata.confidence,
            quality=MeasurementQuality.ERROR,
            error=str(exc),
        )

    # 2. Apply scale.
    scaled = apply_scale(decoded, metadata.scale)

    # 3. Apply temperature offset when configured (Sunsynk convention:
    #    value = raw x scale - offset).
    normalized = scaled
    formula = metadata.formula
    if metadata.offset != 0.0:
        normalized = apply_temperature_offset(scaled, metadata.offset)
        formula = f"{metadata.formula} - {metadata.offset}"

    return NormalizationResult(
        register_address=register_address,
        parameter_name=metadata.name,
        raw_value=raw_value,
        decoded_value=decoded,
        normalized_value=normalized,
        unit=metadata.unit,
        formula=formula,
        confidence=metadata.confidence,
        quality=quality,
    )


def normalize_to_reading(
    register_address: int,
    raw_value: int,
    metadata: RegisterMetadata,
    *,
    quality: MeasurementQuality = MeasurementQuality.VALID,
) -> NormalizedReading:
    """Convenience: normalize and return a NormalizedReading."""
    result = normalize_register(register_address, raw_value, metadata, quality=quality)
    return NormalizedReading(
        register_address=result.register_address,
        parameter_name=result.parameter_name,
        raw_value=result.raw_value,
        normalized_value=result.normalized_value,  # type: ignore[arg-type]
        unit=result.unit,
        formula=result.formula,
        confidence=result.confidence,
        quality=result.quality,
    )


def make_measurement(
    *,
    inverter_id: str,
    register_address: Optional[int],
    parameter_name: str,
    raw_value: Optional[int],
    normalized_value: Optional[float],
    unit: str,
    timestamp,
    quality: MeasurementQuality,
    confidence: ConfidenceLevel,
    source: DataSource,
    description: str = "",
) -> Measurement:
    """Factory for Measurement (keeps construction in one place)."""
    from .models import Measurement  # local import to avoid circularity at module load

    return Measurement(
        inverter_id=inverter_id,
        register_address=register_address,
        parameter_name=parameter_name,
        raw_value=raw_value,
        normalized_value=normalized_value,
        unit=unit,
        timestamp=timestamp,
        quality=quality,
        confidence=confidence,
        source=source,
        description=description,
    )


from .models import DataSource, Measurement  # noqa: E402
