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
@ProtocolCmd(value = FrameType.BILLING_VERIFY_MODEL, protocols = {V15, V16, V17, V18, V20})
public class YkcV15BillingVerifyModelDownCmd extends YkcCmdExecutor {
	@Override
	public void execute(Channel channel, String versionLabel, YkcMsgData data) {
		// 应答
		ByteBuf msgBody = Unpooled.buffer(10);
		msgBody.writeBytes(data.getDeviceIdBytes());
		msgBody.writeShort(data.getBillingModelNo());

		//  一律应答 不一致  让桩侧重新获取最新的 计费模型
		msgBody.writeByte(0x01);  // 0x00 一致  0x01 不一致

		ByteBuf res = YkcAckUtil.createAck(data.getSequence(), data.getEncryptFlag(), FrameType.BILLING_VERIFY_MODEL_ACK, msgBody);

		log.info("【云快充】{} ⬇️ 计费模型验证请求应答Hex: {}", versionLabel, new String(HexUtil.encodeHex(res.array())).toUpperCase());

		channel.writeAndFlush(res);
	}
}
