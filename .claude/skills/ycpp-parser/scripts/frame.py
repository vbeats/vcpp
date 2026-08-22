"""Envelope parsing and encoding for legacy and YunKuaiChong V2.1 frames."""

from __future__ import annotations

import re
from dataclasses import dataclass
from typing import Callable, Optional

from .codecs import cp56_decode, cp56_encode
from .crc import crc_report, crc_wire_bytes


class FrameError(ValueError):
    pass


def normalize_hex(value: str) -> bytes:
    text = re.sub(r"0x", "", value or "", flags=re.IGNORECASE)
    text = re.sub(r"[\s,_:-]+", "", text)
    if not text:
        raise FrameError("未提供十六进制报文")
    if len(text) % 2:
        raise FrameError("十六进制报文字符数必须为偶数")
    if not re.fullmatch(r"[0-9a-fA-F]+", text):
        raise FrameError("十六进制报文包含非法字符")
    return bytes.fromhex(text)


@dataclass(frozen=True)
class EnvelopeSpec:
    kind: str
    length_size: int
    length_byteorder: str
    has_send_time: bool = False


LEGACY = EnvelopeSpec("legacy", 1, "big", False)
V21 = EnvelopeSpec("v2.1", 2, "big", True)


@dataclass
class ParsedFrame:
    protocol: str
    version: str
    spec: EnvelopeSpec
    raw: bytes
    length: int
    sequence: int
    sequence_raw: bytes
    send_time: Optional[str]
    send_time_raw: bytes
    encrypt_flag: int
    frame_type: int
    body: bytes
    crc: dict
    fields: list[dict]
    unknown_body_hex: str
    warnings: list[str]
    errors: list[str]
    frame_name: str = "未知帧"
    direction: str = "未知"
    builder: Optional[Callable] = None

    def as_dict(self) -> dict:
        return {
            "schema_version": "1.0",
            "protocol": self.protocol,
            "version": self.version,
            "raw_hex": self.raw.hex().upper(),
            "envelope": {
                "kind": self.spec.kind,
                "length": self.length,
                "length_size": self.spec.length_size,
                "sequence": self.sequence,
                "sequence_raw_hex": self.sequence_raw.hex().upper(),
                "send_time": self.send_time,
                "send_time_raw_hex": self.send_time_raw.hex().upper(),
                "encrypt_flag": self.encrypt_flag,
                "frame_type": f"0x{self.frame_type:02X}",
                "body_hex": self.body.hex().upper(),
            },
            "frame": {
                "name": self.frame_name,
                "direction": self.direction,
            },
            "fields": self.fields,
            "unknown_body_hex": self.unknown_body_hex,
            "crc": self.crc,
            "warnings": self.warnings,
            "errors": self.errors,
        }


def spec_for(protocol: str, version: str) -> EnvelopeSpec:
    return V21 if protocol == "ykc" and version == "V2.1" else LEGACY


def parse_envelope(data: bytes, protocol: str, version: str) -> ParsedFrame:
    spec = spec_for(protocol, version)
    errors: list[str] = []
    warnings: list[str] = []
    if len(data) < 1 + spec.length_size + 4 + 2:
        raise FrameError("报文长度不足，无法读取帧头和 CRC")
    if data[0] != 0x68:
        raise FrameError(f"起始标志错误：期望68，实际{data[0]:02X}")
    length_start = 1
    length_end = length_start + spec.length_size
    length = int.from_bytes(data[length_start:length_end], spec.length_byteorder)
    total = 1 + spec.length_size + length + 2
    if len(data) != total:
        if len(data) < total:
            raise FrameError(f"报文被截断：长度字段要求{total}字节，实际{len(data)}字节")
        raise FrameError(f"报文长度不匹配：长度字段要求{total}字节，实际{len(data)}字节；如输入包含多帧请逐帧解析")
    frame_data = data[length_end:length_end + length]
    transmitted = data[length_end + length:length_end + length + 2]
    if spec.has_send_time:
        if length < 11:
            raise FrameError("V2.1 长度字段小于固定数据区11字节")
        sequence_raw = frame_data[:2]
        sequence = int.from_bytes(sequence_raw, "little")
        send_time_raw = frame_data[2:9]
        try:
            send_time = cp56_decode(send_time_raw)
        except Exception as exc:
            send_time = None
            warnings.append(f"发送时间解码失败：{exc}")
        encrypt_flag = frame_data[9]
        frame_type = frame_data[10]
        body = frame_data[11:]
        coverage = "sequence+send_time+encrypt_flag+frame_type+body"
    else:
        if length < 4:
            raise FrameError("传统帧长度字段小于固定数据区4字节")
        sequence_raw = frame_data[:2]
        sequence = int.from_bytes(sequence_raw, "little")
        send_time_raw = b""
        send_time = None
        encrypt_flag = frame_data[2]
        frame_type = frame_data[3]
        body = frame_data[4:]
        coverage = "sequence+encrypt_flag+frame_type+body"
    crc = crc_report(frame_data, transmitted)
    crc["coverage"] = coverage
    crc["coverage_hex"] = frame_data.hex().upper()
    crc["wire_order"] = "低字节在前"
    if not crc["match"]:
        warnings.append("CRC 校验不匹配")
    if encrypt_flag:
        warnings.append("消息体标记为密文，未提供会话密钥，仅解析帧头并校验原文 CRC")
    return ParsedFrame(protocol, version, spec, data[:total], length, sequence,
                       sequence_raw, send_time, send_time_raw, encrypt_flag,
                       frame_type, body, crc, [], "", warnings, errors)


def encode_frame(protocol: str, version: str, frame_type: int, body: bytes,
                 sequence: int = 0, encrypt_flag: int = 0,
                 send_time: Optional[str] = None) -> bytes:
    spec = spec_for(protocol, version)
    sequence_raw = int(sequence).to_bytes(2, "little", signed=False)
    if spec.has_send_time:
        time_raw = cp56_encode(send_time)
        frame_data = sequence_raw + time_raw + bytes((encrypt_flag & 0xFF, frame_type & 0xFF)) + body
    else:
        frame_data = sequence_raw + bytes((encrypt_flag & 0xFF, frame_type & 0xFF)) + body
    length = len(frame_data)
    if length >= 1 << (8 * spec.length_size):
        raise FrameError(f"数据区长度超出{spec.length_size}字节长度字段范围")
    return b"\x68" + length.to_bytes(spec.length_size, spec.length_byteorder) + frame_data + crc_wire_bytes(frame_data)


def parse_frame(data: bytes | str, protocol: str, version: str, decoder=None) -> ParsedFrame:
    raw = normalize_hex(data) if isinstance(data, str) else data
    result = parse_envelope(raw, protocol, version)
    if decoder is not None and result.encrypt_flag == 0:
        name, direction, fields, consumed, warnings = decoder(protocol, version, result.body, result.frame_type)
        result.frame_name = name
        result.direction = direction
        result.fields = fields
        result.unknown_body_hex = result.body[consumed:].hex().upper()
        result.warnings.extend(warnings)
    elif result.encrypt_flag != 0:
        result.unknown_body_hex = result.body.hex().upper()
    return result
