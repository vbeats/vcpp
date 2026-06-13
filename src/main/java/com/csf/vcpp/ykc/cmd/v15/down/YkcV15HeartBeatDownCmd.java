package com.csf.vcpp.ykc.cmd.v15.down;

import cn.hutool.core.util.HexUtil;
import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcCmdExecutor;
import com.csf.vcpp.ykc.model.YkcMsgData;
import com.csf.vcpp.ykc.utils.YkcAckUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.*;

@Service
@Slf4j
@RequiredArgsConstructor
@ProtocolCmd(value = FrameType.HEARTBEAT_ACK, protocols = {V15, V16, V17, V18, V20})
public class YkcV15HeartBeatDownCmd extends YkcCmdExecutor { // 心跳应答

	@Override
	public void execute(Channel channel, String versionLabel, YkcMsgData data) {
		// 心跳应答
		ByteBuf msgBody = Unpooled.buffer(9);
		msgBody.writeBytes(data.getDeviceIdBytes());
		msgBody.writeByte(data.getGunNo());
		msgBody.writeByte(0);  // 固定 0

		ByteBuf res = YkcAckUtil.createAck(data.getSequence(), data.getEncryptFlag(), FrameType.HEARTBEAT_ACK, msgBody);
		log.info("【云快充】{} ⬇️ 心跳💓应答Hex: {}", versionLabel, new String(HexUtil.encodeHex(res.array())).toUpperCase());

		channel.writeAndFlush(res);
	}
}
