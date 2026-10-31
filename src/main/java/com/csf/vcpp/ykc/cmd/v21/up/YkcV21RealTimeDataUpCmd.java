package com.csf.vcpp.ykc.cmd.v21.up;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.utils.BcdUtil;
import com.csf.vcpp.utils.BinUtil;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcCmdExecutor;
import com.csf.vcpp.ykc.model.YkcMsgData;
import com.csf.vcpp.ykc.utils.YkcExceptionUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V21;

/**
 * 0x13 实时监测数据（V2.1）。
 * <p>
 * 相对 V1.5 ~ V1.8 新增：桩体温度(1B) + 烟感状态(1B) + 电表示值(5B)。
 * </p>
 */
@Slf4j
@Service
@ProtocolCmd(value = FrameType.REAL_TIME_DATA_UP, protocols = {V21})
public class YkcV21RealTimeDataUpCmd extends YkcCmdExecutor {

	@Override
	public void execute(Channel channel, String versionLabel, YkcMsgData data) {
		ByteBuf body = data.getMsgBody();

		byte[] tradeNoBytes = new byte[16];
		body.readBytes(tradeNoBytes);
		String tradeNo = BcdUtil.toString(tradeNoBytes);  // 交易流水号

		byte[] deviceNoBytes = new byte[7];
		body.readBytes(deviceNoBytes);
		String deviceNo = BcdUtil.bcdBytesToHex(deviceNoBytes);  // 桩号

		byte gunNo = body.readByte();// 枪号

		byte gunStatus = body.readByte(); // 状态
		byte gw = body.readByte(); // 枪是否归位
		byte cq = body.readByte(); // 是否插枪

		BigDecimal outVol = new BigDecimal(body.readUnsignedShortLE()).multiply(new BigDecimal("0.1")).setScale(1, RoundingMode.DOWN);
		BigDecimal outCurrent = new BigDecimal(body.readUnsignedShortLE()).multiply(new BigDecimal("0.1")).setScale(1, RoundingMode.DOWN);

		byte gunTempByte = body.readByte();
		Integer gunTemp = gunStatus == 0x03 ? gunTempByte - 50 : 0;

		Long gunNum = body.readLongLE(); // 枪线编码
		byte soc = body.readByte();

		byte batteryMaxTempByte = body.readByte();
		Integer batteryMaxTemp = gunStatus == 0x03 ? batteryMaxTempByte - 50 : 0;

		int totalChargeTime = body.readUnsignedShortLE();
		int remainChargeTime = body.readUnsignedShortLE();

		BigDecimal chargeKwh = new BigDecimal(body.readUnsignedIntLE()).multiply(new BigDecimal("0.0001")).setScale(4, RoundingMode.DOWN);
		BigDecimal sunChargeKwh = new BigDecimal(body.readUnsignedIntLE()).multiply(new BigDecimal("0.0001")).setScale(4, RoundingMode.DOWN);
		BigDecimal chargeAmount = new BigDecimal(body.readUnsignedIntLE()).multiply(new BigDecimal("0.0001")).setScale(4, RoundingMode.DOWN);

		byte[] errorBytes = new byte[2];
		body.readBytes(errorBytes);
		List<String> exceptionInfo = YkcExceptionUtil.getRealTimeDataError(errorBytes);

		// ---------- V2.1 新增 ----------
		int pileTemp = body.readableBytes() >= 7 ? body.readByte() - 50 : 0; // 桩体温度
		byte smokeStatus = body.readableBytes() >= 6 ? body.readByte() : 0; // 烟感状态
		BigDecimal meterValue = BigDecimal.ZERO;
		if (body.readableBytes() >= 5) {
			byte[] meterBytes = new byte[5];
			body.readBytes(meterBytes);
			meterValue = new BigDecimal(BinUtil.bin2Long(meterBytes)).multiply(new BigDecimal("0.0001")).setScale(4, RoundingMode.DOWN);
		}

		log.info("""
				【云快充】{} ⬆️ 实时监测数据 - 交易流水号: {}
				桩号: {}
				枪号: {}
				枪状态: {}[{}]
				枪是否归位: {}[{}]
				是否插枪: {}[{}]
				输出电压: {}V
				输出电流: {}A
				枪线温度: {}℃
				枪线编码: {}
				SOC: {}%
				电池组最高温度: {}℃
				累计充电时间: {}min
				剩余时间: {}min
				充电度数: {}°
				计损充电度数: {}°
				已充金额: {}元
				硬件故障: {}
				桩体温度: {}℃
				烟感状态: {}[{}]
				电表示值: {}°
				---------------------------------
				""",
			versionLabel, tradeNo, deviceNo, gunNo, gunStatus, getGunStatusDesc(gunStatus), gw, getGwDesc(gw),
			cq, getCqDesc(cq), outVol, outCurrent, gunTemp, gunNum, soc, batteryMaxTemp,
			totalChargeTime, remainChargeTime, chargeKwh, sunChargeKwh, chargeAmount, exceptionInfo,
			pileTemp, smokeStatus, getSmokeDesc(smokeStatus), meterValue);
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

	private String getSmokeDesc(byte smokeStatus) {
		return switch (smokeStatus) {
			case 0x01 -> "启动";
			case 0x02 -> "空闲";
			case 0x03 -> "报警";
			case 0x04 -> "故障";
			default -> "-";
		};
	}
}
