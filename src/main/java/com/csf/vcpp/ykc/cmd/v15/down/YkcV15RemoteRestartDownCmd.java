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
@ProtocolCmd(value = FrameType.REMOTE_RESTART, protocols = {V15, V16, V17})
public class YkcV15RemoteRestartDownCmd extends YkcCmdExecutor { // 远程重启
	@Override
	public void execute(Channel channel, String versionLabel, YkcMsgData data) {
		ByteBuf msgBody = Unpooled.buffer(8);

		long deviceId = BcdUtil.bcdBytesToLong(data.getDeviceIdBytes());

		msgBody.writeBytes(data.getDeviceIdBytes());

		// 0x01：立即执行
		//0x02：空闲执行
		msgBody.writeByte(0x01);

		log.info("【云快充】{} ⬇️ 远程重启 - 桩号: {} ", versionLabel, deviceId);

		ByteBuf res = YkcAckUtil.createAck(0, data.getEncryptFlag(), FrameType.REMOTE_RESTART, msgBody);

		log.info("【云快充】{} ⬇️ 远程重启Hex: {}", versionLabel, new String(HexUtil.encodeHex(res.array())).toUpperCase());

		channel.writeAndFlush(res);
	}
}
