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
@ProtocolCmd(value = FrameType.CHARGING_BMS_STATUS, protocols = {V15, V16, V17})
public class YkcV15ChargingBmsStatusUpCmd extends YkcCmdExecutor { // 充电过程BMS信息
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

		// BMS 最高单体动力蓄电池电压所在编号
		int maxVoltageCell = body.readByte() + 1;  // 1~256

		// BMS 最高动力蓄电池温度
		int maxTemperature = body.readByte() - 50; // -50~ 200

		// 最高温度检测点编号
		int maxTemperatureCell = body.readByte() + 1; // 1~128

		// 最低动力蓄电池温度
		int minTemperature = body.readByte() - 50; // -50~ 200

		// 最低动力蓄电池温度检测点编号
		int minTemperatureCell = body.readByte() + 1; // 1~128

		byte p1 = body.readByte();

		// BMS 单体动力蓄电池电压过高/过低     (<00>：=正常; <01>：=过高; <10>：=过低)
		byte voltageStatus = (byte) (p1 & 0x03);

		// BMS 整车动力蓄电池荷电状态SOC 过高/过低   (<00>：=正常; <01>：=过高; <10>：=过低)
		byte socStatus = (byte) (p1 >> 2 & 0x03);

		// BMS 动力蓄电池充电过电流    (<00>：=正常; <01>：=过流; <10>：=不可信状态)
		byte currentStatus = (byte) (p1 >> 4 & 0x03);

		// BMS 动力蓄电池温度过高     (<00>：=正常; <01>：=过高; <10>：=不可信状态)
		byte temperatureStatus = (byte) (p1 >> 6 & 0x03);

		byte p2 = body.readByte();

		// BMS 动力蓄电池绝缘状态   (<00>：=正常; <01>：=过流; <10>：=不可信状态)
		byte insulationStatus = (byte) (p2 & 0x03);

		// BMS 动力蓄电池组输出连接器连接状态   (<00>：=正常; <01>：=过流; <10>：=不可信状态)
		byte connectionStatus = (byte) (p2 >> 2 & 0x03);

		// 充电禁止 (<00>：=禁止; <01>：=允许)
		byte chargeStatus = (byte) (p2 >> 4 & 0x03);

		log.info("【云快充】{} ⬆️ 充电过程BMS信息 - 交易流水号: {} 桩号: {} 枪号: {} BMS 最高单体动力蓄电池电压所在编号: {} BMS 最高动力蓄电池温度: {}℃ BMS 最高温度检测点编号: {} BMS 最低动力蓄电池温度: {}℃ BMS 最低动力蓄电池温度检测点编号: {} BMS 单体动力蓄电池电压过高/过低: {}[{}] BMS 整车动力蓄电池荷电状态SOC 过高/过低: {}[{}] BMS 动力蓄电池充电过电流: {}[{}] BMS 动力蓄电池温度过高: {}[{}] BMS 动力蓄电池绝缘状态: {}[{}] BMS 动力蓄电池组输出连接器连接状态: {}[{}] BMS 充电禁止: {}[{}]",
			versionLabel, tradeNo, deviceId, gunNo, maxVoltageCell, maxTemperature, maxTemperatureCell, minTemperature, minTemperatureCell, voltageStatus, getStatusDesc1(voltageStatus), socStatus, getStatusDesc1(socStatus), currentStatus, getStatusDesc3(currentStatus), temperatureStatus, getStatusDesc2(temperatureStatus), insulationStatus, getStatusDesc3(insulationStatus), connectionStatus, getStatusDesc3(connectionStatus), chargeStatus, getStatusDesc4(chargeStatus));
	}

	private String getStatusDesc1(byte status) {
		return switch (status) {
			case 0x00 -> "正常";
			case 0x01 -> "过高";
			case 0x10 -> "过低";
			default -> "-";
		};
	}

	private String getStatusDesc2(byte status) {
		return switch (status) {
			case 0x00 -> "正常";
			case 0x01 -> "过高";
			case 0x10 -> "不可信状态";
			default -> "-";
		};
	}

	private String getStatusDesc3(byte status) {
		return switch (status) {
			case 0x00 -> "正常";
			case 0x01 -> "过流";
			case 0x10 -> "不可信状态";
			default -> "-";
		};
	}

	private String getStatusDesc4(byte status) {
		return switch (status) {
			case 0x00 -> "禁止";
			case 0x01 -> "允许";
			default -> "-";
		};
	}
}
