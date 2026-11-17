"""SunPortal Data Layer — domain package."""

from .models import (
    Measurement,
    MeasurementQuality,
    ConfidenceLevel,
    DataSource,
    NormalizedReading,
)
from .register_metadata import RegisterMetadata, REGISTER_MAP
from .normalizer import (
    decode_u_word,
    decode_s_word,
    apply_scale,
    apply_temperature_offset,
    normalize_register,
    NormalizationResult,
)

__all__ = [
    "Measurement",
    "MeasurementQuality",
    "ConfidenceLevel",
    "DataSource",
    "NormalizedReading",
    "RegisterMetadata",
    "REGISTER_MAP",
    "decode_u_word",
    "decode_s_word",
    "apply_scale",
    "apply_temperature_offset",
    "normalize_register",
    "NormalizationResult",
]
