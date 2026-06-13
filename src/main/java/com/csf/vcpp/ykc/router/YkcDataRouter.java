package com.csf.vcpp.ykc.router;

import cn.hutool.core.util.HexUtil;
import com.csf.vcpp.utils.CrcUtil;
import com.csf.vcpp.ykc.context.YkcServerContext;
import com.csf.vcpp.ykc.executor.YkcCmdExecutor;
import com.csf.vcpp.ykc.model.YkcMsgData;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class YkcDataRouter {
	public void route(Channel channel, String versionLabel, String msg) {
		log.info("【云快充】{} data ⬆️: {}", versionLabel, msg);

		ByteBuf byteBuf = Unpooled.copiedBuffer(HexUtil.decodeHex(msg));
		byteBuf.skipBytes(1);

		short dataLength = byteBuf.readUnsignedByte();// 1字节 数据长度

		// crc 待校验数据
		byte[] data = new byte[dataLength];
		byteBuf.getBytes(2, data);

		// ---------- msg
		int sequence = byteBuf.readUnsignedShortLE();  // 2字节 序列号

		byte encryptFlag = byteBuf.readByte();    // 1字节 加密标志 0x00 不加密  0x01 加密

		short frameType = byteBuf.readUnsignedByte(); // 2字节 帧类型

		ByteBuf msgBody = byteBuf.readBytes(dataLength - 4);// 消息体 长度 = 数据长度 - 序列号 - 加密标志 - 帧类型

		int crc = byteBuf.readUnsignedShort();

		if (!CrcUtil.checkCrc(data, crc)) {
			log.error("【云快充】{} data ⬆️ crc校验未通过...", versionLabel);
			return;
		}

		YkcMsgData msgData = new YkcMsgData();
		msgData.setSequence(sequence);
		msgData.setEncryptFlag(encryptFlag);
		msgData.setFrameType(frameType);
		msgData.setMsgBody(msgBody);

		this.routeMsg(channel, versionLabel, msgData);
	}

	private void routeMsg(Channel channel, String versionLabel, YkcMsgData msgData) {
		YkcCmdExecutor cmdExecutor = YkcServerContext.EXECUTER_MAP.get(versionLabel + "_" + msgData.getFrameType());
		if (ObjectUtils.isEmpty(cmdExecutor)) {
			log.warn("【云快充】{} data ⬆️ 未实现的帧类型 {}", versionLabel, msgData.getFrameType());
			return;
		}

		try {
			cmdExecutor.execute(channel, versionLabel, msgData);
		} catch (Exception e) {
			log.error("【云快充】{} data ⬆️ 执行业务逻辑异常...", versionLabel, e);
		} finally {
			ByteBuf body = msgData.getMsgBody();
			if (body.refCnt() > 0) {
				body.release();
			}
		}
	}
}
