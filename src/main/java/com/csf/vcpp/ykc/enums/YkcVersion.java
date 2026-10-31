package com.csf.vcpp.ykc.enums;

import com.csf.vcpp.ykc.consts.YkcProtocolConst;
import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;

/**
 * 云快充协议版本能力模型。
 * <p>
 * 一个版本 = 一组「帧结构参数」+ 一组「支持的帧类型」。
 * 指令执行器只需按 {@code @ProtocolCmd(protocols = {V15, V16})} 声明自己支持的版本，
 * 注册表在启动时会用 {@link #supports(FrameType)} 做版本对应关系校验。
 * </p>
 * <p>
 * 版本对应关系修正要点（相对历史实现）：
 * </p>
 * <ul>
 *     <li>交易记录：V1.5 / V1.6 为 {@code 0x3B}；V1.7 起为 {@code 0x3D}（历史实现只在 V15/V16 注册了 0x3B）。</li>
 *     <li>数据长度域：V1.5 ~ V1.8 为 1 字节；V2.1 起为 2 字节。</li>
 *     <li>发送时间域：仅 V2.1 起存在（CP56Time2a，7 字节）。</li>
 *     <li>计费模型：V1.5 ~ V1.8 固定 4 段（尖峰平谷），深谷扩展为 5 段（按消息体长度识别）；V2.1 段数由「费率数量」字段决定。</li>
 *     <li>二维码：V1.5 补充协议 0xF0/0xF1；V1.6 补充协议 0x9C/0x9E + 0x9B；V2.1 为 0x5A/0x5B。</li>
 * </ul>
 */
@Getter
public enum YkcVersion {

	V15(YkcProtocolConst.V15, "云快充1.5", 1, false, FrameType.TRADE_UP, 4, false),
	V16(YkcProtocolConst.V16, "云快充1.6", 1, false, FrameType.TRADE_UP, 4, false),
	V17(YkcProtocolConst.V17, "云快充1.7", 1, false, FrameType.TRADE_UP2, 4, false),
	V18(YkcProtocolConst.V18, "云快充1.8", 1, false, FrameType.TRADE_UP2, 4, false),
	V21(YkcProtocolConst.V21, "云快充2.1", 2, true, FrameType.TRADE_UP2, 0, true),
	;

	/**
	 * 所有版本通用的帧类型。
	 * <p>
	 * 用内部类持有：枚举常量的构造先于普通静态字段初始化，直接声明静态常量会拿到 null。
	 * </p>
	 */
	private static final class Frames {

		static final Set<FrameType> BASE = EnumSet.of(
			// ---------- 注册 / 心跳 ----------
			FrameType.LOGIN, FrameType.LOGIN_ACK,
			FrameType.HEARTBEAT, FrameType.HEARTBEAT_ACK,
			FrameType.BILLING_MODEL_VERIFY, FrameType.BILLING_MODEL_VERIFY_ACK,
			FrameType.BILLING_MODEL, FrameType.BILLING_MODEL_ACK,
			// ---------- 实时数据 ----------
			FrameType.REAL_TIME_DATA, FrameType.REAL_TIME_DATA_UP,
			FrameType.HANDSHAKE, FrameType.PARAM_CONFIG, FrameType.CHARGE_FINISH, FrameType.ERROR,
			FrameType.CHARGING_BMS_STOP, FrameType.CHARGING_MOTOR_STOP,
			FrameType.CHARGING_BMS_QA, FrameType.CHARGING_BMS_STATUS,
			// ---------- 运营交互 ----------
			FrameType.APPLY_CHARGE, FrameType.APPLY_CHARGE_ACK,
			FrameType.REMOTE_CHARGE_ACK, FrameType.REMOTE_CHARGE,
			FrameType.REMOTE_STOP_ACK, FrameType.REMOTE_STOP,
			FrameType.TRADE_ACK,
			FrameType.BALANCE_UPDATE_ACK, FrameType.BALANCE_UPDATE,
			FrameType.CARD_SYNC_ACK, FrameType.CARD_SYNC,
			FrameType.CARD_CLEAR_ACK, FrameType.CARD_CLEAR,
			FrameType.CARD_QUERY_UP, FrameType.CARD_QUERY_ACK,
			// ---------- 平台设置 ----------
			FrameType.PARAM_SETTING_ACK, FrameType.PARAM_SETTING,
			FrameType.TIME_SYNC_ACK, FrameType.TIME_SYNC,
			FrameType.BILLING_MODEL_SETTING_ACK, FrameType.BILLING_MODEL_SETTING,
			// ---------- 车位锁 ----------
			FrameType.LOCK_UP, FrameType.REMOTE_LOCK, FrameType.REMOTE_LOCK_ACK,
			// ---------- 远程维护 ----------
			FrameType.REMOTE_RESTART_ACK, FrameType.REMOTE_RESTART,
			FrameType.REMOTE_UPDATE_ACK, FrameType.REMOTE_UPDATE,
			// ---------- 并充模式 ----------
				FrameType.PARALLEL_CHARGE, FrameType.PARALLEL_CHARGE_ACK,
				FrameType.REMOTE_PARALLEL_CHARGE_ACK, FrameType.REMOTE_PARALLEL_CHARGE);
	}

	private final String label;

	private final String desc;

	/**
	 * 数据长度域字节数：V1.x = 1，V2.1 = 2
	 */
	private final int lengthFieldLength;

	/**
	 * 是否携带 7 字节 CP56Time2a 发送时间域
	 */
	private final boolean sendTimeField;

	/**
	 * 该版本使用的交易记录帧类型（0x3B / 0x3D）
	 */
	private final FrameType tradeRecordFrameType;

	/**
	 * 计费模型固定费率段数；0 表示由「费率数量」字段动态决定（V2.1）
	 */
	private final int billingSectionCount;

	/**
	 * 计费模型费率段数是否由报文动态决定
	 */
	private final boolean variableBillingSections;

	/**
	 * 该版本支持的全部帧类型
	 */
	private final Set<FrameType> supportedFrames;

	YkcVersion(String label, String desc, int lengthFieldLength, boolean sendTimeField,
	            FrameType tradeRecordFrameType, int billingSectionCount, boolean variableBillingSections) {
		this.label = label;
		this.desc = desc;
		this.lengthFieldLength = lengthFieldLength;
		this.sendTimeField = sendTimeField;
		this.tradeRecordFrameType = tradeRecordFrameType;
		this.billingSectionCount = billingSectionCount;
		this.variableBillingSections = variableBillingSections;
		this.supportedFrames = buildSupportedFrames(this, tradeRecordFrameType);
	}

	private static Set<FrameType> buildSupportedFrames(YkcVersion version, FrameType tradeRecordFrameType) {
		Set<FrameType> frames = EnumSet.copyOf(Frames.BASE);
		frames.add(tradeRecordFrameType);

		switch (version) {
			case V15 -> frames.addAll(EnumSet.of(FrameType.QR_PREFIX, FrameType.QR_PREFIX_ACK));
			case V16 -> frames.addAll(EnumSet.of(
					FrameType.QR_PREFIX, FrameType.QR_PREFIX_ACK,
					FrameType.QR_SEND, FrameType.QR_SEND2, FrameType.QR_SEND_ACK));
			case V17 -> {
				// V1.7 相对 V1.6 只调整了交易记录帧
			}
			case V18 -> frames.addAll(EnumSet.of(
					FrameType.MAX_POWER, FrameType.MAX_POWER_ACK,
					FrameType.APPLY_CHARGE2, FrameType.APPLY_CHARGE2_ACK,
					FrameType.REMOTE_CHARGE2, FrameType.REMOTE_CHARGE2_ACK));
			case V21 -> frames.addAll(EnumSet.of(
					FrameType.MAX_POWER, FrameType.MAX_POWER_ACK,
					FrameType.APPLY_CHARGE2, FrameType.APPLY_CHARGE2_ACK,
					FrameType.REMOTE_CHARGE2, FrameType.REMOTE_CHARGE2_ACK,
					FrameType.DEVICE_ERROR, FrameType.DEVICE_ERROR_ACK,
					FrameType.DEVICE_ERROR_RESET, FrameType.DEVICE_ERROR_RESET_ACK,
					FrameType.TRADE_LOG, FrameType.TRADE_LOG_ACK,
					FrameType.CHARGE_MOTOR_START, FrameType.CHARGE_MOTOR_START_ACK,
					FrameType.QR_SETTING, FrameType.QR_SETTING_ACK,
					FrameType.PLATFORM_CONNECT, FrameType.PLATFORM_CONNECT_ACK,
					FrameType.PARAM_SETTING2, FrameType.PARAM_SETTING2_ACK,
					FrameType.KEY_UPDATE, FrameType.KEY_UPDATE_ACK,
					FrameType.LOG, FrameType.LOG_ACK,
					FrameType.VIN_UPLOAD, FrameType.VIN_UPLOAD_ACK));
		}
		return frames;
	}

	public boolean supports(FrameType frameType) {
		return frameType != null && supportedFrames.contains(frameType);
	}

	/**
	 * 是否为交易记录上送帧（0x3B / 0x3D）
	 */
	public boolean isTradeRecord(FrameType frameType) {
		return tradeRecordFrameType == frameType;
	}

	/**
	 * 深谷（第 5 段）计费模型是否可用：V1.5 起为可选扩展，V2.1 由费率数量天然支持
	 */
	public boolean deepValleyCapable() {
		return variableBillingSections || this == V15 || this == V16 || this == V17 || this == V18;
	}

	private static final Set<String> LABELS = EnumSet.allOf(YkcVersion.class).stream()
			.map(YkcVersion::getLabel)
			.collect(java.util.stream.Collectors.toUnmodifiableSet());

	public static boolean isSupported(String label) {
		return label != null && LABELS.contains(label);
	}

	public static YkcVersion of(String label) {
		for (YkcVersion version : values()) {
			if (version.label.equals(label)) {
				return version;
			}
		}
		throw new IllegalArgumentException("不支持的云快充协议版本: " + label);
	}
}
