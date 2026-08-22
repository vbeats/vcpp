"""Command line entry point for the ycpp-parser skill."""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

if __package__ in (None, ""):
    sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
    from scripts import protocols  # noqa: F401 - importing registers built-in definitions
    from scripts.frame import FrameError, encode_frame, normalize_hex, parse_frame
    from scripts.protocols import encode_body, frame_type_for
    from scripts.registry import canonical_protocol, canonical_version, get_definition, supported_protocols, supported_versions
    from scripts.report import to_json, to_report
else:
    from . import protocols  # noqa: F401 - importing registers built-in definitions
    from .frame import FrameError, encode_frame, normalize_hex, parse_frame
    from .protocols import encode_body, frame_type_for
    from .registry import canonical_protocol, canonical_version, get_definition, supported_protocols, supported_versions
    from .report import to_json, to_report


VERSION_CHOICES = ["V1.4", "V1.5", "V1.6", "V1.7", "V1.8.1", "V2.1"]


def _add_common(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--protocol", "-p", help="协议：ykc/云快充或 shunfeng/瞬峰")
    parser.add_argument("--version", "-v", help="协议版本，例如 V1.7")
    parser.add_argument("--format", choices=("report", "json"), default="report")
    parser.add_argument("--emoji", action="store_true", help="在中文报告中加入方向 emoji")


def _read_hex(args: argparse.Namespace) -> str:
    if args.hex is not None and args.input is not None:
        raise FrameError("--hex 与 --input 只能二选一")
    if args.hex is not None:
        return args.hex
    if args.input is not None:
        return Path(args.input).read_text(encoding="utf-8")
    raise FrameError("请提供 --hex 或 --input")


def _resolve_protocol_version(protocol: str | None, version: str | None, interactive: bool = True) -> tuple[str, str]:
    protocol = canonical_protocol(protocol)
    version = canonical_version(version)
    if protocol is None and interactive and sys.stdin.isatty():
        try:
            protocol = input("请输入协议（ykc/shunfeng）：").strip()
        except EOFError:
            protocol = None
        protocol = canonical_protocol(protocol)
    if version is None and interactive and sys.stdin.isatty():
        choices = supported_versions(protocol) or VERSION_CHOICES
        print("请选择协议版本：" + ", ".join(choices), file=sys.stderr)
        try:
            version = input("版本：").strip()
        except EOFError:
            version = None
        version = canonical_version(version)
    if protocol is None:
        raise FrameError(json.dumps({"error": "protocol_required", "message": "请指定协议", "choices": supported_protocols()}, ensure_ascii=False))
    if version is None:
        choices = supported_versions(protocol) or VERSION_CHOICES
        raise FrameError(json.dumps({"error": "version_required", "message": "请指定协议版本", "choices": choices}, ensure_ascii=False))
    return protocol, version


def command_parse(args: argparse.Namespace) -> int:
    protocol, version = _resolve_protocol_version(args.protocol, args.version, interactive=args.format != "json")
    raw = _read_hex(args)
    definition = get_definition(protocol, version)
    result = parse_frame(normalize_hex(raw), protocol, version, protocols.decode_body)
    if result.encrypt_flag != 0:
        result.frame_name = definition.frame_names.get(result.frame_type, "未知帧")
        result.direction = definition.directions.get(result.frame_type, "未知")
    output = to_json(result.as_dict()) if args.format == "json" else to_report(result.as_dict(), args.emoji)
    print(output)
    return 0


def _parse_fields(value: str | None) -> dict:
    if not value:
        return {}
    path = Path(value)
    if path.exists():
        return json.loads(path.read_text(encoding="utf-8"))
    return json.loads(value)


def _infer_protocol(request: str | None, explicit: str | None) -> str | None:
    if explicit:
        return canonical_protocol(explicit)
    text = (request or "").lower()
    if any(item in text for item in ("云快充", "ykc", "yunkuaichong")):
        return "ykc"
    if any(item in text for item in ("瞬峰", "顺峰", "shunfeng")):
        return "shunfeng"
    return None


def _infer_version(request: str | None, explicit: str | None) -> str | None:
    if explicit:
        return canonical_version(explicit)
    text = request or ""
    match = re.search(r"(?:版本|v)?\s*(\d+(?:\.\d+){1,2})", text, re.IGNORECASE)
    return canonical_version(match.group(1)) if match else None


def command_generate(args: argparse.Namespace) -> int:
    protocol = _infer_protocol(args.request, args.protocol)
    version = _infer_version(args.request, args.version)
    protocol, version = _resolve_protocol_version(protocol, version, interactive=args.format != "json")
    fields = _parse_fields(args.fields_json)
    frame_type = frame_type_for(protocol, version, args.request, args.frame_type)
    body = protocols.encode_body(protocol, version, frame_type, fields)
    sequence = int(args.sequence, 0) if isinstance(args.sequence, str) else args.sequence
    encrypt_flag = int(args.encrypt_flag, 0)
    if encrypt_flag != 0:
        raise FrameError("不支持在没有会话密钥时生成加密消息体，请使用 --encrypt-flag 0")
    frame = encode_frame(protocol, version, frame_type, body, sequence=sequence,
                         encrypt_flag=encrypt_flag, send_time=args.send_time)
    result = parse_frame(frame, protocol, version, protocols.decode_body)
    result.warnings.insert(0, "这是模拟生成报文，字段未提供时使用零值或占位值")
    result_dict = result.as_dict()
    result_dict["generated"] = True
    result_dict["request"] = args.request or ""
    result_dict["generated_hex"] = frame.hex().upper()
    output = to_json(result_dict) if args.format == "json" else "生成报文：" + frame.hex().upper() + "\n" + to_report(result_dict, args.emoji)
    print(output)
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="云快充/瞬峰协议报文解析和模拟生成工具")
    subparsers = parser.add_subparsers(dest="command", required=True)
    parse_parser = subparsers.add_parser("parse", help="解析十六进制报文")
    _add_common(parse_parser)
    parse_parser.add_argument("--hex", help="十六进制报文，支持空格、换行和0x前缀")
    parse_parser.add_argument("--input", help="从文本文件读取十六进制报文")
    parse_parser.set_defaults(handler=command_parse)
    generate_parser = subparsers.add_parser("generate", help="生成模拟报文")
    _add_common(generate_parser)
    generate_parser.add_argument("--request", help="自然语言请求，例如：云快充1.7版本的交易记录")
    generate_parser.add_argument("--frame-type", help="帧类型，例如 0x3D")
    generate_parser.add_argument("--fields-json", help="JSON 字符串或 JSON 文件路径")
    generate_parser.add_argument("--sequence", default="0", help="序列号，默认0")
    generate_parser.add_argument("--encrypt-flag", default="0", help="加密标志，当前只生成明文")
    generate_parser.add_argument("--send-time", help="V2.1 CP56Time2a 时间，例如 2026-01-01 00:00:00")
    generate_parser.set_defaults(handler=command_generate)
    return parser


def main(argv: list[str] | None = None) -> int:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    if hasattr(sys.stderr, "reconfigure"):
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    parser = build_parser()
    args = parser.parse_args(argv)
    try:
        if args.command == "generate" and not args.request and not args.frame_type:
            raise FrameError("生成模式请提供 --request 或 --frame-type")
        return args.handler(args)
    except (FrameError, ValueError, json.JSONDecodeError) as exc:
        message = str(exc)
        try:
            json.loads(message)
            print(message)
        except json.JSONDecodeError:
            print(f"错误：{message}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
