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
@ProtocolCmd(value = FrameType.REMOTE_CHARGE, protocols = {V15, V16, V17})
public class YkcV15RemoteChargeDownCmd extends YkcCmdExecutor { // 运营平台远程控制启机

	@Override
	public void execute(Channel channel, String versionLabel, YkcMsgData data) {
		ByteBuf msgBody = Unpooled.buffer(44);

		long deviceId = BcdUtil.bcdBytesToLong(data.getDeviceIdBytes());

		msgBody.writeBytes(BcdUtil.numStrToBcdBytes(data.getTradeNo()));
		msgBody.writeBytes(data.getDeviceIdBytes());

		msgBody.writeByte(data.getGunNo()); // 枪号

		// 逻辑卡号
		msgBody.writeBytes(BcdUtil.numStrToBcdBytes("0000000000000000"));

		// 物理卡号
		msgBody.writeBytes(BcdUtil.numStrToBcdBytes("0000000000000000"));

		// 账户余额
		msgBody.writeInt(0);

		log.info("【云快充】{} ⬇️ 运营平台远程控制启机 - 交易流水号: {} 桩号: {} 枪号: {} ", versionLabel, data.getTradeNo(), deviceId, data.getGunNo());

		ByteBuf res = YkcAckUtil.createAck(0, data.getEncryptFlag(), FrameType.REMOTE_CHARGE, msgBody);

		log.info("【云快充】{} ⬇️ 运营平台远程控制启机Hex: {}", versionLabel, new String(HexUtil.encodeHex(res.array())).toUpperCase());

		channel.writeAndFlush(res);
	}

}
