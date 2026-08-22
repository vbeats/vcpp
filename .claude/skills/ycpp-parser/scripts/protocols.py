"""Built-in field layouts for YunKuaiChong and Shunfeng."""

from __future__ import annotations

from typing import Any

from .codecs import Cursor, ascii_decode, ascii_encode, bcd_decode, bcd_encode, cp56_decode, cp56_encode, encode_uint_le, hex_encode, uint_le
from .frame import encode_frame, parse_frame
from .registry import ProtocolDefinition, register_definition


def _byte(value: Any) -> int:
    if isinstance(value, str):
        text = value.strip().lower()
        return int(text, 0) if text.startswith("0x") else int(text)
    return int(value or 0)


def _field(cursor: Cursor, key: str, label: str, length: int, decoder, codec: str) -> dict:
    return cursor.field(key, label, length, decoder, codec)


def _enum_value(raw: bytes, labels: dict[int, str]) -> str:
    value = _byte(raw[0])
    return f"{value}[{labels.get(value, '未知')}]"


def _legacy_protocol_version(raw: bytes) -> str:
    value = _byte(raw[0])
    return f"{value / 10:.1f} (0x{value:02X})"


def _version3_decode(raw: bytes) -> str:
    return ".".join(str(part) for part in raw)


def _version3_encode(value: Any) -> bytes:
    if isinstance(value, (bytes, bytearray)):
        raw = bytes(value)
    else:
        parts = str(value).strip().lstrip("Vv").split(".")
        if len(parts) != 3:
            raise ValueError(f"三字节协议版本必须为 x.y.z: {value!r}")
        raw = bytes(int(part, 0) for part in parts)
    if len(raw) != 3:
        raise ValueError("三字节协议版本长度必须为3")
    return raw


def _login_legacy(body: bytes, frame_type: int):
    cursor = Cursor(body)
    fields = [
        _field(cursor, "device_no", "桩号", 7, bcd_decode, "bcd"),
        _field(cursor, "pile_type", "桩类型", 1, lambda x: _enum_value(x, {0: "直流桩", 1: "交流桩"}), "uint8-enum"),
        _field(cursor, "gun_count", "枪数量", 1, lambda x: _byte(x[0]), "uint8"),
        _field(cursor, "protocol_version", "通信协议版本", 1, _legacy_protocol_version, "uint8/10"),
        _field(cursor, "mcu_version", "mcu程序版本", 8, ascii_decode, "ascii"),
        _field(cursor, "network_type", "网络连接类型", 1, lambda x: _enum_value(x, {0: "SIM卡", 1: "LAN", 2: "WAN"}), "uint8-enum"),
        _field(cursor, "sim", "sim卡号", 10, bcd_decode, "bcd"),
        _field(cursor, "carrier", "运营商", 1, lambda x: _enum_value(x, {0: "移动", 2: "电信", 3: "联通", 4: "其他"}), "uint8-enum"),
    ]
    return "登录请求", "桩 -> 平台", fields, cursor.offset, cursor.warnings


def _login_v18(body: bytes, frame_type: int):
    cursor = Cursor(body)
    fields = [
        _field(cursor, "random_secret", "随机密钥", 88, ascii_decode, "ascii"),
        _field(cursor, "device_no", "桩号", 7, bcd_decode, "bcd"),
        _field(cursor, "pile_type", "桩类型", 1, lambda x: _enum_value(x, {0: "直流桩", 1: "交流桩"}), "uint8-enum"),
        _field(cursor, "gun_count", "枪数量", 1, lambda x: _byte(x[0]), "uint8"),
        _field(cursor, "protocol_version", "通信协议版本", 3, _version3_decode, "version3-bin"),
        _field(cursor, "mcu_version", "mcu程序版本", 8, ascii_decode, "ascii"),
        _field(cursor, "network_type", "网络连接类型", 1, lambda x: _enum_value(x, {0: "SIM卡", 1: "LAN", 2: "WAN"}), "uint8-enum"),
        _field(cursor, "sim", "sim卡号", 10, bcd_decode, "bcd"),
        _field(cursor, "carrier", "运营商", 1, lambda x: _enum_value(x, {0: "移动", 2: "电信", 3: "联通", 4: "其他"}), "uint8-enum"),
        _field(cursor, "token", "Token", 7, bcd_decode, "bcd"),
    ]
    return "登录请求", "桩 -> 平台", fields, cursor.offset, cursor.warnings


def _login_v21(body: bytes, frame_type: int):
    name, direction, fields, consumed, warnings = _login_v18(body, frame_type)
    cursor = Cursor(body[consumed:])
    extension_fields = [
        _field(cursor, "phone", "手机号码", 11, ascii_decode, "ascii"),
        _field(cursor, "supported_network", "支持网络制式", 1, lambda x: _byte(x[0]), "uint8"),
        _field(cursor, "current_network", "当前网络制式", 1, lambda x: _byte(x[0]), "uint8"),
        _field(cursor, "longitude", "经度", 4, lambda x: uint_le(x, 1000000), "uint32-le/1e6"),
        _field(cursor, "latitude", "纬度", 4, lambda x: uint_le(x, 1000000), "uint32-le/1e6"),
    ]
    for field in extension_fields:
        field["offset"] += consumed
    fields.extend(extension_fields)
    return name, direction, fields, consumed + cursor.offset, warnings + cursor.warnings


def _tariff_fields(cursor: Cursor) -> list[dict]:
    fields = []
    for tariff in ("尖", "峰", "平", "谷"):
        for suffix, scale in (("单价", 100000), ("电量", 10000), ("计损电量", 10000), ("金额", 10000)):
            key = {"尖": "jian", "峰": "feng", "平": "ping", "谷": "gu"}[tariff] + "_" + suffix
            fields.append(_field(cursor, key, tariff + suffix, 4,
                                 lambda x, s=scale: uint_le(x, s), f"uint32-le/{scale}"))
    return fields


def _trade_legacy(body: bytes, frame_type: int, meter_length: int, version: str):
    cursor = Cursor(body)
    fields = [
        _field(cursor, "trade_no", "交易流水号", 16, bcd_decode, "bcd"),
        _field(cursor, "device_no", "桩号", 7, bcd_decode, "bcd"),
        _field(cursor, "gun_no", "枪号", 1, lambda x: _byte(x[0]), "uint8"),
        _field(cursor, "start_time", "开始时间", 7, cp56_decode, "cp56time2a"),
        _field(cursor, "end_time", "结束时间", 7, cp56_decode, "cp56time2a"),
    ]
    if version in ("V1.7", "V1.8.1"):
        fields.extend([
            _field(cursor, "meter_no", "电表表号", 6, bcd_decode, "bcd"),
            _field(cursor, "meter_cipher", "电表密文", 34, lambda x: x.hex().upper(), "raw"),
            _field(cursor, "meter_protocol", "电表协议版本号", 2, lambda x: int.from_bytes(x, "little"), "uint16-le"),
            _field(cursor, "meter_encryption", "加密方式", 1, lambda x: _byte(x[0]), "uint8"),
        ])
    fields.extend(_tariff_fields(cursor))
    fields.extend([
        _field(cursor, "meter_start", "电表总起值", meter_length, lambda x: uint_le(x, 10000), f"uint{meter_length * 8}-le/10000"),
        _field(cursor, "meter_end", "电表总止值", meter_length, lambda x: uint_le(x, 10000), f"uint{meter_length * 8}-le/10000"),
        _field(cursor, "total_energy", "总电量", 4, lambda x: uint_le(x, 10000), "uint32-le/10000"),
        _field(cursor, "loss_energy", "计损总电量", 4, lambda x: uint_le(x, 10000), "uint32-le/10000"),
        _field(cursor, "amount", "消费金额", 4, lambda x: uint_le(x, 10000), "uint32-le/10000"),
        _field(cursor, "vin", "VIN", 17, ascii_decode, "ascii"),
        _field(cursor, "trade_flag", "交易标识", 1, lambda x: _byte(x[0]), "uint8"),
        _field(cursor, "trade_time", "交易时间", 7, cp56_decode, "cp56time2a"),
        _field(cursor, "stop_reason", "停止原因", 1, lambda x: _byte(x[0]), "uint8"),
        _field(cursor, "physical_card", "物理卡号", 8, lambda x: int.from_bytes(x, "little"), "uint64-le"),
    ])
    return "交易记录", "桩 -> 平台", fields, cursor.offset, cursor.warnings


def _trade_v21(body: bytes, frame_type: int):
    cursor = Cursor(body)
    fields = [
        _field(cursor, "trade_no", "交易流水号", 16, bcd_decode, "bcd"),
        _field(cursor, "device_no", "桩号", 7, bcd_decode, "bcd"),
        _field(cursor, "gun_no", "枪号", 1, lambda x: _byte(x[0]), "uint8"),
        _field(cursor, "start_time", "开始时间", 7, cp56_decode, "cp56time2a"),
        _field(cursor, "end_time", "结束时间", 7, cp56_decode, "cp56time2a"),
        _field(cursor, "meter_no", "电表表号", 6, bcd_decode, "bcd"),
        _field(cursor, "meter_cipher", "电表密文", 34, lambda x: x.hex().upper(), "raw"),
        _field(cursor, "meter_protocol", "电表协议版本号", 2, lambda x: int.from_bytes(x, "little"), "uint16-le"),
        _field(cursor, "meter_encryption", "加密方式", 1, lambda x: _byte(x[0]), "uint8"),
        _field(cursor, "meter_start", "电表总起值", 5, lambda x: uint_le(x, 10000), "uint40-le/10000"),
        _field(cursor, "meter_end", "电表总止值", 5, lambda x: uint_le(x, 10000), "uint40-le/10000"),
        _field(cursor, "total_energy", "总电量", 4, lambda x: uint_le(x, 10000), "uint32-le/10000"),
        _field(cursor, "loss_energy", "计损总电量", 4, lambda x: uint_le(x, 10000), "uint32-le/10000"),
        _field(cursor, "amount", "消费金额", 4, lambda x: uint_le(x, 10000), "uint32-le/10000"),
        _field(cursor, "vin", "VIN", 17, ascii_decode, "ascii"),
        _field(cursor, "trade_flag", "交易标识", 1, lambda x: _byte(x[0]), "uint8"),
        _field(cursor, "trade_time", "交易时间", 7, cp56_decode, "cp56time2a"),
        _field(cursor, "stop_reason", "停止原因", 1, lambda x: _byte(x[0]), "uint8"),
        _field(cursor, "physical_card", "物理卡号", 8, lambda x: int.from_bytes(x, "little"), "uint64-le"),
    ]
    if cursor.remaining:
        count = cursor.data[cursor.offset]
        fields.append(_field(cursor, "rate_count", "费率时段数量", 1, lambda x: _byte(x[0]), "uint8"))
        count = min(count, 48)
        for index in range(count):
            for suffix, scale in (("单价", 100000), ("电量", 10000), ("计损电量", 10000), ("金额", 10000)):
                fields.append(_field(cursor, f"rate_{index + 1}_{suffix}", f"第{index + 1}个费率{suffix}", 4,
                                     lambda x, s=scale: uint_le(x, s), f"uint32-le/{scale}"))
        if cursor.remaining >= 48 * 4:
            for index in range(48):
                fields.append(_field(cursor, f"period_energy_{index + 1}", f"第{index + 1}时段电量", 4,
                                     lambda x: uint_le(x, 10000), "uint32-le/10000"))
        elif cursor.remaining:
            warnings = [f"V2.1 费率尾部长度不足：剩余{cursor.remaining}字节，保留原始数据"]
            return "交易记录", "桩 -> 平台", fields, cursor.offset, warnings
    return "交易记录", "桩 -> 平台", fields, cursor.offset, cursor.warnings


def _shunfeng_0b(body: bytes, frame_type: int):
    cursor = Cursor(body)
    fields = [_field(cursor, "device_no", "桩编号", 7, bcd_decode, "bcd")]
    return "计费模型请求", "桩 -> 平台", fields, cursor.offset, cursor.warnings


def _shunfeng_0c(body: bytes, frame_type: int):
    cursor = Cursor(body)
    fields = [
        _field(cursor, "device_no", "桩编号", 7, bcd_decode, "bcd"),
        _field(cursor, "billing_model", "计费模型编号", 2, bcd_decode, "bcd"),
    ]
    for index in range(8):
        fields.append(_field(cursor, f"electric_rate_{index + 1}", f"电费费率{index + 1}", 4,
                             lambda x: uint_le(x, 100000), "uint32-le/100000"))
        fields.append(_field(cursor, f"service_rate_{index + 1}", f"服务费费率{index + 1}", 4,
                             lambda x: uint_le(x, 100000), "uint32-le/100000"))
    fields.append(_field(cursor, "loss_ratio", "计损比例", 1, lambda x: _byte(x[0]), "uint8"))
    for index in range(48):
        fields.append(_field(cursor, f"period_rate_{index + 1}", f"第{index + 1}时段费率号", 1,
                             lambda x: _byte(x[0]), "uint8"))
    return "计费模型", "平台 -> 桩", fields, cursor.offset, cursor.warnings


def _field_value(fields: dict, key: str, default: Any = None) -> Any:
    return fields.get(key, default)


def _encode_login_legacy(values: dict, version: str = "V1.5") -> bytes:
    protocol_byte = {"V1.4": 14, "V1.5": 15, "V1.6": 16, "V1.7": 17}[version]
    return b"".join([
        bcd_encode(_field_value(values, "device_no", "0"), 7),
        bytes((_byte(_field_value(values, "pile_type", 0)), _byte(_field_value(values, "gun_count", 1)),
               _byte(_field_value(values, "protocol_version", protocol_byte)))),
        ascii_encode(_field_value(values, "mcu_version", "SIM"), 8),
        bytes((_byte(_field_value(values, "network_type", 0)),)),
        bcd_encode(_field_value(values, "sim", "0"), 10),
        bytes((_byte(_field_value(values, "carrier", 0)),)),
    ])


def _encode_login_v18(values: dict, include_v21: bool = False) -> bytes:
    data = b"".join([
        ascii_encode(_field_value(values, "random_secret", "SIMULATED"), 88),
        bcd_encode(_field_value(values, "device_no", "0"), 7),
        bytes((_byte(_field_value(values, "pile_type", 0)), _byte(_field_value(values, "gun_count", 1)))),
        _version3_encode(_field_value(values, "protocol_version", "2.1.0" if include_v21 else "1.8.1")),
        ascii_encode(_field_value(values, "mcu_version", "SIM"), 8),
        bytes((_byte(_field_value(values, "network_type", 0)),)),
        bcd_encode(_field_value(values, "sim", "0"), 10),
        bytes((_byte(_field_value(values, "carrier", 0)),)),
        bcd_encode(_field_value(values, "token", "0"), 7),
    ])
    if include_v21:
        data += ascii_encode(_field_value(values, "phone", ""), 11)
        data += bytes((_byte(_field_value(values, "supported_network", 0)), _byte(_field_value(values, "current_network", 0))))
        data += encode_uint_le(_field_value(values, "longitude", 0), 4, 1000000)
        data += encode_uint_le(_field_value(values, "latitude", 0), 4, 1000000)
    return data


def _encode_trade(values: dict, meter_length: int, version: str) -> bytes:
    data = b"".join([
        bcd_encode(_field_value(values, "trade_no", "0"), 16),
        bcd_encode(_field_value(values, "device_no", "0"), 7),
        bytes((_byte(_field_value(values, "gun_no", 1)),)),
        cp56_encode(_field_value(values, "start_time")), cp56_encode(_field_value(values, "end_time")),
    ])
    if version in ("V1.7", "V1.8.1") or version == "V2.1":
        data += bcd_encode(_field_value(values, "meter_no", "0"), 6)
        data += hex_encode(_field_value(values, "meter_cipher", ""), 34)
        data += encode_uint_le(_field_value(values, "meter_protocol", 0), 2)
        data += bytes((_byte(_field_value(values, "meter_encryption", 0)),))
    if version != "V2.1":
        for tariff in ("jian", "feng", "ping", "gu"):
            for suffix, scale in (("单价", 100000), ("电量", 10000), ("计损电量", 10000), ("金额", 10000)):
                key = tariff + "_" + suffix
                data += encode_uint_le(_field_value(values, key, 0), 4, scale)
    data += b"".join([
        encode_uint_le(_field_value(values, "meter_start", 0), meter_length, 10000),
        encode_uint_le(_field_value(values, "meter_end", 0), meter_length, 10000),
        encode_uint_le(_field_value(values, "total_energy", 0), 4, 10000),
        encode_uint_le(_field_value(values, "loss_energy", 0), 4, 10000),
        encode_uint_le(_field_value(values, "amount", 0), 4, 10000),
        ascii_encode(_field_value(values, "vin", ""), 17),
        bytes((_byte(_field_value(values, "trade_flag", 1)),)),
        cp56_encode(_field_value(values, "trade_time")),
        bytes((_byte(_field_value(values, "stop_reason", 0)),)),
        encode_uint_le(_field_value(values, "physical_card", 0), 8),
    ])
    if version == "V2.1":
        count = min(max(_byte(_field_value(values, "rate_count", 0)), 0), 48)
        data += bytes((count,))
        for index in range(count):
            for suffix, scale in (("单价", 100000), ("电量", 10000), ("计损电量", 10000), ("金额", 10000)):
                data += encode_uint_le(_field_value(values, f"rate_{index + 1}_{suffix}", 0), 4, scale)
        for index in range(48):
            data += encode_uint_le(_field_value(values, f"period_energy_{index + 1}", 0), 4, 10000)
    return data


def _billing_identity(cursor: Cursor) -> list[dict]:
    return [
        _field(cursor, "device_no", "桩编号", 7, bcd_decode, "bcd"),
        _field(cursor, "billing_model", "计费模型编号", 2, bcd_decode, "bcd"),
    ]


def _ykc_billing_validate(body: bytes, frame_type: int):
    cursor = Cursor(body)
    fields = _billing_identity(cursor)
    if frame_type == 0x06:
        fields.append(_field(cursor, "validation_result", "验证结果", 1, lambda x: _byte(x[0]), "uint8"))
    name = "计费模型验证请求" if frame_type == 0x05 else "计费模型验证应答"
    direction = "桩 -> 平台" if frame_type == 0x05 else "平台 -> 桩"
    return name, direction, fields, cursor.offset, cursor.warnings


def _ykc_billing_request(body: bytes, frame_type: int):
    cursor = Cursor(body)
    fields = [_field(cursor, "device_no", "桩编号", 7, bcd_decode, "bcd")]
    return "计费模型请求", "桩 -> 平台", fields, cursor.offset, cursor.warnings


def _ykc_billing_model_legacy(body: bytes, frame_type: int):
    cursor = Cursor(body)
    fields = _billing_identity(cursor)
    for tariff, key in (("尖", "jian"), ("峰", "feng"), ("平", "ping"), ("谷", "gu")):
        fields.append(_field(cursor, f"{key}_electric_rate", f"{tariff}电费费率", 4,
                             lambda x: uint_le(x, 100000), "uint32-le/100000"))
        fields.append(_field(cursor, f"{key}_service_rate", f"{tariff}服务费率", 4,
                             lambda x: uint_le(x, 100000), "uint32-le/100000"))
    fields.append(_field(cursor, "loss_ratio", "计损比例", 1, lambda x: _byte(x[0]), "uint8"))
    for index in range(48):
        fields.append(_field(cursor, f"period_rate_{index + 1}", f"第{index + 1}时段费率号", 1,
                             lambda x: _byte(x[0]), "uint8"))
    return "计费模型应答", "平台 -> 桩", fields, cursor.offset, cursor.warnings


def _ykc_billing_model_v21(body: bytes, frame_type: int):
    cursor = Cursor(body)
    fields = _billing_identity(cursor)
    count_field = _field(cursor, "rate_count", "费率数量", 1, lambda x: _byte(x[0]), "uint8")
    fields.append(count_field)
    count = count_field["value"] if isinstance(count_field["value"], int) else 0
    if count > 48:
        cursor.warnings.append(f"费率数量超出协议上限48：{count}")
        count = 48
    for index in range(count):
        fields.append(_field(cursor, f"electric_rate_{index + 1}", f"第{index + 1}个电费费率", 4,
                             lambda x: uint_le(x, 100000), "uint32-le/100000"))
        fields.append(_field(cursor, f"service_rate_{index + 1}", f"第{index + 1}个服务费率", 4,
                             lambda x: uint_le(x, 100000), "uint32-le/100000"))
    fields.append(_field(cursor, "loss_ratio", "计损比例", 1, lambda x: _byte(x[0]), "uint8"))
    for index in range(48):
        fields.append(_field(cursor, f"period_rate_{index + 1}", f"第{index + 1}时段费率号", 1,
                             lambda x: _byte(x[0]), "uint8"))
    return "计费模型应答", "平台 -> 桩", fields, cursor.offset, cursor.warnings


def _encode_ykc_billing_validate(values: dict, response: bool = False) -> bytes:
    data = bcd_encode(_field_value(values, "device_no", "0"), 7)
    data += bcd_encode(_field_value(values, "billing_model", "0000"), 2)
    if response:
        data += bytes((_byte(_field_value(values, "validation_result", 0)),))
    return data


def _encode_ykc_billing_request(values: dict) -> bytes:
    return bcd_encode(_field_value(values, "device_no", "0"), 7)


def _encode_ykc_billing_model_legacy(values: dict) -> bytes:
    data = _encode_ykc_billing_validate(values)
    for key in ("jian", "feng", "ping", "gu"):
        data += encode_uint_le(_field_value(values, f"{key}_electric_rate", 0), 4, 100000)
        data += encode_uint_le(_field_value(values, f"{key}_service_rate", 0), 4, 100000)
    data += bytes((_byte(_field_value(values, "loss_ratio", 0)),))
    return data + bytes(_byte(_field_value(values, f"period_rate_{index + 1}", 0)) for index in range(48))


def _encode_ykc_billing_model_v21(values: dict) -> bytes:
    data = _encode_ykc_billing_validate(values)
    count = min(max(_byte(_field_value(values, "rate_count", 0)), 0), 48)
    data += bytes((count,))
    for index in range(count):
        data += encode_uint_le(_field_value(values, f"electric_rate_{index + 1}", 0), 4, 100000)
        data += encode_uint_le(_field_value(values, f"service_rate_{index + 1}", 0), 4, 100000)
    data += bytes((_byte(_field_value(values, "loss_ratio", 0)),))
    return data + bytes(_byte(_field_value(values, f"period_rate_{index + 1}", 1)) for index in range(48))


def _encode_shunfeng_0b(values: dict) -> bytes:
    return bcd_encode(_field_value(values, "device_no", "0"), 7)


def _encode_shunfeng_0c(values: dict) -> bytes:
    data = bcd_encode(_field_value(values, "device_no", "0"), 7) + bcd_encode(_field_value(values, "billing_model", "0100"), 2)
    for index in range(8):
        data += encode_uint_le(_field_value(values, f"electric_rate_{index + 1}", 0), 4, 100000)
        data += encode_uint_le(_field_value(values, f"service_rate_{index + 1}", 0), 4, 100000)
    data += bytes((_byte(_field_value(values, "loss_ratio", 0)),))
    return data + bytes(_byte(_field_value(values, f"period_rate_{index + 1}", 1)) for index in range(48))


def _make_decoder(fn):
    return fn


def _register() -> None:
    billing_names = {0x05: "计费模型验证请求", 0x06: "计费模型验证应答", 0x09: "计费模型请求", 0x0A: "计费模型应答"}
    billing_directions = {0x05: "桩 -> 平台", 0x06: "平台 -> 桩", 0x09: "桩 -> 平台", 0x0A: "平台 -> 桩"}
    for version in ("V1.4", "V1.5", "V1.6", "V1.7"):
        trade_type = 0x3F if version == "V1.4" else 0x3B if version in ("V1.5", "V1.6") else 0x3D
        frame_names = {0x01: "登录请求", trade_type: "交易记录", **billing_names}
        decoders = {
            0x01: _login_legacy,
            0x05: _ykc_billing_validate,
            0x06: _ykc_billing_validate,
            0x09: _ykc_billing_request,
            0x0A: _ykc_billing_model_legacy,
            trade_type: lambda body, frame_type, v=version: _trade_legacy(body, frame_type, 4 if v == "V1.4" else 5, v),
        }
        encoders = {
            0x01: lambda values, v=version: _encode_login_legacy(values, v),
            0x05: _encode_ykc_billing_validate,
            0x06: lambda values: _encode_ykc_billing_validate(values, True),
            0x09: _encode_ykc_billing_request,
            0x0A: _encode_ykc_billing_model_legacy,
            trade_type: lambda values, v=version: _encode_trade(values, 4 if v == "V1.4" else 5, v),
        }
        directions = {0x01: "桩 -> 平台", trade_type: "桩 -> 平台", **billing_directions}
        register_definition(ProtocolDefinition("ykc", version, frame_names, decoders, encoders, directions))

    v18_names = {0x01: "登录请求", 0x3D: "交易记录", **billing_names}
    v18_decoders = {
        0x01: _login_v18,
        0x3D: lambda body, frame_type: _trade_legacy(body, frame_type, 5, "V1.8.1"),
        0x05: _ykc_billing_validate,
        0x06: _ykc_billing_validate,
        0x09: _ykc_billing_request,
        0x0A: _ykc_billing_model_legacy,
    }
    v18_encoders = {
        0x01: _encode_login_v18,
        0x3D: lambda values: _encode_trade(values, 5, "V1.8.1"),
        0x05: _encode_ykc_billing_validate,
        0x06: lambda values: _encode_ykc_billing_validate(values, True),
        0x09: _encode_ykc_billing_request,
        0x0A: _encode_ykc_billing_model_legacy,
    }
    register_definition(ProtocolDefinition("ykc", "V1.8.1", v18_names, v18_decoders, v18_encoders,
                                           {0x01: "桩 -> 平台", 0x3D: "桩 -> 平台", **billing_directions}))

    v21_decoders = {
        0x01: _login_v21,
        0x3D: _trade_v21,
        0x05: _ykc_billing_validate,
        0x06: _ykc_billing_validate,
        0x09: _ykc_billing_request,
        0x0A: _ykc_billing_model_v21,
    }
    v21_encoders = {
        0x01: lambda values: _encode_login_v18(values, True),
        0x3D: lambda values: _encode_trade(values, 5, "V2.1"),
        0x05: _encode_ykc_billing_validate,
        0x06: lambda values: _encode_ykc_billing_validate(values, True),
        0x09: _encode_ykc_billing_request,
        0x0A: _encode_ykc_billing_model_v21,
    }
    register_definition(ProtocolDefinition("ykc", "V2.1", {0x01: "登录请求", 0x3D: "交易记录", **billing_names},
                                           v21_decoders, v21_encoders,
                                           {0x01: "桩 -> 平台", 0x3D: "桩 -> 平台", **billing_directions}))
    register_definition(ProtocolDefinition("shunfeng", "V1.5", {0x0B: "计费模型请求", 0x0C: "计费模型"},
                                           {0x0B: _shunfeng_0b, 0x0C: _shunfeng_0c},
                                           {0x0B: _encode_shunfeng_0b, 0x0C: _encode_shunfeng_0c},
                                           {0x0B: "桩 -> 平台", 0x0C: "平台 -> 桩"}))


_register()


def decode_body(protocol: str, version: str, body: bytes, frame_type: int):
    from .registry import get_definition
    definition = get_definition(protocol, version)
    decoder = definition.decoders.get(frame_type)
    if decoder is None:
        return "未知帧", "未知", [], 0, [f"未注册帧类型 0x{frame_type:02X}，保留完整消息体"]
    return decoder(body, frame_type)


def encode_body(protocol: str, version: str, frame_type: int, values: dict) -> bytes:
    from .registry import get_definition
    definition = get_definition(protocol, version)
    encoder = definition.encoders.get(frame_type)
    if encoder is None:
        raise ValueError(f"未注册生成器：{protocol}/{version}/0x{frame_type:02X}")
    return encoder(values)


def frame_type_for(protocol: str, version: str, request: str | None, explicit: Any = None) -> int:
    if explicit is not None:
        text = str(explicit).strip().lower()
        return int(text, 16) if text.startswith("0x") else int(text)
    text = (request or "").lower().replace(" ", "")
    if "登录" in text:
        return 0x01
    if "交易" in text or "账单" in text:
        return 0x3F if version == "V1.4" else 0x3B if version in ("V1.5", "V1.6") else 0x3D
    if protocol == "ykc" and ("计费模型" in text or "费率" in text):
        return 0x09 if "请求" in text and not any(word in text for word in ("应答", "下发", "模型内容")) else 0x0A
    if protocol == "shunfeng" and ("0b" in text or "请求" in text):
        return 0x0B
    if protocol == "shunfeng" and ("0c" in text or "计费模型" in text):
        return 0x0C
    raise ValueError("无法从请求识别帧类型，请指定 --frame-type")
