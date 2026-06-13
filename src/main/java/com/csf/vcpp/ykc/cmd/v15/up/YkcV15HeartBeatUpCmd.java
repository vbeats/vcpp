package com.csf.vcpp.ykc.cmd.v15.up;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.utils.BcdUtil;
import com.csf.vcpp.ykc.cmd.v15.down.YkcV15HeartBeatDownCmd;
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
@ProtocolCmd(value = FrameType.HEARTBEAT, protocols = {V15, V16, V17, V18, V20})
public class YkcV15HeartBeatUpCmd extends YkcCmdExecutor { // 心跳

	private final YkcV15HeartBeatDownCmd ykcV15HeartBeatDownCmd;

	@Override
	public void execute(Channel channel, String versionLabel, YkcMsgData data) {
		ByteBuf body = data.getMsgBody();

		byte[] deviceIdBytes = new byte[7];
		body.readBytes(deviceIdBytes);
		Long deviceId = BcdUtil.bcdBytesToLong(deviceIdBytes);  // 桩号

		byte gunNo = body.readByte(); // 枪号

		byte gunStatus = body.readByte(); // 枪状态 0x00：正常 0x01：故障

		log.info("【云快充】{} ⬆️ 心跳💓 - 桩号 : {} 枪号 : {} 枪状态: {}[{}]", versionLabel, deviceId, gunNo, gunStatus, getGunStatusDesc(gunStatus));

		data.setDeviceIdBytes(deviceIdBytes);
		data.setGunNo(gunNo);
		ykcV15HeartBeatDownCmd.execute(channel, versionLabel, data);
	}

	private String getGunStatusDesc(byte gunStatus) {
		return switch (gunStatus) {
			case 0x00 -> "正常";
			case 0x01 -> "故障";
			default -> "";
		};
	}
}
