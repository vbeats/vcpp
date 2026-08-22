"""Protocol registry: add new vendors and versions without changing the frame layer."""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Callable, Any


Decoder = Callable[[bytes, int], tuple[str, str, list[dict], int, list[str]]]
Encoder = Callable[[dict], bytes]


@dataclass
class ProtocolDefinition:
    protocol: str
    version: str
    frame_names: dict[int, str]
    decoders: dict[int, Decoder]
    encoders: dict[int, Encoder]
    directions: dict[int, str] = field(default_factory=dict)


_DEFINITIONS: dict[tuple[str, str], ProtocolDefinition] = {}
_ALIASES: dict[str, str] = {
    "云快充": "ykc", "ykc": "ykc", "yunkuaichong": "ykc", "yun kuaichong": "ykc",
    "瞬峰": "shunfeng", "顺峰": "shunfeng", "shunfeng": "shunfeng",
}


def canonical_protocol(value: str | None) -> str | None:
    if value is None:
        return None
    key = str(value).strip().lower()
    return _ALIASES.get(key, key)


def canonical_version(value: str | None) -> str | None:
    if value is None:
        return None
    text = str(value).strip().upper().replace("版本", "")
    aliases = {
        "14": "V1.4", "1.4": "V1.4", "V14": "V1.4",
        "15": "V1.5", "1.5": "V1.5", "V15": "V1.5",
        "16": "V1.6", "1.6": "V1.6", "V16": "V1.6",
        "17": "V1.7", "1.7": "V1.7", "V17": "V1.7",
        "18": "V1.8.1", "1.8": "V1.8.1", "1.8.1": "V1.8.1", "V18": "V1.8.1", "V1.8.1": "V1.8.1",
        "21": "V2.1", "2.1": "V2.1", "2.1.0": "V2.1", "V21": "V2.1", "V2.1.0": "V2.1",
    }
    return aliases.get(text, text if text.startswith("V") else f"V{text}")


def register_definition(definition: ProtocolDefinition) -> None:
    _DEFINITIONS[(definition.protocol, definition.version)] = definition


def get_definition(protocol: str, version: str) -> ProtocolDefinition:
    key = (canonical_protocol(protocol), canonical_version(version))
    try:
        return _DEFINITIONS[key]
    except KeyError as exc:
        supported = ", ".join(f"{p}/{v}" for p, v in sorted(_DEFINITIONS))
        raise ValueError(f"不支持协议版本：{key[0]}/{key[1]}；当前支持：{supported}") from exc


def supported_versions(protocol: str | None = None) -> list[str]:
    protocol = canonical_protocol(protocol)
    values = {version for (item_protocol, version) in _DEFINITIONS if protocol is None or item_protocol == protocol}
    return sorted(values)


def supported_protocols() -> list[str]:
    return sorted({protocol for protocol, _ in _DEFINITIONS})


def register_decoder(protocol: str, version: str, frame_type: int, decoder: Decoder, name: str = "未知帧") -> None:
    definition = get_definition(protocol, version)
    definition.decoders[frame_type] = decoder
    definition.frame_names[frame_type] = name


def register_encoder(protocol: str, version: str, frame_type: int, encoder: Encoder) -> None:
    get_definition(protocol, version).encoders[frame_type] = encoder
