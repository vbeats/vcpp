package com.csf.vcpp.ykc.cmd.v15.down;

import cn.hutool.core.util.HexUtil;
import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.utils.BcdUtil;
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
@ProtocolCmd(value = FrameType.REMOTE_STOP, protocols = {V15, V16, V17})
public class YkcV15RemoteStopDownCmd extends YkcCmdExecutor { // 运营平台远程停机

	@Override
	public void execute(Channel channel, String versionLabel, YkcMsgData data) {
		ByteBuf msgBody = Unpooled.buffer(8);
		msgBody.writeBytes(data.getDeviceIdBytes());

		msgBody.writeByte(data.getGunNo()); // 枪号

		log.info("【云快充】{} ⬇️ 运营平台远程停机 - 桩号: {} 枪号: {}", versionLabel, BcdUtil.bcdBytesToLong(data.getDeviceIdBytes()), data.getGunNo());

		ByteBuf res = YkcAckUtil.createAck(0, data.getEncryptFlag(), FrameType.REMOTE_STOP, msgBody);

		log.info("【云快充】{} ⬇️ 运营平台远程停机Hex: {}", versionLabel, new String(HexUtil.encodeHex(res.array())).toUpperCase());

		channel.writeAndFlush(res);
	}

}
