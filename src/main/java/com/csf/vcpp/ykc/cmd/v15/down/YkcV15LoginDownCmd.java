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
@ProtocolCmd(value = FrameType.LOGIN_ACK, protocols = {V15, V16, V17, V18})
public class YkcV15LoginDownCmd extends YkcCmdExecutor { // 登录应答

	@Override
	public void execute(Channel channel, String versionLabel, YkcMsgData data) {
		ByteBuf msgBody = Unpooled.buffer(8);
		msgBody.writeBytes(data.getDeviceIdBytes());
		msgBody.writeByte(data.getLoginRes()); // 登录结果

		ByteBuf res = YkcAckUtil.createAck(data.getSequence(), data.getEncryptFlag(), FrameType.LOGIN_ACK, msgBody);

		log.info("【云快充】{} ⬇️ 登录请求应答Hex: {}", versionLabel, new String(HexUtil.encodeHex(res.array())).toUpperCase());

		channel.writeAndFlush(res);
	}
}
