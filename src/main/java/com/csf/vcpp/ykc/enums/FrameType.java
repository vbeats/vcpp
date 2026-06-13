package com.csf.vcpp.ykc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
@ToString
public enum FrameType {
	// -------------- 注册心跳 ------------------
	LOGIN((short) 0x01, "充电桩登录认证"), // ⬆️
	LOGIN_ACK((short) 0x02, "登录认证应答"), // ⬇️

	HEARTBEAT((short) 0x03, "充电桩心跳包"), // ⬆️
	HEARTBEAT_ACK((short) 0x04, "心跳包应答"), // ⬇️

	BILLING_VERIFY_MODEL((short) 0x05, "计费模型验证请求"), // ⬆️
	BILLING_VERIFY_MODEL_ACK((short) 0x06, "计费模型验证请求应答"), // ⬇️

	BILLING_MODEL((short) 0x09, "计费模型请求"), // ⬆️
	BILLING_MODEL_ACK((short) 0x0A, "计费模型请求应答"), // ⬇️

	// -------------- 实时数据帧 ------------------

	REAL_TIME_DATA((short) 0x12, "读取实时监测数据"), // ⬇️
	REAL_TIME_DATA_UP((short) 0x13, "上传实时监测数据"), // ⬆️

	HANDSHAKE((short) 0x15, "充电握手"), // ⬆️
	PARAM_CONFIG((short) 0x17, "参数配置"), // ⬆️
	CHARGE_FINISH((short) 0x19, "充电结束"), // ⬆️
	ERROR((short) 0x1B, "错误报文"), // ⬆️
	CHARGING_BMS_STOP((short) 0x1D, "充电阶段BMS中止"), // ⬆️
	CHARGING_MOTOR_STOP((short) 0x21, "充电阶段充电机中止"), // ⬆️
	CHARGING_BMS_QA((short) 0x23, "充电过程BMS需求&充电机输出"), // ⬆️
	CHARGING_BMS_STATUS((short) 0x25, "充电过程BMS信息"), // ⬆️

	// -------------- 运营交互 ------------------

	APPLY_CHARGE((short) 0x31, "充电桩主动申请启动充电"), // ⬆️
	APPLY_CHARGE_ACK((short) 0x32, "运营平台确认启动充电"), // ⬇️

	REMOTE_CHARGE_ACK((short) 0x33, "远程启机命令回复"), // ⬆️
	REMOTE_CHARGE((short) 0x34, "运营平台远程控制启机"), // ⬇️

	REMOTE_STOP_ACK((short) 0x35, "远程停机命令回复"), // ⬆️
	REMOTE_STOP((short) 0x36, "运营平台远程停机"), // ⬇️

	TRADE_UP((short) 0x3B, "交易记录"), // ⬆️
	TRADE_UP2((short) 0x3D, "交易记录"), // ⬆️  v1.7+
	TRADE_ACK((short) 0x40, "交易记录确认"), // ⬇️

	BALANCE_UPDATE_ACK((short) 0x41, "余额更新应答"), // ⬆️
	BALANCE_UPDATE((short) 0x42, "远程账户余额更新"), // ⬇️

	CARD_SYNC_ACK((short) 0x43, "卡数据同步应答"), // ⬆️
	CARD_SYNC((short) 0x44, "离线卡数据同步"), // ⬇️

	CARD_CLEAR_ACK((short) 0x45, "离线卡数据清除应答"), // ⬆️
	CARD_CLEAR((short) 0x46, "离线卡数据清除"), // ⬇️

	CARD_QUERY_UP((short) 0x47, "离线卡数据查询应答"), // ⬆️
	CARD_QUERY_ACK((short) 0x48, "离线卡数据查询"), // ⬇️

	//v2.0...
	DEVICE_ERROR_ACK((short) 0x49, "设备故障上送回复确认"), // ⬇️
	DEVICE_ERROR((short) 0x50, "设备故障上送"), // ⬆️

	DEVICE_ERROR_RESET_ACK((short) 0x4A, "设备故障复位上送回复确认"), // ⬇️
	DEVICE_ERROR_RESET((short) 0x4B, "设备故障复位上送"), // ⬆️

	TRADE_LOG_ACK((short) 0x4C, "交易记录召唤确认"), // ⬆️
	TRADE_LOG((short) 0x4D, "交易记录召唤"), // ⬇️

	CHARGE_MOTOR_START_ACK((short) 0x4E, "充电机启动完成应答"), // ⬇️
	CHARGE_MOTOR_START((short) 0x4F, "充电机启动完成"), // ⬆️

	// -------------- 平台设置 ------------------

	PARAM_SETTING_ACK((short) 0x51, "充电桩工作参数设置应答"), // ⬆️ 功率修改应答
	PARAM_SETTING((short) 0x52, "充电桩工作参数设置"), // ⬇️ 功率修改

	TIME_SYNC_ACK((short) 0x55, "对时设置应答"), // ⬆️
	TIME_SYNC((short) 0x56, "对时设置"), // ⬇️

	BILLING_MODEL_SETTING_ACK((short) 0x57, "计费模型设置应答"), // ⬆️
	BILLING_MODEL_SETTING((short) 0x58, "计费模型设置"), // ⬇️

	MAX_POWER_ACK((short) 0x59, "默认最大功率下发应答"), // ⬇️  v1.8+
	MAX_POWER((short) 0x60, "默认最大功率下发"), // ⬇️  v1.8+

	// 2.0.....
	QR_SETTING_ACK((short) 0x5A, "二维码设置应答"), // ⬆️
	QR_SETTING((short) 0x5B, "二维码设置"), // ⬇️

	PLATFORM_CONNECT_ACK((short) 0x5C, "平台连接设置应答"), // ⬆️
	PLATFORM_CONNECT((short) 0x5D, "平台连接设置"), // ⬇️

	PARAM_SETTING2_ACK((short) 0x5E, "参数设置应答"), // ⬆️
	PARAM_SETTING2((short) 0x5F, "参数设置"), // ⬇️

	// -------------- 车位锁 ------------------

	LOCK_UP((short) 0x61, "地锁数据上送"), // ⬆️

	REMOTE_LOCK((short) 0x62, "遥控地锁升锁与降锁命令"), // ⬇️
	REMOTE_LOCK_ACK((short) 0x63, "充电桩返回数据"), // ⬆️ // 遥控地锁应答

	// -------------- 远程维护 ------------------
	REMOTE_RESTART_ACK((short) 0x91, "远程重启应答"), // ⬆️
	REMOTE_RESTART((short) 0x92, "远程重启"), // ⬇️

	REMOTE_UPDATE_ACK((short) 0x93, "远程更新应答"), // ⬆️
	REMOTE_UPDATE((short) 0x94, "远程更新"), // ⬇️

	KEY_UPDATE_ACK((short) 0x95, "密钥更新应答"), // ⬆️
	KEY_UPDATE((short) 0x96, "密钥更新"), // ⬇️

	LOG_ACK((short) 0x97, "日志召唤应答"), // ⬆️
	LOG((short) 0x98, "日志召唤"), // ⬇️

	// -------------- 并充模式 ------------------
	PARALLEL_CHARGE((short) 0xA1, "充电桩主动申请并充充电"), // ⬆️
	PARALLEL_CHARGE_ACK((short) 0xA2, "运营平台确认并充启动充电"), // ⬇️

	REMOTE_PARALLEL_CHARGE_ACK((short) 0xA3, "远程并充启机命令回复"), // ⬆️
	REMOTE_PARALLEL_CHARGE((short) 0xA4, "运营平台远程控制并充启机"), // ⬇️

	// --------------v.18+------------------
	APPLY_CHARGE2((short) 0xA5, "充电桩主动申请启动充电"), // ⬆️
	APPLY_CHARGE2_ACK((short) 0xA6, "运营平台确认启动充电"), // ⬇️

	REMOTE_CHARGE2_ACK((short) 0xA7, "远程启机命令回复"), // ⬆️
	REMOTE_CHARGE2((short) 0xA8, "运营平台远程控制启机"), // ⬇️


	// --------------v.20+------------------
	VIN_UPLOAD((short) 0xA9, "充电桩上报vin 码"), // ⬆️
	VIN_UPLOAD_ACK((short) 0xAA, "充电桩上报vin 码回复确认"), // ⬇️
	;

	private static final Map<Short, FrameType> lookup = new HashMap<>(64);

	static {
		Arrays.stream(FrameType.values()).forEach(e -> lookup.put(e.type, e));
	}

	private final Short type;

	private final String desc;


	public static FrameType getByValue(Short type) {
		return lookup.get(type);
	}
}
