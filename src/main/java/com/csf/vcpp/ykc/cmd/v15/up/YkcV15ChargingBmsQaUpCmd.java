package com.csf.vcpp.ykc.cmd.v15.up;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.utils.BcdUtil;
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

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.*;

@Service
@Slf4j
@RequiredArgsConstructor
@ProtocolCmd(value = FrameType.CHARGING_BMS_QA, protocols = {V15, V16, V17})
public class YkcV15ChargingBmsQaUpCmd extends YkcCmdExecutor { // 充电过程BMS需求&充电机输出
	@Override
	public void execute(Channel channel, String versionLabel, YkcMsgData data) {
		ByteBuf body = data.getMsgBody();

		byte[] tradeNoBytes = new byte[16];
		body.readBytes(tradeNoBytes);
		String tradeNo = BcdUtil.toString(tradeNoBytes);  // 交易流水号

		byte[] deviceIdBytes = new byte[7];
		body.readBytes(deviceIdBytes);
		Long deviceId = BcdUtil.bcdBytesToLong(deviceIdBytes);  // 桩号

		byte gunNo = body.readByte(); // 枪号

		// BMS 电压需求
		BigDecimal voltageDemand = new BigDecimal(body.readUnsignedShortLE()).multiply(new BigDecimal("0.1")).setScale(1, RoundingMode.DOWN);

		// BMS 电流需求
		BigDecimal currentDemand = new BigDecimal(body.readUnsignedShortLE()).multiply(new BigDecimal("0.1")).subtract(new BigDecimal(400)).setScale(1, RoundingMode.DOWN);

		// BMS 充电模式
		byte chargeMode = body.readByte(); // 0x01：恒压充电；0x02：恒流充电

		// BMS 充电电压测量值
		BigDecimal voltageMeasurement = new BigDecimal(body.readUnsignedShortLE()).multiply(new BigDecimal("0.1")).setScale(1, RoundingMode.DOWN);

		// BMS 充电电流测量值
		BigDecimal currentMeasurement = new BigDecimal(body.readUnsignedShortLE()).multiply(new BigDecimal("0.1")).subtract(new BigDecimal(400)).setScale(1, RoundingMode.DOWN);

		// BMS 最高单体动力蓄电池电压
		int i = body.readUnsignedShortLE();
		int batteryMaxVol = i & 0x0FFF;

		// BMS 最高单体动力蓄电池电压所在组号
		int batteryMaxVolGroup = (i >> 12) & 0x0F;

		// BMS 当前荷电状态SOC（ %）
		byte soc = body.readByte();

		// BMS 估算剩余充电时间
		int remainingChargeTime = body.readUnsignedShortLE();

		// 电桩电压输出值
		BigDecimal voltageOutput = new BigDecimal(body.readUnsignedShortLE()).multiply(new BigDecimal("0.1")).setScale(1, RoundingMode.DOWN);

		// 电桩电流输出值
		BigDecimal currentOutput = new BigDecimal(body.readUnsignedShortLE()).multiply(new BigDecimal("0.1")).subtract(new BigDecimal(400)).setScale(1, RoundingMode.DOWN);

		// 累计充电时间	0~600 min
		int totalChargeTime = body.readUnsignedShortLE();

		log.info("【云快充】{} ⬆️ 充电过程BMS需求与充电机输出 - 交易流水号: {} 桩号: {} 枪号: {} BMS 电压需求: {}V BMS 电流需求: {}A BMS 充电模式: {}[{}] BMS 充电电压测量值: {}V BMS 充电电流测量值: {}A BMS 最高单体动力蓄电池电压: {}V BMS 最高单体动力蓄电池电压所在组号: {} BMS 当前荷电状态SOC: {}% BMS 估算剩余充电时间: {}min 电桩电压输出值: {}V 电桩电流输出值: {}A 累计充电时间: {}min",
			versionLabel, tradeNo, deviceId, gunNo, voltageDemand, currentDemand, chargeMode, getDesc(chargeMode), voltageMeasurement, currentMeasurement, batteryMaxVol, batteryMaxVolGroup, soc, remainingChargeTime, voltageOutput, currentOutput, totalChargeTime);
	}

	private String getDesc(byte mode) {
		return mode == 0x01 ? "恒压充电" : "恒流充电";
	}
}
