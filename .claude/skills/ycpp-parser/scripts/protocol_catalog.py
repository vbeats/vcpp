"""Complete documented frame catalog and simulation codecs."""

from __future__ import annotations

from .layouts import counted_codec, field as f, fixed_codec, remaining_codec, variable_tail_codec
from .registry import get_definition


D = f("device_no", "桩编号", 7, "bcd", default="0")
G = f("gun_no", "枪号", 1, "bcd", default="1")
GU = f("gun_no", "枪号", 1, default=1)
TR = f("trade_no", "交易流水号", 16, "bcd", default="0")
CARD_BCD = f("logical_card", "逻辑卡号", 8, "bcd", default="0")
CARD_UINT = f("physical_card", "物理卡号", 8, default=0)


def _register(version: str, frame_type: int, name: str, direction: str, codec) -> None:
    definition = get_definition("ykc", version)
    decoder, encoder = codec
    definition.frame_names[frame_type] = name
    definition.directions[frame_type] = direction
    definition.decoders[frame_type] = decoder
    definition.encoders[frame_type] = encoder


def _alias(version: str, frame_type: int, name: str, direction: str, source_type: int) -> None:
    definition = get_definition("ykc", version)
    source_decoder = definition.decoders[source_type]

    def decoder(body: bytes, actual_type: int):
        _, _, fields, consumed, warnings = source_decoder(body, actual_type)
        return name, direction, fields, consumed, warnings

    definition.frame_names[frame_type] = name
    definition.directions[frame_type] = direction
    definition.decoders[frame_type] = decoder
    definition.encoders[frame_type] = definition.encoders[source_type]


def _fixed(version: str, frame_type: int, name: str, direction: str, specs) -> None:
    _register(version, frame_type, name, direction, fixed_codec(name, direction, specs))


def _register_common(version: str, *, modern_start: bool, v21: bool = False) -> None:
    _fixed(version, 0x02, "登录认证应答", "平台 -> 桩", (D, f("login_result", "登录结果", 1)))
    _fixed(version, 0x03, "充电桩心跳包", "桩 -> 平台", (D, G, f("gun_status", "枪状态", 1)))
    _fixed(version, 0x04, "心跳包应答", "平台 -> 桩", (D, G, f("heartbeat_ack", "心跳应答", 1)))
    _fixed(version, 0x12, "读取实时监测数据", "平台 -> 桩", (D, G))

    realtime = [
        TR, D, G, f("status", "状态", 1, default=2), f("gun_returned", "枪是否归位", 1),
        f("plugged", "是否插枪", 1), f("output_voltage", "输出电压", 2, scale=10),
        f("output_current", "输出电流", 2, scale=10), f("gun_temperature", "枪线温度", 1, offset=-50, default=-50),
        f("gun_code", "枪线编码", 8), f("soc", "SOC", 1),
        f("battery_max_temperature", "电池组最高温度", 1, offset=-50, default=-50),
        f("charged_minutes", "累计充电时间", 2), f("remaining_minutes", "剩余时间", 2),
        f("charged_energy", "充电度数", 4, scale=10000),
        f("loss_charged_energy", "计损充电度数", 4, scale=10000),
        f("charged_amount", "已充金额", 4, scale=10000), f("hardware_fault_bits", "硬件故障", 2, "raw"),
    ]
    if v21:
        realtime.extend((
            f("charger_body_temperature", "桩体温度", 1, offset=-50, default=-50),
            f("smoke_status", "烟感状态", 1), f("meter_value", "电表示值", 5, scale=10000),
        ))
    _fixed(version, 0x13, "上传实时监测数据", "桩 -> 平台", realtime)

    _fixed(version, 0x15, "充电握手", "桩 -> 平台", (
        TR, D, G, f("bms_protocol_version", "BMS通信协议版本号", 3, "raw", default="010100"),
        f("bms_battery_type", "BMS电池类型", 1), f("bms_rated_capacity", "BMS额定容量", 2, scale=10),
        f("bms_rated_voltage", "BMS额定总电压", 2, scale=10), f("bms_manufacturer", "BMS生产厂商", 4, "ascii"),
        f("bms_pack_serial", "BMS电池组序号", 4), f("bms_production_year", "BMS生产年份", 1, offset=1985, default=1985),
        f("bms_production_month", "BMS生产月份", 1), f("bms_production_day", "BMS生产日期", 1),
        f("bms_charge_count", "BMS充电次数", 3), f("bms_ownership", "BMS产权标识", 1),
        f("reserved", "预留位", 1), f("bms_vin", "BMS车辆识别码", 17, "ascii"),
        f("bms_software_version", "BMS软件版本号", 8, "raw"),
    ))
    _fixed(version, 0x17, "参数配置", "桩 -> 平台", (
        TR, D, G, f("bms_max_cell_charge_voltage", "BMS单体最高允许充电电压", 2, scale=100),
        f("bms_max_charge_current", "BMS最高允许充电电流", 2, scale=10, offset=-400, default=-400),
        f("bms_nominal_energy", "BMS标称总能量", 2, scale=10), f("bms_max_total_voltage", "BMS最高允许总电压", 2, scale=10),
        f("bms_max_temperature", "BMS最高允许温度", 1, offset=-50, default=-50), f("bms_soc", "BMS荷电状态", 2, scale=10),
        f("bms_current_voltage", "BMS当前电池电压", 2), f("charger_max_voltage", "电桩最高输出电压", 2, scale=10),
        f("charger_min_voltage", "电桩最低输出电压", 2, scale=10),
        f("charger_max_current", "电桩最大输出电流", 2, scale=10, offset=-400, default=-400),
        f("charger_min_current", "电桩最小输出电流", 2, scale=10, offset=-400, default=-400),
    ))
    _fixed(version, 0x19, "充电结束", "桩 -> 平台", (
        TR, D, G, f("bms_stop_soc", "BMS中止SOC", 1), f("bms_min_cell_voltage", "BMS单体最低电压", 2, scale=100),
        f("bms_max_cell_voltage", "BMS单体最高电压", 2, scale=100),
        f("bms_min_temperature", "BMS最低温度", 1, offset=-50, default=-50),
        f("bms_max_temperature", "BMS最高温度", 1, offset=-50, default=-50),
        f("charged_minutes", "累计充电时间", 2), f("charger_output_energy", "电桩输出能量", 2, scale=10),
        f("charger_id", "充电机编号", 4),
    ))
    _fixed(version, 0x1B, "错误报文", "桩 -> 平台", (TR, D, G, f("error_status_bits", "错误状态位", 8, "raw")))
    _fixed(version, 0x1D, "充电阶段BMS中止", "桩 -> 平台", (
        TR, D, G, f("bms_stop_reason", "BMS中止原因", 1, "raw"),
        f("bms_stop_fault_reason", "BMS中止故障原因", 2, "raw"), f("bms_stop_error_reason", "BMS中止错误原因", 1, "raw"),
    ))
    _fixed(version, 0x21, "充电阶段充电机中止", "桩 -> 平台", (
        TR, D, G, f("charger_stop_reason", "充电机中止原因", 1, "raw"),
        f("charger_stop_fault_reason", "充电机中止故障原因", 2, "raw"), f("charger_stop_error_reason", "充电机中止错误原因", 1, "raw"),
    ))
    _fixed(version, 0x23, "BMS需求与充电机输出", "桩 -> 平台", (
        TR, D, G, f("bms_voltage_request", "BMS电压需求", 2, scale=10),
        f("bms_current_request", "BMS电流需求", 2, scale=10, offset=-400, default=-400),
        f("bms_charge_mode", "BMS充电模式", 1), f("bms_voltage_measurement", "BMS电压测量值", 2, scale=10),
        f("bms_current_measurement", "BMS电流测量值", 2, scale=10, offset=-400, default=-400),
        f("bms_max_cell_voltage_group", "BMS最高单体电压及组号", 2, "raw"), f("bms_soc", "BMS当前SOC", 1),
        f("bms_remaining_minutes", "BMS剩余充电时间", 2), f("charger_output_voltage", "电桩输出电压", 2, scale=10),
        f("charger_output_current", "电桩输出电流", 2, scale=10, offset=-400, default=-400),
        f("charged_minutes", "累计充电时间", 2),
    ))
    _fixed(version, 0x25, "BMS信息", "桩 -> 平台", (
        TR, D, G, f("max_cell_voltage_index", "最高单体电压编号", 1, offset=1, default=1),
        f("max_battery_temperature", "最高电池温度", 1, offset=-50, default=-50),
        f("max_temperature_probe_index", "最高温度探针编号", 1, offset=1, default=1),
        f("min_battery_temperature", "最低电池温度", 1, offset=-50, default=-50),
        f("min_temperature_probe_index", "最低温度探针编号", 1, offset=1, default=1),
        f("bms_status_bits", "BMS状态位", 2, "raw"),
    ))

    if modern_start:
        start_request, start_confirm, start_command, start_reply = 0xA5, 0xA6, 0xA8, 0xA7
    else:
        start_request, start_confirm, start_command, start_reply = 0x31, 0x32, 0x34, 0x33
    _fixed(version, start_request, "主动申请启动充电", "桩 -> 平台", (
        D, G, f("start_method", "启动方式", 1, default=1), f("password_required", "是否需要密码", 1),
        f("account_or_physical_card", "账号或物理卡号", 8), f("input_password_md5", "输入密码MD5", 16, "raw"),
        f("vin", "VIN", 17, "ascii"),
    ))
    confirm_fields = [TR, D, G, CARD_BCD, f("account_balance", "账户余额", 4, scale=100)]
    if modern_start:
        confirm_fields.append(f("current_max_power", "当前允许最大功率", 2))
    if v21:
        confirm_fields.extend((f("soc_limit", "SOC限制", 1), f("energy_limit", "充电电量限制", 4, scale=10000)))
    confirm_fields.extend((f("auth_result", "鉴权结果", 1, default=1), f("failure_reason", "失败原因", 1, "bcd")))
    _fixed(version, start_confirm, "平台确认启动充电", "平台 -> 桩", confirm_fields)
    command_fields = [TR, D, G, CARD_BCD, CARD_UINT, f("account_balance", "账户余额", 4, scale=100)]
    if modern_start:
        command_fields.append(f("current_max_power", "当前允许最大功率", 2))
    if v21:
        command_fields.extend((f("soc_limit", "SOC限制", 1), f("energy_limit", "充电电量限制", 4, scale=10000)))
    _fixed(version, start_command, "平台远程控制启机", "平台 -> 桩", command_fields)
    _fixed(version, start_reply, "远程启动充电命令回复", "桩 -> 平台", (
        TR, D, G, f("start_result", "启动结果", 1, "bcd", default="1"), f("failure_reason", "失败原因", 1),
    ))

    _fixed(version, 0x36, "运营平台远程停机", "平台 -> 桩", (D, G))
    _fixed(version, 0x35, "远程停机命令回复", "桩 -> 平台", (
        D, G, f("stop_result", "停止结果", 1, "bcd", default="1"), f("failure_reason", "失败原因", 1),
    ))
    _fixed(version, 0x40, "交易记录确认", "平台 -> 桩", (TR, f("confirm_result", "确认结果", 1)))
    _fixed(version, 0x42, "远程账户余额更新", "平台 -> 桩", (D, G, CARD_UINT, f("updated_balance", "修改后账户金额", 4, scale=100)))
    _fixed(version, 0x41, "余额更新应答", "桩 -> 平台", (D, CARD_UINT, f("update_result", "修改结果", 1)))

    _register(version, 0x44, "离线卡数据同步", "平台 -> 桩", counted_codec(
        "离线卡数据同步", "平台 -> 桩", (D,), f("card_count", "下发卡个数", 1),
        (CARD_BCD, CARD_UINT), "cards", 15))
    _fixed(version, 0x43, "离线卡数据同步应答", "桩 -> 平台", (
        D, f("save_result", "保存结果", 1, default=1), f("failure_reason", "失败原因", 1),
    ))
    for frame_type, name, count_key, max_items in (
        (0x46, "离线卡数据清除", "clear_count", 24), (0x48, "离线卡数据查询", "query_count", 26),
    ):
        _register(version, frame_type, name, "平台 -> 桩", counted_codec(
            name, "平台 -> 桩", (D,), f(count_key, "卡数量", 1), (CARD_UINT,), "cards", max_items))
    _register(version, 0x45, "离线卡数据清除应答", "桩 -> 平台", remaining_codec(
        "离线卡数据清除应答", "桩 -> 平台", (D,),
        (CARD_UINT, f("clear_result", "清除结果", 1, default=1), f("failure_reason", "失败原因", 1, "raw")), "cards", 24))
    _register(version, 0x47, "离线卡数据查询应答", "桩 -> 平台", remaining_codec(
        "离线卡数据查询应答", "桩 -> 平台", (D,),
        (CARD_UINT, f("query_result", "查询结果", 1, default=1)), "cards", 26))

    _fixed(version, 0x56, "对时设置", "平台 -> 桩", (D, f("current_time", "当前时间", 7, "cp56")))
    _fixed(version, 0x55, "对时设置应答", "桩 -> 平台", (D, f("current_time", "当前时间", 7, "cp56")))
    _fixed(version, 0x57, "计费模型设置应答", "桩 -> 平台", (D, f("set_result", "设置结果", 1, default=1)))
    _fixed(version, 0x61, "地锁数据上送", "桩 -> 平台", (
        D, GU, f("lock_status", "车位锁状态", 1), f("parking_status", "车位状态", 1),
        f("lock_battery_percent", "地锁电量状态", 1), f("alarm_status", "报警状态", 1), f("reserved", "预留", 4, "raw"),
    ))
    _fixed(version, 0x62, "遥控地锁升降命令", "平台 -> 桩", (D, GU, f("lock_command", "地锁命令", 1), f("reserved", "预留", 4, "raw")))
    _fixed(version, 0x63, "地锁控制返回", "桩 -> 平台", (D, GU, f("lock_control_result", "地锁控制结果", 1), f("reserved", "预留", 4, "raw")))
    _fixed(version, 0x92, "远程重启", "平台 -> 桩", (D, f("execution_control", "执行控制", 1, default=2)))
    _fixed(version, 0x91, "远程重启应答", "桩 -> 平台", (D, f("set_result", "设置结果", 1, default=1)))
    _fixed(version, 0x93, "远程更新应答", "桩 -> 平台", (D, f("update_status", "升级状态", 1)))


def _register_parallel(version: str) -> None:
    _fixed(version, 0xA1, "主动申请并充充电", "桩 -> 平台", (
        D, G, f("start_method", "启动方式", 1, default=1), f("password_required", "是否需要密码", 1),
        f("account_or_physical_card", "账号或物理卡号", 8), f("input_password_md5", "输入密码MD5", 16, "raw"),
        f("vin", "VIN", 17, "ascii"), f("primary_secondary", "主辅枪标记", 1), f("parallel_id", "并充序号", 6, "bcd"),
    ))
    _fixed(version, 0xA2, "平台确认并充启动", "平台 -> 桩", (
        TR, D, G, CARD_BCD, f("account_balance", "账户余额", 4, scale=100), f("auth_result", "鉴权结果", 1, default=1),
        f("failure_reason", "失败原因", 1, "bcd"), f("parallel_id", "并充序号", 6, "bcd"),
    ))
    _fixed(version, 0xA4, "平台远程控制并充启机", "平台 -> 桩", (
        TR, D, G, CARD_BCD, CARD_UINT, f("account_balance", "账户余额", 4, scale=100), f("parallel_id", "并充序号", 6, "bcd"),
    ))
    _fixed(version, 0xA3, "远程并充启机命令回复", "桩 -> 平台", (
        TR, D, G, f("start_result", "启动结果", 1, "bcd", default="1"), f("failure_reason", "失败原因", 1),
        f("primary_secondary", "主辅枪标记", 1), f("parallel_id", "并充序号", 6, "bcd"),
    ))


def _register_legacy_settings(version: str) -> None:
    _fixed(version, 0x52, "充电桩工作参数设置", "平台 -> 桩", (
        D, f("work_allowed", "是否允许工作", 1), f("max_output_power_percent", "最大允许输出功率", 1, default=100),
    ))
    _fixed(version, 0x51, "工作参数设置应答", "桩 -> 平台", (D, f("set_result", "设置结果", 1, default=1)))
    _alias(version, 0x58, "计费模型设置", "平台 -> 桩", 0x0A)
    _fixed(version, 0x94, "远程更新", "平台 -> 桩", (
        D, f("charger_type", "桩型号", 1, default=1), f("charger_power", "桩功率", 2),
        f("server_address", "升级服务器地址", 16, "ascii"), f("server_port", "升级服务器端口", 2),
        f("username", "用户名", 16, "ascii"), f("password", "密码", 16, "ascii"), f("file_path", "文件路径", 32, "ascii"),
        f("execution_control", "执行控制", 1, default=2), f("download_timeout_minutes", "下载超时", 1),
    ))


def _register_v18_settings(version: str, *, v21: bool = False) -> None:
    _fixed(version, 0x52, "功率修改", "平台 -> 桩", (
        D, G, f("max_power", "最大功率", 2), f("response_priority", "响应优先级", 1), f("limit_minutes", "限制时长", 2),
    ))
    _fixed(version, 0x51, "功率修改应答", "桩 -> 平台", (D, G, f("set_result", "设置结果", 1, default=1)))
    _fixed(version, 0x60, "默认最大功率下发", "平台 -> 桩", (
        D, G, f("default_max_power", "默认最大功率", 2), f("start_time", "开始时间", 7, "cp56"), f("end_time", "结束时间", 7, "cp56"),
    ))
    _fixed(version, 0x59, "默认最大功率下发应答", "桩 -> 平台", (D, G, f("set_result", "设置结果", 1, default=1)))
    update_fields = [
        D, f("charger_type", "桩型号", 1, default=1), f("charger_power", "桩功率", 2),
        f("server_address", "升级服务器地址", 16, "ascii"), f("server_port", "升级服务器端口", 2),
        f("username", "用户名", 16, "ascii"), f("password", "密码", 16, "ascii"), f("file_path", "文件路径", 32, "ascii"),
    ]
    if v21:
        update_fields.extend((f("file_name", "文件名", 32, "ascii"), f("execution_control", "执行控制", 1, default=2),
                              f("download_timeout_minutes", "下载超时", 1), f("file_md5", "文件MD5", 32, "ascii")))
    else:
        update_fields.extend((f("execution_control", "执行控制", 1, default=2), f("download_timeout_minutes", "下载超时", 1)))
    _fixed(version, 0x94, "远程更新", "平台 -> 桩", update_fields)
    _alias(version, 0x58, "计费模型设置", "平台 -> 桩", 0x0A)


def _register_v21_only() -> None:
    version = "V2.1"
    _register(version, 0x02, "登录认证应答", "平台 -> 桩", variable_tail_codec(
        "登录认证应答", "平台 -> 桩", (D, f("login_result", "登录结果", 1)), f("key_length", "最新密钥长度", 1),
        "latest_key", "最新密钥", "ascii", max_length=255))
    _fixed(version, 0x49, "设备故障上送确认", "平台 -> 桩", (D, G, f("receive_ack", "接收确认", 1)))
    fault = (D, G, f("fault_type", "故障类型", 1), f("fault_code", "故障代码", 2), f("fault_time", "故障时间", 7, "cp56"))
    _fixed(version, 0x50, "设备故障上送", "桩 -> 平台", fault)
    _fixed(version, 0x4A, "故障复位上送确认", "平台 -> 桩", (D, G, f("receive_ack", "接收确认", 1)))
    _fixed(version, 0x4B, "设备故障复位上送", "桩 -> 平台", fault[:-1] + (f("fault_reset_time", "故障复位时间", 7, "cp56"),))
    _fixed(version, 0x4D, "交易记录召唤", "平台 -> 桩", (TR, D, G))
    _fixed(version, 0x4C, "交易记录召唤确认", "桩 -> 平台", (
        TR, D, G, f("recall_result", "召唤结果", 1), f("failure_reason", "失败原因", 1),
    ))
    _fixed(version, 0x4F, "充电机启动完成", "桩 -> 平台", (
        TR, D, G, f("start_result", "启动结果", 1), f("failure_reason", "失败原因", 2, "raw"),
        f("current_meter_value", "当前电表示值", 5, scale=10000), f("max_allowed_total_voltage", "最高允许总电压", 2, scale=10),
        f("bms_protocol_version", "BMS协议版本", 3, "raw"), f("bms_battery_type", "BMS电池类型", 1),
        f("bms_rated_capacity", "BMS额定容量", 2, scale=10), f("bms_rated_voltage", "BMS额定电压", 2, scale=10),
        f("bms_manufacturer", "BMS生产厂商", 4, "ascii"), f("bms_pack_serial", "BMS电池组序号", 4),
        f("bms_production_year", "BMS生产年份", 1), f("bms_production_month", "BMS生产月份", 1),
        f("bms_production_day", "BMS生产日期", 1), f("bms_charge_count", "BMS充电次数", 3),
        f("bms_ownership", "BMS产权标识", 1), f("vehicle_vin", "车辆VIN", 17, "ascii"), f("bms_software_version", "BMS软件版本", 8, "raw"),
        f("bms_max_cell_charge_voltage", "BMS单体最高允许充电电压", 2, scale=100),
        f("bms_max_charge_current", "BMS最高允许充电电流", 2), f("bms_nominal_energy", "BMS标称总能量", 2, scale=10),
        f("bms_max_total_voltage", "BMS最高允许总电压", 2, scale=10),
        f("bms_max_temperature", "BMS最高允许温度", 1, offset=-50, default=-50), f("bms_soc", "BMS荷电状态", 2, scale=10),
        f("bms_current_voltage", "BMS当前电压", 2), f("charger_max_voltage", "电桩最高输出电压", 2, scale=10),
        f("charger_min_voltage", "电桩最低输出电压", 2, scale=10), f("charger_max_current", "电桩最大输出电流", 2),
        f("charger_min_current", "电桩最小输出电流", 2), f("bms_ready", "BMS就绪状态", 1, default=0xFF),
        f("charger_ready", "充电机就绪状态", 1, default=0xFF),
    ))
    _fixed(version, 0x4E, "充电机启动完成应答", "平台 -> 桩", (TR, D, G, f("start_result", "启动结果", 1)))
    _fixed(version, 0xA9, "充电桩上报VIN", "桩 -> 平台", (D, G, f("vin", "VIN", 17, "ascii"), f("current_time", "当前时间", 7, "cp56")))
    _fixed(version, 0xAA, "VIN上报回复", "平台 -> 桩", (D, G, f("auth_result", "鉴权结果", 1, default=1), TR))

    _register(version, 0x5B, "二维码设置", "平台 -> 桩", variable_tail_codec(
        "二维码设置", "平台 -> 桩", (D, GU, f("qr_format", "二维码格式", 1)), f("qr_length", "二维码长度", 1),
        "qr_content", "二维码内容", "ascii", max_length=255))
    _fixed(version, 0x5A, "二维码设置应答", "桩 -> 平台", (D, GU, f("set_result", "设置结果", 1, default=1)))
    _fixed(version, 0x5D, "平台连接设置", "平台 -> 桩", (
        D, f("platform_domain", "平台域名", 64, "ascii"), f("platform_address", "平台地址", 15, "ascii"), f("platform_port", "平台端口", 2),
    ))
    _fixed(version, 0x5C, "平台连接设置应答", "桩 -> 平台", (D, f("set_result", "设置结果", 1, default=1)))
    _fixed(version, 0x5F, "参数设置", "平台 -> 桩", (
        D, G, f("plug_and_charge_switch", "即插即充开关", 1, default=1), f("auth_timeout_seconds", "鉴权超时", 1),
        f("offline_charge_timeout_seconds", "离线充电超时", 1),
    ))
    _fixed(version, 0x5E, "参数设置应答", "桩 -> 平台", (D, G, f("set_result", "设置结果", 1, default=1)))
    _register(version, 0x96, "密钥更新", "平台 -> 桩", variable_tail_codec(
        "密钥更新", "平台 -> 桩", (D,), f("key_length", "密钥长度", 1), "latest_key", "最新密钥", "ascii",
        suffix=(f("execution_control", "执行控制", 1, default=2),), max_length=255))
    _fixed(version, 0x95, "密钥更新应答", "桩 -> 平台", (D, f("set_result", "设置结果", 1, default=1)))
    _fixed(version, 0x98, "日志召唤", "平台 -> 桩", (
        D, f("upload_server_address", "上传服务器地址", 16, "ascii"), f("upload_server_port", "上传服务器端口", 2),
        f("username", "用户名", 16, "ascii"), f("password", "密码", 16, "ascii"), f("upload_server_path", "上传服务器路径", 64, "ascii"),
        f("log_start_time", "日志开始时间", 7, "cp56"), f("log_end_time", "日志结束时间", 7, "cp56"),
        f("log_type", "日志类型", 1), f("charger_log_path", "充电桩日志路径", 64, "ascii"), f("upload_timeout_minutes", "上传超时", 1),
    ))
    _fixed(version, 0x97, "日志召唤应答", "桩 -> 平台", (
        D, f("recall_result", "召唤结果", 1), f("failure_reason", "失败原因", 1),
        f("log_file_count", "日志文件数量", 2), f("log_encoding_flags", "日志编码标志", 2, "raw"),
    ))


def _register_supplements() -> None:
    _register("V1.5", 0xF0, "二维码前缀下发", "平台 -> 桩", variable_tail_codec(
        "二维码前缀下发", "平台 -> 桩", (D, f("qr_format", "二维码格式", 1)), f("prefix_length", "二维码前缀长度", 1),
        "qr_prefix", "二维码前缀", "ascii", max_length=200))
    _fixed("V1.5", 0xF1, "二维码前缀下发应答", "桩 -> 平台", (D, f("set_result", "下发结果", 1)))
    _register("V1.6", 0x9C, "平台下发二维码", "平台 -> 桩", variable_tail_codec(
        "平台下发二维码", "平台 -> 桩", (GU,), f("qr_length", "二维码URL长度", 2), "qr_url", "二维码URL", "ascii", max_length=200))
    _register("V1.6", 0x9E, "平台按桩下发二维码", "平台 -> 桩", variable_tail_codec(
        "平台按桩下发二维码", "平台 -> 桩", (D, GU), f("qr_length", "二维码URL长度", 2), "qr_url", "二维码URL", "ascii", max_length=200))
    _fixed("V1.6", 0x9B, "二维码下发应答", "桩 -> 平台", (D, GU, f("result", "获取结果", 1, default=1)))


def _apply() -> None:
    for version in ("V1.4", "V1.5", "V1.6", "V1.7"):
        _register_common(version, modern_start=False)
        _register_legacy_settings(version)
    for version in ("V1.6", "V1.7", "V1.8.1", "V2.1"):
        _register_parallel(version)
    _register_common("V1.8.1", modern_start=True)
    _register_common("V2.1", modern_start=True, v21=True)
    _register_v18_settings("V1.8.1")
    _register_v18_settings("V2.1", v21=True)
    _register_v21_only()
    _register_supplements()


_apply()
