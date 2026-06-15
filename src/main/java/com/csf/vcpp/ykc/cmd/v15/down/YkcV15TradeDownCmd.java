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

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V15;
import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V16;

@Service
@Slf4j
@RequiredArgsConstructor
@ProtocolCmd(value = FrameType.TRADE_ACK, protocols = {V15, V16})
public class YkcV15TradeDownCmd extends YkcCmdExecutor { // 交易记录确认
	@Override
	public void execute(Channel channel, String versionLabel, YkcMsgData data) {
		ByteBuf msgBody = Unpooled.buffer(17);
		msgBody.writeBytes(BcdUtil.numStrToBcdBytes(data.getTradeNo()));
		msgBody.writeByte(0x00); //0x00上传成功 0x01非法账单

		ByteBuf res = YkcAckUtil.createAck(data.getSequence(), data.getEncryptFlag(), FrameType.TRADE_ACK, msgBody);
		log.info("【云快充】{} ⬇️ 交易记录 {} - 交易记录确认Hex: {}", versionLabel, data.getTradeNo(), new String(HexUtil.encodeHex(res.array())).toUpperCase());
		channel.writeAndFlush(res);
	}
}
