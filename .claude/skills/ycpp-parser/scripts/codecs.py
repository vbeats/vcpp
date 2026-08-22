"""Small, dependency-free binary codecs used by protocol definitions."""

from __future__ import annotations

from datetime import datetime
from typing import Callable, Any


class Cursor:
    def __init__(self, data: bytes):
        self.data = data
        self.offset = 0
        self.warnings: list[str] = []

    @property
    def remaining(self) -> int:
        return len(self.data) - self.offset

    def take(self, length: int, label: str) -> bytes:
        start = self.offset
        if length < 0:
            raise ValueError("字段长度不能为负数")
        end = min(start + length, len(self.data))
        raw = self.data[start:end]
        self.offset = end
        if len(raw) != length:
            self.warnings.append(f"字段“{label}”截断：需要{length}字节，实际{len(raw)}字节")
        return raw

    def field(self, key: str, label: str, length: int, decoder: Callable[[bytes], Any], codec: str) -> dict:
        offset = self.offset
        raw = self.take(length, label)
        try:
            value = decoder(raw) if len(raw) == length else f"<截断: {raw.hex().upper()}>"
        except Exception as exc:
            self.warnings.append(f"字段“{label}”解码失败：{exc}")
            value = f"<无法解码: {raw.hex().upper()}>"
        return {
            "key": key,
            "label": label,
            "value": value,
            "raw_hex": raw.hex().upper(),
            "offset": offset,
            "length": length,
            "codec": codec,
        }


def bcd_decode(data: bytes) -> str:
    digits = []
    for byte in data:
        high, low = byte >> 4, byte & 0x0F
        if high > 9 or low > 9:
            raise ValueError(f"非法 BCD: {byte:02X}")
        digits.extend((str(high), str(low)))
    return "".join(digits)


def bcd_encode(value: Any, length: int) -> bytes:
    text = "" if value is None else str(value).strip()
    if text.lower().startswith("0x"):
        text = text[2:]
    if not text.isdigit():
        raise ValueError(f"BCD 字段必须是数字: {value!r}")
    width = length * 2
    if len(text) > width:
        raise ValueError(f"BCD 字段超长，需要不超过{width}位: {value!r}")
    text = text.zfill(width)
    return bytes((int(text[i]) << 4) | int(text[i + 1]) for i in range(0, width, 2))


def ascii_decode(data: bytes) -> str:
    return data.rstrip(b"\x00").decode("ascii", errors="replace")


def ascii_encode(value: Any, length: int) -> bytes:
    text = "" if value is None else str(value)
    raw = text.encode("ascii", errors="replace")
    if len(raw) > length:
        raise ValueError(f"ASCII 字段超长，需要不超过{length}字节: {value!r}")
    return raw.ljust(length, b"\x00")


def uint_le(data: bytes, scale: int = 1) -> Any:
    value = int.from_bytes(data, "little", signed=False)
    return value if scale == 1 else round(value / scale, 5)


def encode_uint_le(value: Any, length: int, scale: int = 1) -> bytes:
    numeric = float(value or 0) * scale
    integer = int(round(numeric))
    if integer < 0 or integer >= 1 << (length * 8):
        raise ValueError(f"BIN 字段超出{length}字节范围: {value!r}")
    return integer.to_bytes(length, "little")


def cp56_decode(data: bytes) -> str:
    if len(data) != 7:
        raise ValueError("CP56Time2a 必须为7字节")
    millis = int.from_bytes(data[:2], "little")
    if millis > 59999:
        raise ValueError(f"毫秒值超出范围: {millis}")
    second, millisecond = divmod(millis, 1000)
    minute = data[2] & 0x3F
    hour = data[3] & 0x1F
    day = data[4] & 0x1F
    month = data[5] & 0x0F
    year = 2000 + (data[6] & 0x7F)
    value = datetime(year, month, day, hour, minute, second, millisecond * 1000)
    return value.isoformat(sep=" ")


def cp56_encode(value: Any) -> bytes:
    if value in (None, ""):
        value = "2026-01-01 00:00:00"
    if isinstance(value, datetime):
        date_time = value
    else:
        text = str(value).replace("T", " ")
        date_time = datetime.fromisoformat(text)
    if not 2000 <= date_time.year <= 2099:
        raise ValueError(f"CP56Time2a 年份仅支持 2000~2099: {date_time.year}")
    millis = date_time.second * 1000 + date_time.microsecond // 1000
    return bytes((millis & 0xFF, millis >> 8, date_time.minute & 0x3F,
                  date_time.hour & 0x1F, date_time.day & 0x1F,
                  date_time.month & 0x0F, date_time.year % 100))


def hex_encode(value: Any, length: int) -> bytes:
    text = "" if value is None else str(value).replace(" ", "")
    if text.lower().startswith("0x"):
        text = text[2:]
    if len(text) % 2 or any(char not in "0123456789abcdefABCDEF" for char in text):
        raise ValueError(f"十六进制字段无效: {value!r}")
    raw = bytes.fromhex(text)
    if len(raw) > length:
        raise ValueError(f"BIN 原始字段超长，需要不超过{length}字节")
    return raw.ljust(length, b"\x00")
