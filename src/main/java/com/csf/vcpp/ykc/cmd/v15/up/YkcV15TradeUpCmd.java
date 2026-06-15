package com.csf.vcpp.ykc.cmd.v15.up;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.utils.BcdUtil;
import com.csf.vcpp.utils.BinUtil;
import com.csf.vcpp.utils.CP56Time2aUtil;
import com.csf.vcpp.ykc.cmd.v15.down.YkcV15TradeDownCmd;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcCmdExecutor;
import com.csf.vcpp.ykc.model.YkcMsgData;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V15;
import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V16;

@Service
@Slf4j
@RequiredArgsConstructor
@ProtocolCmd(value = FrameType.TRADE_UP, protocols = {V15, V16})
public class YkcV15TradeUpCmd extends YkcCmdExecutor { // 交易记录

	private final YkcV15TradeDownCmd ykcV15TradeDownCmd;

	@Override
	public void execute(Channel channel, String versionLabel, YkcMsgData data) {
		ByteBuf body = data.getMsgBody();

		byte[] tradeNoBytes = new byte[16];
		body.readBytes(tradeNoBytes);
		String tradeNo = BcdUtil.toString(tradeNoBytes);  // 交易流水号

		byte[] deviceIdBytes = new byte[7];
		body.readBytes(deviceIdBytes);
		Long deviceId = BcdUtil.bcdBytesToLong(deviceIdBytes);  // 桩号

		byte gunNo = body.readByte();// 枪号

		byte[] startTimeBytes = new byte[7];
		body.readBytes(startTimeBytes);
		// 开始时间
		LocalDateTime startTime = CP56Time2aUtil.decode(startTimeBytes);

		byte[] endTimeBytes = new byte[7];
		body.readBytes(endTimeBytes);
		// 结束时间
		LocalDateTime endTime = CP56Time2aUtil.decode(endTimeBytes);

		// 尖单价 = 尖电费率+服务费率
		BigDecimal jian = new BigDecimal(body.readUnsignedIntLE()).multiply(new BigDecimal("0.00001")).setScale(5, RoundingMode.DOWN);
		// 尖电量
		BigDecimal jianKwh = new BigDecimal(body.readUnsignedIntLE()).multiply(new BigDecimal("0.0001")).setScale(4, RoundingMode.DOWN);
		// 计损尖电量
		BigDecimal jianSunKwh = new BigDecimal(body.readUnsignedIntLE()).multiply(new BigDecimal("0.0001")).setScale(4, RoundingMode.DOWN);
		// 尖金额
		BigDecimal jianAmount = new BigDecimal(body.readUnsignedIntLE()).multiply(new BigDecimal("0.0001")).setScale(4, RoundingMode.DOWN);

		// 峰单价 = 峰电费率+服务费率
		BigDecimal feng = new BigDecimal(body.readUnsignedIntLE()).multiply(new BigDecimal("0.00001")).setScale(5, RoundingMode.DOWN);
		// 峰电量
		BigDecimal fengKwh = new BigDecimal(body.readUnsignedIntLE()).multiply(new BigDecimal("0.0001")).setScale(4, RoundingMode.DOWN);
		// 计损峰电量
		BigDecimal fengSunKwh = new BigDecimal(body.readUnsignedIntLE()).multiply(new BigDecimal("0.0001")).setScale(4, RoundingMode.DOWN);
		// 峰金额
		BigDecimal fengAmount = new BigDecimal(body.readUnsignedIntLE()).multiply(new BigDecimal("0.0001")).setScale(4, RoundingMode.DOWN);

		// 平单价 = 平电费率+服务费率
		BigDecimal ping = new BigDecimal(body.readUnsignedIntLE()).multiply(new BigDecimal("0.00001")).setScale(5, RoundingMode.DOWN);
		// 平电量
		BigDecimal pingKwh = new BigDecimal(body.readUnsignedIntLE()).multiply(new BigDecimal("0.0001")).setScale(4, RoundingMode.DOWN);
		// 计损平电量
		BigDecimal pingSunKwh = new BigDecimal(body.readUnsignedIntLE()).multiply(new BigDecimal("0.0001")).setScale(4, RoundingMode.DOWN);
		// 平金额
		BigDecimal pingAmount = new BigDecimal(body.readUnsignedIntLE()).multiply(new BigDecimal("0.0001")).setScale(4, RoundingMode.DOWN);

		// 谷单价 = 谷电费率+服务费率
		BigDecimal gu = new BigDecimal(body.readUnsignedIntLE()).multiply(new BigDecimal("0.00001")).setScale(5, RoundingMode.DOWN);
		// 谷电量
		BigDecimal guKwh = new BigDecimal(body.readUnsignedIntLE()).multiply(new BigDecimal("0.0001")).setScale(4, RoundingMode.DOWN);
		// 计损谷电量
		BigDecimal guSunKwh = new BigDecimal(body.readUnsignedIntLE()).multiply(new BigDecimal("0.0001")).setScale(4, RoundingMode.DOWN);
		// 谷金额
		BigDecimal guAmount = new BigDecimal(body.readUnsignedIntLE()).multiply(new BigDecimal("0.0001")).setScale(4, RoundingMode.DOWN);

		// 电表总起值
		byte[] meterStartBytes = new byte[5];
		body.readBytes(meterStartBytes);
		BigDecimal meterStart = new BigDecimal(BinUtil.bin2Long(meterStartBytes)).multiply(new BigDecimal("0.0001")).setScale(4, RoundingMode.DOWN);

		// 电表总止值
		byte[] meterEndBytes = new byte[5];
		body.readBytes(meterEndBytes);
		BigDecimal meterEnd = new BigDecimal(BinUtil.bin2Long(meterEndBytes)).multiply(new BigDecimal("0.0001")).setScale(4, RoundingMode.DOWN);

		// 总电量
		BigDecimal totalKwh = new BigDecimal(body.readUnsignedIntLE()).multiply(new BigDecimal("0.0001")).setScale(4, RoundingMode.DOWN);

		// 计损总电量
		BigDecimal totalSunKwh = new BigDecimal(body.readUnsignedIntLE()).multiply(new BigDecimal("0.0001")).setScale(4, RoundingMode.DOWN);

		// 消费金额
		BigDecimal totalAmount = new BigDecimal(body.readUnsignedIntLE()).multiply(new BigDecimal("0.0001")).setScale(4, RoundingMode.DOWN);

		// vin  VIN码，此处VIN码和充电时VIN码不同，正序直接上传，无需补0和反序
		byte[] vinBytes = new byte[17];
		body.readBytes(vinBytes);
		String vin = new String(vinBytes, StandardCharsets.US_ASCII).replaceAll("\\u0000+$", "");

		// 交易标识
		/**
		 * 0x01：app启动
		 * 0x02：卡启动
		 * 0x04：离线卡启动
		 * 0x05: vin码启动充电
		 */
		byte tradeType = body.readByte();

		// 交易时间
		byte[] tradeTimeBytes = new byte[7];
		body.readBytes(tradeTimeBytes);
		LocalDateTime tradeTime = CP56Time2aUtil.decode(tradeTimeBytes);

		// 停止原因
		String stopReasonStr = getStopReason(body.readByte());

		// 物理卡号
		long cardNo = body.readLongLE();

		log.info("【云快充】{} ⬆️ 交易记录 - 交易流水号: {} 桩号: {} 枪号: {} 开始时间: {} 结束时间: {} 尖单价: {} 尖电量: {}° 计损尖电量: {}° 尖金额: {} 峰单价: {} 峰电量: {}° 计损峰电量: {}° 峰金额: {} 平单价: {} 平电量: {}° 计损平电量: {}° 平金额: {} 谷单价: {} 谷电量: {}° 计损谷电量: {}° 谷金额: {} 电表总起值: {}° 电表总止值: {}° 总电量: {}° 计损总电量: {}° 消费金额: {}元 vin: {} 交易标识: {}[{}] 交易时间: {} 停止原因: {} 物理卡号: {}",
			versionLabel, tradeNo, deviceId, gunNo, startTime, endTime, jian, jianKwh, jianSunKwh, jianAmount, feng, fengKwh, fengSunKwh, fengAmount, ping, pingKwh, pingSunKwh, pingAmount, gu, guKwh, guSunKwh, guAmount, meterStart, meterEnd, totalKwh, totalSunKwh, totalAmount, vin, tradeType, getTradeTypeDesc(tradeType), tradeTime, stopReasonStr, cardNo);

		// todo upload

		// ack
		data.setTradeNo(tradeNo);
		ykcV15TradeDownCmd.execute(channel, versionLabel, data);
	}

	private String getTradeTypeDesc(byte tradeType) {
		return switch (tradeType) {
			case 0x01 -> "app启动";
			case 0x02 -> "卡启动";
			case 0x04 -> "离线卡启动";
			case 0x05 -> "vin码启动充电";
			default -> "-";
		};
	}

	private String getStopReason(short stopReason) {
		return switch (stopReason) {
			case 0x40 -> "结束充电，APP 远程停止";
			case 0x41 -> "结束充电，SOC 达到 100%";
			case 0x42 -> "结束充电，充电电量满足设定条件";
			case 0x43 -> "结束充电，充电金额满足设定条件";
			case 0x44 -> "结束充电，充电时间满足设定条件";
			case 0x45 -> "结束充电，手动停止充电";
			case 0x46, 0x47, 0x48, 0x49 -> "结束充电，其他方式（预留）";
			case 0x4A -> "充电启动失败，充电桩控制系统故障(需要重启或自动恢复)";
			case 0x4B -> "充电启动失败，控制导引断开";
			case 0x4C -> "充电启动失败，断路器跳位";
			case 0x4D -> "充电启动失败，电表通信中断";
			case 0x4E -> "充电启动失败，余额不足";
			case 0x4F -> "充电启动失败，充电模块故障";
			case 0x50 -> "充电启动失败，急停开入";
			case 0x51 -> "充电启动失败，防雷器异常";
			case 0x52 -> "充电启动失败，BMS 未就绪";
			case 0x53 -> "充电启动失败，温度异常";
			case 0x54 -> "充电启动失败，电池反接故障";
			case 0x55 -> "充电启动失败，电子锁异常";
			case 0x56 -> "充电启动失败，合闸失败";
			case 0x57 -> "充电启动失败，绝缘异常";
			case 0x58 -> "充电启动失败，预留";
			case 0x59 -> "充电启动失败，接收 BMS 握手报文 BHM 超时";
			case 0x5A -> "充电启动失败，接收 BMS 和车辆的辨识报文超时 BRM";
			case 0x5B -> "充电启动失败，接收电池充电参数报文超时 BCP";
			case 0x5C -> "充电启动失败，接收 BMS 完成充电准备报文超时 BRO AA";
			case 0x5D -> "充电启动失败，接收电池充电总状态报文超时 BCS";
			case 0x5E -> "充电启动失败，接收电池充电要求报文超时 BCL";
			case 0x5F -> "充电启动失败，接收电池状态信息报文超时 BSM";
			case 0x60 -> "充电启动失败，GB2015 电池在 BHM 阶段有电压不允许充电";
			case 0x61 -> "充电启动失败，GB2015 辨识阶段在 BRO_AA 时候电池实际电压与 BCP 报文电池电压差距大于 5%";
			case 0x62 -> "充电启动失败，B2015 充电机在预充电阶段从 BRO_AA 变成BRO_00 状态";
			case 0x63 -> "充电启动失败，接收主机配置报文超时";
			case 0x64 -> "充电启动失败，充电机未准备就绪,我们没有回 CRO AA，对应老国标";
			case 0x65, 0x66, 0x67, 0x68, 0x69 -> "充电启动失败，其他原因（预留）";
			case 0x6A -> "充电异常中止，系统闭锁";
			case 0x6B -> "充电异常中止，导引断开";
			case 0x6C -> "充电异常中止，断路器跳位";
			case 0x6D -> "充电异常中止，电表通信中断";
			case 0x6E -> "充电异常中止，余额不足";
			case 0x6F -> "充电异常中止，交流保护动作";
			case 0x70 -> "充电异常中止，直流保护动作";
			case 0x71 -> "充电异常中止，充电模块故障";
			case 0x72 -> "充电异常中止，急停开入";
			case 0x73 -> "充电异常中止，防雷器异常";
			case 0x74 -> "充电异常中止，温度异常";
			case 0x75 -> "充电异常中止，输出异常";
			case 0x76 -> "充电异常中止，充电无流";
			case 0x77 -> "充电异常中止，电子锁异常";
			case 0x78 -> "充电异常中止，预留";
			case 0x79 -> "充电异常中止，总充电电压异常";
			case 0x7A -> "充电异常中止，总充电电流异常";
			case 0x7B -> "充电异常中止，单体充电电压异常";
			case 0x7C -> "充电异常中止，电池组过温";
			case 0x7D -> "充电异常中止，最高单体充电电压异常";
			case 0x7E -> "充电异常中止，最高电池组过温";
			case 0x7F -> "充电异常中止，BMV 单体充电电压异常";
			case 0x80 -> "充电异常中止，BMT 电池组过温";
			case 0x81 -> "充电异常中止，电池状态异常停止充电";
			case 0x82 -> "充电异常中止，车辆发报文禁止充电";
			case 0x83 -> "充电异常中止，充电桩断电";
			case 0x84 -> "充电异常中止，接收电池充电总状态报文超时";
			case 0x85 -> "充电异常中止，接收电池充电要求报文超时";
			case 0x86 -> "充电异常中止，接收电池状态信息报文超时";
			case 0x87 -> "充电异常中止，接收 BMS 中止充电报文超时";
			case 0x88 -> "充电异常中止，接收 BMS 充电统计报文超时";
			case 0x89 -> "充电异常中止，接收对侧 CCS 报文超时";
			case 0x8A, 0x8B, 0x8C, 0x8D, 0x8E, 0x8F -> "充电异常中止，其他原因（预留）";
			case 0x90 -> "未知原因停止";
			default -> "-";
		};
	}
}
