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
@ProtocolCmd(value = FrameType.PARAM_CONFIG, protocols = {V15, V16, V17})
public class YkcV15ParamConfigUpCmd extends YkcCmdExecutor { // 参数配置
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

		// BMS 单体动力蓄电池最高允许充电电压 0~24 V
		BigDecimal batteryMaxVol = new BigDecimal(body.readUnsignedShortLE()).multiply(new BigDecimal("0.01")).setScale(2, RoundingMode.DOWN);

		// BMS 最高允许充电电流
		BigDecimal batteryMaxCurrent = new BigDecimal(body.readUnsignedShortLE()).multiply(new BigDecimal("0.1")).subtract(new BigDecimal(400)).setScale(1, RoundingMode.DOWN);

		// BMS 动力蓄电池标称总能量  0~1000 kwh
		BigDecimal batteryTotalKwh = new BigDecimal(body.readUnsignedShortLE()).multiply(new BigDecimal("0.1")).setScale(1, RoundingMode.DOWN);

		// BMS 最高允许充电总电压
		BigDecimal maxVol = new BigDecimal(body.readUnsignedShortLE()).multiply(new BigDecimal("0.1")).setScale(1, RoundingMode.DOWN);

		// BMS 最高允许温度   -50 ~ 200
		int maxTemp = body.readByte() - 50;

		// BMS 整车动力蓄电池荷电状态 (soc) 0~100
		BigDecimal soc = new BigDecimal(body.readUnsignedShortLE()).multiply(new BigDecimal("0.1")).setScale(1, RoundingMode.DOWN);

		// BMS 整车动力蓄电池当前电池电压
		BigDecimal vol = new BigDecimal(body.readUnsignedShortLE()).multiply(new BigDecimal("0.1")).setScale(1, RoundingMode.DOWN);

		// 电桩最高输出电压
		BigDecimal maxOutVol = new BigDecimal(body.readUnsignedShortLE()).multiply(new BigDecimal("0.1")).setScale(1, RoundingMode.DOWN);

		// 电桩最低输出电压
		BigDecimal minOutVol = new BigDecimal(body.readUnsignedShortLE()).multiply(new BigDecimal("0.1")).setScale(1, RoundingMode.DOWN);

		// 电桩最大输出电流
		BigDecimal maxOutCurrent = new BigDecimal(body.readUnsignedShortLE()).multiply(new BigDecimal("0.1")).subtract(new BigDecimal(400)).setScale(1, RoundingMode.DOWN);

		// 电桩最小输出电流
		BigDecimal minOutCurrent = new BigDecimal(body.readUnsignedShortLE()).multiply(new BigDecimal("0.1")).subtract(new BigDecimal(400)).setScale(1, RoundingMode.DOWN);

		log.info("""
				【云快充】{} ⬆️ 参数配置 - 交易流水号: {}
				桩号: {}
				枪号: {}
				BMS 单体动力蓄电池最高允许充电电压: {}V
				BMS 最高允许充电电流: {}A
				BMS 动力蓄电池标称总能量: {}kwh
				BMS 最高允许充电总电压: {}V
				BMS 最高允许温度: {}℃
				SOC: {}%
				BMS 整车动力蓄电池当前电池电压: {}V
				电桩最高输出电压: {}V
				电桩最低输出电压: {}V
				电桩最大输出电流: {}A
				电桩最小输出电流: {}A
				---------------------------------
				""",
			versionLabel, tradeNo, deviceId, gunNo, batteryMaxVol, batteryMaxCurrent, batteryTotalKwh, maxVol, maxTemp, soc, vol, maxOutVol, minOutVol, maxOutCurrent, minOutCurrent);
	}
}
