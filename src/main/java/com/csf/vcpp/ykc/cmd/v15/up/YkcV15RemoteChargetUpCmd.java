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
@ProtocolCmd(value = FrameType.REMOTE_CHARGE_ACK, protocols = {V15, V16, V17})
public class YkcV15RemoteChargetUpCmd extends YkcCmdExecutor { // 远程启机命令回复

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

		byte res = body.readByte();  // 启动结果 0x00失败 0x01成功

		/**
		 * 0x00 无
		 * 0x01 设备编号不匹配
		 * 0x02 枪已在充电
		 * 0x03 设备故障
		 * 0x04 设备离线
		 * 0x05 未插枪
		 * 桩在收到启充命令后,检测到未插
		 * 枪则发送0x33 报文回复充电失
		 * 败。若在60 秒（以收到0x34 时间
		 * 开始计算）内检测到枪重新连接，
		 * 则补送0x33 成功报文;超时或者
		 * 离线等其他异常，桩不启充、不补
		 * 发0x33 报文
		 */
		byte errCode = body.readByte();


		log.info("【云快充】{} ⬆️ 远程启动充电命令回复 - 交易流水号: {} 桩号: {} 枪号: {} 启动结果: {}[{}] errCode: {}[{}]",
			versionLabel, tradeNo, deviceId, gunNo, res, getResDesc(res), errCode, getErrCodeDesc(errCode));
	}

	private String getResDesc(byte res) {
		return res == 0x01 ? "成功" : "失败";
	}

	private String getErrCodeDesc(byte errCode) {
		return switch (errCode) {
			case 0x00 -> "无";
			case 0x01 -> "设备编号不匹配";
			case 0x02 -> "枪已在充电";
			case 0x03 -> "设备故障";
			case 0x04 -> "设备离线";
			case 0x05 -> "未插枪";
			default -> "-";
		};
	}
}
