package com.csf.vcpp.ykc.cmd.v15.up;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.utils.BcdUtil;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcCmdExecutor;
import com.csf.vcpp.ykc.model.YkcMsgData;
import com.csf.vcpp.ykc.utils.YkcExceptionUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.*;

@Service
@Slf4j
@RequiredArgsConstructor
@ProtocolCmd(value = FrameType.REAL_TIME_DATA_UP, protocols = {V15, V16, V17})
public class YkcV15RealTimeDataUpCmd extends YkcCmdExecutor { // 实时监测数据
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

		/**
		 * 0x00：离线
		 * 0x01：故障
		 * 0x02：空闲
		 * 0x03：充电
		 */
		byte gunStatus = body.readByte(); // 状态

		byte gw = body.readByte();// 枪是否归位   0x00 否 0x01 是0x02 未知
		byte cq = body.readByte(); // 是否插枪 0x00 否 0x01 是

		// 输出电压
		BigDecimal outVol = new BigDecimal(body.readUnsignedShortLE()).multiply(new BigDecimal("0.1")).setScale(1, RoundingMode.DOWN);

		// 输出电流
		BigDecimal outCurrent = new BigDecimal(body.readUnsignedShortLE()).multiply(new BigDecimal("0.1")).setScale(1, RoundingMode.DOWN);

		// 枪线温度
		byte gunTempByte = body.readByte();
		Integer gunTemp = gunStatus == 0x03 ? gunTempByte - 50 : 0; // 空闲时枪线温度 0

		// 枪线编码
		Long gunNum = body.readLongLE();

		// soc
		byte soc = body.readByte();

		// 电池组最高温度
		byte batteryMaxTempByte = body.readByte();
		Integer batteryMaxTemp = gunStatus == 0x03 ? batteryMaxTempByte - 50 : 0;

		// 累计充电时间 min
		int totalChargeTime = body.readUnsignedShortLE();

		// 剩余充电时间
		int remainChargeTime = body.readUnsignedShortLE();

		// 充电度数
		BigDecimal chargeKwh = new BigDecimal(body.readUnsignedIntLE()).multiply(new BigDecimal("0.0001")).setScale(4, RoundingMode.DOWN);

		// 计损充电度数
		BigDecimal sunChargeKwh = new BigDecimal(body.readUnsignedIntLE()).multiply(new BigDecimal("0.0001")).setScale(4, RoundingMode.DOWN);

		// 已充金额
		BigDecimal chargeAmount = new BigDecimal(body.readUnsignedIntLE()).multiply(new BigDecimal("0.0001")).setScale(4, RoundingMode.DOWN);

		// 硬件故障
		byte[] errorBytes = new byte[2];
		body.readBytes(errorBytes);

		List<String> exceptionInfo = YkcExceptionUtil.getRealTimeDataError(errorBytes);

		log.info("【云快充】{} ⬆️ 实时监测数据 - 交易流水号: {} 桩号: {} 枪号: {} 枪状态: {}[{}] 枪是否归位: {}[{}] 是否插枪: {}[{}] 输出电压: {}V 输出电流: {}A 枪线温度: {}℃ 枪线编码: {} SOC: {}% 电池组最高温度: {}℃ 累计充电时间: {}min 剩余时间: {}min 充电度数: {}° 计损充电度数: {}° 已充金额: {}元 硬件故障: {}",
			versionLabel, tradeNo, deviceId, gunNo, gunStatus, getGunStatusDesc(gunStatus), gw, getGwDesc(gw), cq, getCqDesc(cq), outVol, outCurrent, gunTemp, gunNum, soc, batteryMaxTemp, totalChargeTime, remainChargeTime, chargeKwh, sunChargeKwh, chargeAmount, exceptionInfo);
	}

	private String getGunStatusDesc(byte gunStatus) {
		return switch (gunStatus) {
			case 0x00 -> "离线";
			case 0x01 -> "故障";
			case 0x02 -> "空闲";
			case 0x03 -> "充电";
			default -> "-";
		};
	}

	private String getGwDesc(byte gw) {
		return switch (gw) {
			case 0x00 -> "否";
			case 0x01 -> "是";
			case 0x02 -> "未知";
			default -> "-";
		};
	}

	private String getCqDesc(byte cq) {
		return switch (cq) {
			case 0x00 -> "否";
			case 0x01 -> "是";
			default -> "-";
		};
	}
}
