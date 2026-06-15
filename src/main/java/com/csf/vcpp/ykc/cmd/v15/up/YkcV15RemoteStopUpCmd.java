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
@ProtocolCmd(value = FrameType.REMOTE_STOP_ACK, protocols = {V15, V16, V17})
public class YkcV15RemoteStopUpCmd extends YkcCmdExecutor { // 远程停机命令回复

	@Override
	public void execute(Channel channel, String versionLabel, YkcMsgData data) {
		ByteBuf body = data.getMsgBody();

		byte[] deviceIdBytes = new byte[7];
		body.readBytes(deviceIdBytes);
		Long deviceId = BcdUtil.bcdBytesToLong(deviceIdBytes);  // 桩号

		byte gunNo = body.readByte(); // 枪号

		byte res = body.readByte(); // 停止结果  0x00失败 0x01成功

		/**
		 * 0x00 无
		 * 0x01 设备编号不匹配
		 * 0x02 枪未处于充电状态
		 * 0x03 其他
		 */
		byte errCode = body.readByte();

		log.info("【云快充】{} ⬆️ 远程停机命令回复 - 桩号: {} 枪号: {} 停止结果: {}[{}] errCode: {}[{}]", versionLabel, deviceId, gunNo, res, getResDesc(res), errCode, getErrCodeDesc(errCode));
	}

	private String getResDesc(byte res) {
		return res == 0x01 ? "成功" : "失败";
	}

	private String getErrCodeDesc(byte errCode) {
		return switch (errCode) {
			case 0x00 -> "无";
			case 0x01 -> "设备编号不匹配";
			case 0x02 -> "枪未处于充电状态";
			case 0x03 -> "其他";
			default -> "-";
		};
	}
}
