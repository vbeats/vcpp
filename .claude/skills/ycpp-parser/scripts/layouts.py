"""Declarative codecs for protocol frames with fixed and repeated fields."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Iterable

from .codecs import Cursor, ascii_decode, ascii_encode, bcd_decode, bcd_encode, cp56_decode, cp56_encode, encode_uint_le, hex_encode, uint_le


@dataclass(frozen=True)
class FieldSpec:
    key: str
    label: str
    length: int
    codec: str = "uint"
    scale: int = 1
    offset: float = 0
    default: Any = None


def field(key: str, label: str, length: int, codec: str = "uint", scale: int = 1,
          offset: float = 0, default: Any = None) -> FieldSpec:
    return FieldSpec(key, label, length, codec, scale, offset, default)


def _byte(value: Any) -> int:
    if isinstance(value, str):
        text = value.strip().lower()
        return int(text, 0) if text.startswith("0x") else int(text)
    return int(value or 0)


def _decode_value(raw: bytes, spec: FieldSpec) -> Any:
    if spec.codec == "bcd":
        return bcd_decode(raw)
    if spec.codec == "ascii":
        return ascii_decode(raw)
    if spec.codec == "raw":
        return raw.hex().upper()
    if spec.codec == "cp56":
        return cp56_decode(raw)
    value = uint_le(raw, spec.scale)
    if spec.offset:
        value = round(value + spec.offset, 6)
    return value


def _encode_value(value: Any, spec: FieldSpec) -> bytes:
    if value is None:
        value = spec.default
    if spec.codec == "bcd":
        return bcd_encode("0" if value is None else value, spec.length)
    if spec.codec == "ascii":
        return ascii_encode("" if value is None else value, spec.length)
    if spec.codec == "raw":
        return hex_encode("" if value is None else value, spec.length)
    if spec.codec == "cp56":
        return cp56_encode(value)
    numeric = float(value or 0) - spec.offset
    return encode_uint_le(numeric, spec.length, spec.scale)


def _decode_fields(cursor: Cursor, specs: Iterable[FieldSpec], key_prefix: str = "",
                   label_prefix: str = "") -> list[dict]:
    fields: list[dict] = []
    for spec in specs:
        fields.append(cursor.field(
            key_prefix + spec.key,
            label_prefix + spec.label,
            spec.length,
            lambda raw, item=spec: _decode_value(raw, item),
            _codec_name(spec),
        ))
    return fields


def _encode_fields(values: dict, specs: Iterable[FieldSpec], key_prefix: str = "") -> bytes:
    return b"".join(_encode_value(values.get(key_prefix + spec.key), spec) for spec in specs)


def _codec_name(spec: FieldSpec) -> str:
    if spec.codec in ("bcd", "ascii", "raw", "cp56"):
        return "cp56time2a" if spec.codec == "cp56" else spec.codec
    suffix = f"/{spec.scale}" if spec.scale != 1 else ""
    if spec.offset:
        suffix += f"{spec.offset:+g}"
    return f"uint{spec.length * 8}-le{suffix}"


def fixed_codec(name: str, direction: str, specs: Iterable[FieldSpec]):
    specs = tuple(specs)

    def decode(body: bytes, frame_type: int):
        cursor = Cursor(body)
        fields = _decode_fields(cursor, specs)
        return name, direction, fields, cursor.offset, cursor.warnings

    def encode(values: dict) -> bytes:
        return _encode_fields(values, specs)

    return decode, encode


def counted_codec(name: str, direction: str, prefix: Iterable[FieldSpec], count: FieldSpec,
                  items: Iterable[FieldSpec], list_key: str, max_items: int,
                  suffix: Iterable[FieldSpec] = ()):
    prefix, items, suffix = tuple(prefix), tuple(items), tuple(suffix)

    def decode(body: bytes, frame_type: int):
        cursor = Cursor(body)
        fields = _decode_fields(cursor, prefix)
        count_field = _decode_fields(cursor, (count,))[0]
        fields.append(count_field)
        item_count = count_field["value"] if isinstance(count_field["value"], int) else 0
        if item_count > max_items:
            cursor.warnings.append(f"{count.label}超出协议上限{max_items}：{item_count}")
            item_count = max_items
        for index in range(item_count):
            fields.extend(_decode_fields(cursor, items, f"{list_key}_{index + 1}_", f"第{index + 1}项"))
        fields.extend(_decode_fields(cursor, suffix))
        return name, direction, fields, cursor.offset, cursor.warnings

    def encode(values: dict) -> bytes:
        entries = values.get(list_key)
        if entries is not None:
            if not isinstance(entries, list):
                raise ValueError(f"{list_key} 必须是数组")
            item_count = len(entries)
        else:
            item_count = _byte(values.get(count.key, count.default or 0))
            entries = []
        if item_count > max_items:
            raise ValueError(f"{count.label}不能超过{max_items}")
        data = _encode_fields(values, prefix)
        data += _encode_value(item_count, count)
        for index in range(item_count):
            entry = entries[index] if index < len(entries) else values
            prefix_key = "" if index < len(entries) else f"{list_key}_{index + 1}_"
            data += _encode_fields(entry, items, prefix_key)
        return data + _encode_fields(values, suffix)

    return decode, encode


def remaining_codec(name: str, direction: str, prefix: Iterable[FieldSpec], items: Iterable[FieldSpec],
                    list_key: str, max_items: int):
    prefix, items = tuple(prefix), tuple(items)
    item_size = sum(spec.length for spec in items)

    def decode(body: bytes, frame_type: int):
        cursor = Cursor(body)
        fields = _decode_fields(cursor, prefix)
        if item_size and cursor.remaining % item_size:
            cursor.warnings.append(f"重复数据长度不是{item_size}字节的整数倍，尾部保留原始数据")
        item_count = min(cursor.remaining // item_size, max_items)
        for index in range(item_count):
            fields.extend(_decode_fields(cursor, items, f"{list_key}_{index + 1}_", f"第{index + 1}项"))
        return name, direction, fields, cursor.offset, cursor.warnings

    def encode(values: dict) -> bytes:
        entries = values.get(list_key, [])
        if not isinstance(entries, list):
            raise ValueError(f"{list_key} 必须是数组")
        if len(entries) > max_items:
            raise ValueError(f"{list_key} 不能超过{max_items}项")
        data = _encode_fields(values, prefix)
        for entry in entries:
            data += _encode_fields(entry, items)
        return data

    return decode, encode


def variable_tail_codec(name: str, direction: str, prefix: Iterable[FieldSpec], length: FieldSpec,
                        content_key: str, content_label: str, content_codec: str = "ascii",
                        suffix: Iterable[FieldSpec] = (), max_length: int = 255):
    prefix, suffix = tuple(prefix), tuple(suffix)

    def decode(body: bytes, frame_type: int):
        cursor = Cursor(body)
        fields = _decode_fields(cursor, prefix)
        length_field = _decode_fields(cursor, (length,))[0]
        fields.append(length_field)
        content_length = length_field["value"] if isinstance(length_field["value"], int) else 0
        content_length = min(content_length, max_length)
        dynamic = FieldSpec(content_key, content_label, content_length, content_codec)
        fields.extend(_decode_fields(cursor, (dynamic,)))
        fields.extend(_decode_fields(cursor, suffix))
        return name, direction, fields, cursor.offset, cursor.warnings

    def encode(values: dict) -> bytes:
        content = values.get(content_key, "")
        if content_codec == "ascii":
            raw = str(content).encode("ascii", errors="replace")
        else:
            text = str(content).replace(" ", "")
            if text.lower().startswith("0x"):
                text = text[2:]
            raw = bytes.fromhex(text)
        if len(raw) > max_length:
            raise ValueError(f"{content_label}不能超过{max_length}字节")
        supplied = values.get(length.key)
        if supplied is not None and _byte(supplied) != len(raw):
            raise ValueError(f"{length.label}与{content_label}实际长度不一致")
        data = _encode_fields(values, prefix) + _encode_value(len(raw), length) + raw
        return data + _encode_fields(values, suffix)

    return decode, encode
