package com.csf.vcpp.ykc.schema;

import com.csf.vcpp.upstream.UpstreamPublisherGateway;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcCmdExecutor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V15;
import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V16;
import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V17;
import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V18;
import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V21;
import static com.csf.vcpp.ykc.schema.FrameField.bcdHex;
import static com.csf.vcpp.ykc.schema.FrameField.bcdText;
import static com.csf.vcpp.ykc.schema.FrameField.bits;
import static com.csf.vcpp.ykc.schema.FrameField.bytes;
import static com.csf.vcpp.ykc.schema.FrameField.bytesRest;
import static com.csf.vcpp.ykc.schema.FrameField.skip;
import static com.csf.vcpp.ykc.schema.FrameField.uint;
import static com.csf.vcpp.ykc.schema.YkcSchemaUpCmd.Topic.OPS;
import static com.csf.vcpp.ykc.schema.YkcSchemaUpCmd.Topic.TRADE;

/**
 * 上行帧结构注册表：为「解析 + 记录 + 上报」型的帧类型提供统一实现。
 * <p>
 * 需要业务决策的帧（登录 / 计费模型 / 交易记录 / 实时数据 / 启动充电 / 需应答帧…）
 * 由独立执行器实现；注册顺序上独立执行器优先，{@code putIfAbsent} 保证不会被覆盖。
 * </p>
 */
@Component
public class YkcFrameSchemaRegistry {

	/**
	 * 全部版本
	 */
	private static final String[] ALL = {V15, V16, V17, V18, V21};

	private static final String[] ALL_16 = {V16};

	private static final String[] V21_ONLY = {V21};

	private static final String[] V18_21 = {V18, V21};

	private final UpstreamPublisherGateway publisher;

	private final List<Definition> definitions = new ArrayList<>();

	public YkcFrameSchemaRegistry(UpstreamPublisherGateway publisher) {
		this.publisher = publisher;
		registerTrade();
		registerOpr();
		registerSetting();
		registerMaintain();
		registerParallel();
		registerQr();
	}

	// ------------------------------------------------------------------ 交易/运营类

	private void registerTrade() {
		// 0x4C 交易记录召唤确认（V2.1）
		def(FrameType.TRADE_LOG_ACK, V21_ONLY, TRADE, FrameSchema.of(
				bcdText("交易流水号", 16),
				bcdHex("桩编号", 7),
				bcdText("枪号", 1),
				uint("召唤结果", 1),
				uint("失败原因", 1)));

		// 0x41 余额更新应答
		def(FrameType.BALANCE_UPDATE_ACK, ALL, OPS, FrameSchema.of(
				bcdHex("桩编号", 7),
				bytes("物理卡号", 8),
				uint("修改结果", 1)));

		// 0x43 离线卡数据同步应答
		def(FrameType.CARD_SYNC_ACK, ALL, OPS, FrameSchema.of(
				bcdHex("桩编号", 7),
				uint("保存结果", 1),
				uint("失败原因", 1)));

		// 0x45 离线卡数据清除应答
		def(FrameType.CARD_CLEAR_ACK, ALL, OPS, FrameSchema.of(
				bcdHex("桩编号", 7),
				uint("清除结果", 1),
				uint("失败原因", 1)));

		// 0x47 卡数据查询响应（桩 -> 平台）
		def(FrameType.CARD_QUERY_UP, ALL, OPS, FrameSchema.of(
				bcdHex("桩编号", 7),
				uint("卡数量", 1),
				bytesRest("卡数据")));
	}

	private void registerOpr() {
		// 0x61 地锁数据上送
		def(FrameType.LOCK_UP, ALL, OPS, FrameSchema.of(
				bcdHex("桩编号", 7),
				uint("枪号", 1),
				uint("车位锁状态", 1),
				uint("车位状态", 1),
				uint("地锁电量状态", 1),
				uint("报警状态", 1),
				skip(4)));

		// 0x63 遥控地锁升锁与降锁应答
		def(FrameType.REMOTE_LOCK_ACK, ALL, OPS, FrameSchema.of(
				bcdHex("桩编号", 7),
				uint("枪号", 1),
				uint("地锁控制返回标志", 1),
				skip(4)));
	}

	// ------------------------------------------------------------------ 平台设置类

	private void registerSetting() {
		// 0x51 充电桩工作参数设置应答
		def(FrameType.PARAM_SETTING_ACK, ALL, OPS, FrameSchema.of(
				bcdHex("桩编号", 7),
				uint("枪号", 1),
				uint("设置结果", 1)));

		// 0x59 默认最大功率应答（V1.8+）
		def(FrameType.MAX_POWER_ACK, V18_21, OPS, FrameSchema.of(
				bcdHex("桩编号", 7),
				uint("枪号", 1),
				uint("设置结果", 1)));

		// 0x5A 二维码设置应答（V2.1）
		def(FrameType.QR_SETTING_ACK, V21_ONLY, OPS, FrameSchema.of(
				bcdHex("桩编号", 7),
				uint("枪号", 1),
				uint("设置结果", 1)));

		// 0x5C 平台连接模式设置应答（V2.1）
		def(FrameType.PLATFORM_CONNECT_ACK, V21_ONLY, OPS, FrameSchema.of(
				bcdHex("桩编号", 7),
				uint("设置结果", 1)));

		// 0x5E 参数设置(二)应答（V2.1）
		def(FrameType.PARAM_SETTING2_ACK, V21_ONLY, OPS, FrameSchema.of(
				bcdHex("桩编号", 7),
				uint("枪号", 1),
				uint("设置结果", 1)));
	}

	// ------------------------------------------------------------------ 远程维护类

	private void registerMaintain() {
		// 0x93 远程升级应答
		def(FrameType.REMOTE_UPDATE_ACK, ALL, OPS, FrameSchema.of(
				bcdHex("桩编号", 7),
				uint("设置结果", 1)));

		// 0x95 密钥更新应答（V2.1）
		def(FrameType.KEY_UPDATE_ACK, V21_ONLY, OPS, FrameSchema.of(
				bcdHex("桩编号", 7),
				uint("设置结果", 1)));

		// 0x97 日志召唤应答（V2.1）
		def(FrameType.LOG_ACK, V21_ONLY, OPS, FrameSchema.of(
				bcdHex("桩编号", 7),
				uint("召唤结果", 1),
				uint("失败原因", 1),
				uint("日志文件数量", 2),
				bits("日志文件编码", 2)));
	}

	// ------------------------------------------------------------------ 并充模式

	private void registerParallel() {
		// 0xA3 并充模式远程控制启机回复确认
		def(FrameType.REMOTE_PARALLEL_CHARGE_ACK, ALL, TRADE, FrameSchema.of(
				bcdText("交易流水号", 16),
				bcdHex("桩编号", 7),
				bcdText("枪号", 1),
				uint("启动结果", 1),
				uint("失败原因", 1),
				uint("主辅枪标记", 1),
				bcdText("并充序号", 6)));

		// 0xA7 申请启动充电(二)回复确认（V1.8+）
		def(FrameType.REMOTE_CHARGE2_ACK, V18_21, TRADE, FrameSchema.of(
				bcdText("交易流水号", 16),
				bcdHex("桩编号", 7),
				bcdText("枪号", 1),
				uint("启动结果", 1),
				uint("失败原因", 1)));
	}

	// ------------------------------------------------------------------ 二维码

	private void registerQr() {
		// 0xF1 桩应答远程下发二维码前缀（V1.5 补充协议）
		def(FrameType.QR_PREFIX_ACK, new String[]{V15, V16}, OPS, FrameSchema.of(
				bcdHex("桩编号", 7),
				uint("下发结果", 1)));

		// 0x9B 充电桩应答平台下发二维码（V1.6 补充协议）
		def(FrameType.QR_SEND_ACK, ALL_16, OPS, FrameSchema.of(
				bcdHex("桩编号", 7),
				uint("枪号", 1),
				uint("获取结果", 1)));
	}

	// ------------------------------------------------------------------ 内部

	private void def(FrameType frameType, String[] protocols, YkcSchemaUpCmd.Topic topic, FrameSchema schema) {
		definitions.add(new Definition(frameType, protocols, topic, schema));
	}

	/**
	 * 注册到执行器表；已存在执行器的帧类型不会被覆盖
	 */
	public void registerInto(Map<String, YkcCmdExecutor> executers, Map<String, String> owners) {
		for (Definition definition : definitions) {
			YkcSchemaUpCmd cmd = new YkcSchemaUpCmd(definition.frameType(), definition.schema(), publisher, definition.topic());
			for (String protocol : definition.protocols()) {
				String key = protocol + "_" + definition.frameType().getType();
				if (executers.putIfAbsent(key, cmd) == null) {
					owners.put(key, "YkcSchemaUpCmd(" + definition.frameType().name() + ")");
				}
			}
		}
	}

	public List<Definition> getDefinitions() {
		return List.copyOf(definitions);
	}

	public record Definition(FrameType frameType, String[] protocols, YkcSchemaUpCmd.Topic topic, FrameSchema schema) {
	}
}
