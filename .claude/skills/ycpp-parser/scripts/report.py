"""Stable Chinese text and JSON reports."""

from __future__ import annotations

import json


def to_json(result: dict) -> str:
    return json.dumps(result, ensure_ascii=False, indent=2)


def to_report(result: dict, emoji: bool = False) -> str:
    envelope = result["envelope"]
    frame = result["frame"]
    crc = result["crc"]
    protocol_name = {"ykc": "云快充", "shunfeng": "瞬峰"}.get(result["protocol"], result["protocol"])
    direction = frame.get("direction", "未知")
    heading = f"【{protocol_name} {result['version']}】"
    if emoji:
        heading += " ⬆️" if direction.strip().startswith("桩") else " ⬇️" if direction.strip().startswith("平台") else ""
    lines = [
        heading + f" {frame.get('name', '未知帧')}",
        f"协议：{protocol_name}",
        f"版本：{result['version']}",
        f"方向：{direction}",
        f"帧类型：{envelope['frame_type']}",
        f"序列号：{envelope['sequence']}（原始：{envelope['sequence_raw_hex']}）",
        f"加密标志：0x{envelope['encrypt_flag']:02X}",
    ]
    if envelope.get("send_time"):
        lines.append(f"发送时间：{envelope['send_time']}")
    for field in result.get("fields", []):
        value = field.get("value")
        lines.append(f"{field['label']}：{value}")
    lines.append(f"CRC：传输={crc.get('transmitted_hex', '')}，计算={crc.get('computed_hex', '')}，匹配={'是' if crc.get('match') else '否'}")
    if result.get("unknown_body_hex"):
        lines.append(f"未解析消息体：{result['unknown_body_hex']}")
    for warning in result.get("warnings", []):
        lines.append(f"警告：{warning}")
    for error in result.get("errors", []):
        lines.append(f"错误：{error}")
    return "\n".join(lines)
