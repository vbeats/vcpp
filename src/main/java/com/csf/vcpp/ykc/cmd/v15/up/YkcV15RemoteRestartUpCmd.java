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
@ProtocolCmd(value = FrameType.REMOTE_RESTART_ACK, protocols = {V15, V16, V17})
public class YkcV15RemoteRestartUpCmd extends YkcCmdExecutor { // 远程重启应答
	@Override
	public void execute(Channel channel, String versionLabel, YkcMsgData data) {
		ByteBuf body = data.getMsgBody();

		byte[] deviceIdBytes = new byte[7];
		body.readBytes(deviceIdBytes);
		Long deviceId = BcdUtil.bcdBytesToLong(deviceIdBytes);  // 桩号

		byte res = body.readByte(); // 0x00 失败 0x01 成功

		log.info("【云快充】{} ⬆️ 远程重启应答 - 桩号: {} 重启结果: {}[{}]", versionLabel, deviceId, res, getResDesc(res));
	}

	private String getResDesc(byte res) {
		return res == 0x01 ? "成功" : "失败";
	}
}
