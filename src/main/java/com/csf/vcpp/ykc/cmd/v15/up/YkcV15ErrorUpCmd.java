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

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.*;

@Service
@Slf4j
@RequiredArgsConstructor
@ProtocolCmd(value = FrameType.ERROR, protocols = {V15, V16, V17})
public class YkcV15ErrorUpCmd extends YkcCmdExecutor { // 错误报文
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

		//<00>：=正常；<01>：=超时； <10>：=不可信状态

		// ---------------- 8个字节 ----------------
		byte p1 = body.readByte();

		// 接收SPN2560=0x00 的充电机辨识报文超时
		byte spn256000 = (byte) (p1 & 0x03);

		// 接收SPN2560=0xAA 的充电机辨识报文超时
		byte spn2560AA = (byte) (p1 >> 2 & 0x03);

		// 预留位

		byte p2 = body.readByte();

		// 接收充电机的时间同步和充电机最大输出能力报文超时
		byte timeSync = (byte) (p2 & 0x03);

		// 接收充电机完成充电准备报文超时
		byte chargePrepare = (byte) (p2 >> 2 & 0x03);

		// 预留位

		byte p3 = body.readByte();

		// 接收充电机充电状态报文超时
		byte chargeStatus = (byte) (p3 & 0x03);

		// 接收充电机中止充电报文超时
		byte stopCharge = (byte) (p3 >> 2 & 0x03);

		// 预留位
		byte p4 = body.readByte();

		// 接收充电机充电统计报文超时
		byte chargeStatistics = (byte) (p4 & 0x03);

		// bms 其他
		byte p5 = body.readByte();

		// 接收BMS 和车辆的辨识报文超时
		byte bmsVin = (byte) (p5 & 0x03);

		// 预留位
		byte p6 = body.readByte();

		// 接收电池充电参数报文超时
		byte batteryChargeParams = (byte) (p6 & 0x03);

		// 接收BMS 完成充电准备报文超时
		byte bmsChargePrepare = (byte) (p6 >> 2 & 0x03);

		// 预留位
		byte p7 = body.readByte();

		// 接收电池充电总状态报文超时
		byte batteryChargeStatus = (byte) (p7 & 0x03);

		// 接收电池充电要求报文超时
		byte batteryChargeRequirements = (byte) (p7 >> 2 & 0x03);

		// 接收BMS 中止充电报文超时
		byte bmsStopCharge = (byte) (p7 >> 4 & 0x03);

		// 预留位
		byte p8 = body.readByte();

		// 接收BMS 充电统计报文超时
		byte bmsChargeStatistics = (byte) (p8 & 0x03);

		// 充电机其他

		log.info("""
				【云快充】{} 充电桩 ⬆️ BMS充电错误报文 - 交易流水号: {}
				桩号: {}
				枪号: {}
				接收SPN2560=0x00 的充电机辨识报文超时: {}[{}]
				接收SPN2560=0xAA 的充电机辨识报文超时: {}[{}]
				接收充电机的时间同步和充电机最大输出能力报文超时: {}[{}]
				接收充电机完成充电准备报文超时: {}[{}]
				接收充电机充电状态报文超时: {}[{}]
				接收充电机中止充电报文超时: {}[{}]
				接收充电机充电统计报文超时: {}[{}]
				接收BMS 和车辆的辨识报文超时: {}[{}]
				接收电池充电参数报文超时: {}[{}]
				接收BMS 完成充电准备报文超时: {}[{}]
				接收电池充电总状态报文超时: {}[{}]
				接收电池充电要求报文超时: {}[{}]
				接收BMS 中止充电报文超时: {}[{}]
				接收BMS 充电统计报文超时: {}[{}]
				---------------------------------
				""",
			versionLabel, tradeNo, deviceId, gunNo, spn256000, getDesc(spn256000), spn2560AA, getDesc(spn2560AA), timeSync, getDesc(timeSync), chargePrepare, getDesc(chargePrepare), chargeStatus, getDesc(chargeStatus), stopCharge, getDesc(stopCharge), chargeStatistics, getDesc(chargeStatistics), bmsVin, getDesc(bmsVin), batteryChargeParams, getDesc(batteryChargeParams), bmsChargePrepare, getDesc(bmsChargePrepare), batteryChargeStatus, getDesc(batteryChargeStatus), batteryChargeRequirements, getDesc(batteryChargeRequirements), bmsStopCharge, getDesc(bmsStopCharge), bmsChargeStatistics, getDesc(bmsChargeStatistics));
	}

	private String getDesc(byte status) {
		return switch (status) {
			case 0x00 -> "正常";
			case 0x01 -> "超时";
			case 0x10 -> "不可信状态";
			default -> "-";
		};
	}
}
