"""
Stage 40 — декодирование расширенной карты регистров.

Только чистые функции. Никаких Modbus-запросов. Отрицательные значения
сохраняются, неполные/некорректные ответы никогда не превращаются в 0.
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Optional, Sequence

from .extended_register_map import EXTENDED_REGISTERS, ExtendedRegister
from .register_decoder import decode_s_dword, decode_u_dword
from .normalizer import decode_s_word, decode_u_word


@dataclass(frozen=True)
class ExtendedValue:
    address: int
    parameter: str
    group: str
    raw: Optional[int]
    normalized: Optional[float]
    unit: str
    confidence: str
    error: Optional[str] = None

    @property
    def ok(self) -> bool:
        return self.normalized is not None and self.error is None


def decode_extended(address: int, words: Sequence[int], use_candidate: bool = False) -> ExtendedValue:
    reg: Optional[ExtendedRegister] = EXTENDED_REGISTERS.get(address)
    if reg is None:
        return ExtendedValue(address, "unknown", "unknown", None, None, "", "UNKNOWN", "unknown register")
    if len(words) != reg.register_count:
        return ExtendedValue(address, reg.name, reg.group, None, None, reg.unit, reg.confidence,
                             f"expected {reg.register_count} words, got {len(words)}")
    try:
        if reg.data_type in ("U_DWORD", "S_DWORD"):
            low, high = words[0], words[1]
            scale = reg.candidate_scale if use_candidate and reg.candidate_scale else reg.scale
            value = (decode_u_dword(low, high, scale=scale, word_order=reg.word_order)
                     if reg.data_type == "U_DWORD"
                     else decode_s_dword(low, high, scale=scale, word_order=reg.word_order))
            raw = low + (high << 16)
        else:
            word = words[0]
            raw = word
            decoded = (decode_s_word(word) if reg.is_signed else decode_u_word(word))
            scale = reg.candidate_scale if use_candidate and reg.candidate_scale else reg.scale
            value = decoded * scale - reg.offset
    except (ValueError, TypeError) as exc:
        return ExtendedValue(address, reg.name, reg.group, None, None, reg.unit, reg.confidence,
                             f"{type(exc).__name__}: {exc}")
    return ExtendedValue(address, reg.name, reg.group, raw, value, reg.unit, reg.confidence)


__all__ = ["ExtendedValue", "decode_extended"]
