package com.csf.vcpp.ykc.cmd.v15.up;

import cn.hutool.core.util.HexUtil;
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
@ProtocolCmd(value = FrameType.CHARGE_FINISH, protocols = {V15, V16, V17})
public class YkcV15ChargeFinishUpCmd extends YkcCmdExecutor { // 充电结束
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

		// BMS 中止荷电状态SOC
		byte soc = body.readByte();

		// BMS 动力蓄电池单体最低电压
		BigDecimal minVol = new BigDecimal(body.readShortLE()).multiply(new BigDecimal("0.01")).setScale(2, RoundingMode.DOWN);

		// BMS 动力蓄电池单体最高电压
		BigDecimal maxVol = new BigDecimal(body.readShortLE()).multiply(new BigDecimal("0.01")).setScale(2, RoundingMode.DOWN);

		// BMS 动力蓄电池最低温度
		int minTemp = body.readByte() - 50;

		// BMS 动力蓄电池最高温度
		int maxTemp = body.readByte() - 50;

		// 电桩累计充电时间
		int totalChargeTime = body.readShortLE();

		// 电桩输出能量
		BigDecimal kwh = new BigDecimal(body.readShortLE()).multiply(new BigDecimal("0.1")).setScale(1, RoundingMode.DOWN);

		// 电桩充电机编号
		byte[] motorNoBytes = new byte[4];
		body.readBytes(motorNoBytes);
		String motorNo = HexUtil.encodeHexStr(motorNoBytes);

		log.info("""
				【云快充】{} ⬆️ 充电结束 - 交易流水号: {}
				桩号: {}
				枪号: {}
				SOC: {}%
				BMS 动力蓄电池单体最低电压: {}V
				BMS 动力蓄电池单体最高电压: {}V
				BMS 动力蓄电池最低温度: {}℃
				BMS 动力蓄电池最高温度: {}℃
				累计充电时间: {}min
				输出能量: {}kwh
				充电机编号: {}
				---------------------------------
				""",
			versionLabel, tradeNo, deviceId, gunNo, soc, minVol, maxVol, minTemp, maxTemp, totalChargeTime, kwh, motorNo);
	}
}
