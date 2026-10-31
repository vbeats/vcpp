package com.csf.vcpp.ykc.router;

import com.csf.vcpp.utils.CP56Time2aUtil;
import com.csf.vcpp.utils.CrcUtil;
import com.csf.vcpp.ykc.context.YkcServerContext;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.enums.YkcVersion;
import com.csf.vcpp.ykc.executor.YkcCmdExecutor;
import com.csf.vcpp.ykc.model.YkcMsgData;
import com.csf.vcpp.ykc.utils.YkcAckUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 云快充上行报文拆帧 + 路由。
 * <p>
 * 内存约定：入参 {@code frame} 的所有权归本方法，无论解析成功、CRC 失败还是抛异常，
 * 都在 finally 中释放；派生出的消息体 {@code msgBody} 由 {@link #routeMsg} 的 finally 释放。
 * 执行器内部不得重复释放消息体。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class YkcUpDataRouter {

	/**
	 * 上行报文路由入口。
	 *
	 * @param frame 完整的单帧数据（所有权转移，方法内部负责释放）
	 */
	public void route(Channel channel, String versionLabel, ByteBuf frame) {
		if (frame == null || !frame.isReadable()) {
			YkcAckUtil.release(frame);
			return;
		}
		try {
			YkcMsgData msgData = decode(versionLabel, frame);
			if (msgData == null) {
				return;
			}
			YkcServerContext.touch(channel);
			routeMsg(channel, versionLabel, msgData);
		} catch (Exception e) {
			log.error("【云快充】{} ⬆️ data 拆帧异常, hex: {}", versionLabel, ByteBufUtil.hexDump(frame), e);
		} finally {
			YkcAckUtil.release(frame);
		}
	}

	/**
	 * 拆帧：起始符 / 数据长度 / 序列号 / 发送时间 / 加密标志 / 帧类型 / 消息体 / CRC
	 *
	 * @return 帧数据；校验失败返回 null
	 */
	private YkcMsgData decode(String versionLabel, ByteBuf frame) {
		YkcVersion version = YkcVersion.of(versionLabel);
		int lengthFieldLength = version.getLengthFieldLength();
		int headerLength = 1 + lengthFieldLength;

		if (frame.readableBytes() < headerLength) {
			log.warn("【云快充】{} ⬆️ data 帧长度不足: {}", versionLabel, frame.readableBytes());
			return null;
		}

		frame.skipBytes(1); // 起始符 0x68

		int dataLength = lengthFieldLength == 2 ? frame.readUnsignedShort() : frame.readUnsignedByte();
		int infoLength = version.isSendTimeField() ? 11 : 4;

		if (dataLength < infoLength) {
			log.warn("【云快充】{} ⬆️ data 数据长度非法: {}", versionLabel, dataLength);
			return null;
		}
		if (frame.readableBytes() < dataLength + 2) {
			log.warn("【云快充】{} ⬆️ data 帧长度不足, 期望: {} 实际: {}", versionLabel, dataLength + 2, frame.readableBytes());
			return null;
		}

		// CRC 待校验数据域
		byte[] data = new byte[dataLength];
		frame.getBytes(headerLength, data);

		int sequence = frame.readUnsignedShortLE(); // 2字节 序列号（小端）

		LocalDateTime eventTime = null;
		if (version.isSendTimeField()) {
			byte[] timeBytes = new byte[7];
			frame.readBytes(timeBytes);
			eventTime = CP56Time2aUtil.decode(timeBytes); // CP56Time2a 数据帧发送时间
		}

		byte encryptFlag = frame.readByte(); // 1字节 加密标志 0x00 不加密 0x01 加密
		short frameTypeValue = frame.readUnsignedByte(); // 1字节 帧类型
		ByteBuf msgBody = frame.readBytes(dataLength - infoLength); // 消息体
		int crc = frame.readUnsignedShort(); // CRC 低字节在前

		if (log.isDebugEnabled()) {
			log.debug("【云快充】{} data Hex ⬆️: {} eventTime: {}", versionLabel, ByteBufUtil.hexDump(frame, 0, frame.capacity()), eventTime);
		}

		if (!CrcUtil.checkCrc(data, crc)) {
			log.error("【云快充】{} data ⬆️ crc校验未通过, 帧类型: {}", versionLabel, frameTypeValue);
			YkcAckUtil.release(msgBody);
			return null;
		}

		YkcMsgData msgData = new YkcMsgData();
		msgData.setSequence(sequence);
		msgData.setEventTime(eventTime);
		msgData.setEncryptFlag(encryptFlag);
		msgData.setFrameType(frameTypeValue);
		msgData.setMsgBody(msgBody);
		return msgData;
	}

	private void routeMsg(Channel channel, String versionLabel, YkcMsgData msgData) {
		FrameType frameType = FrameType.getByValue(msgData.getFrameType());
		YkcCmdExecutor cmdExecutor = YkcServerContext.EXECUTER_MAP.get(versionLabel + "_" + msgData.getFrameType());
		if (cmdExecutor == null) {
			log.warn("【云快充】{} ⬆️ data 帧类型未实现cmd executor: 0x{}",
					versionLabel, Integer.toHexString(msgData.getFrameType() & 0xFF).toUpperCase());
			return;
		}

		try {
			cmdExecutor.execute(channel, versionLabel, msgData);
		} catch (Exception e) {
			log.error("【云快充】{} ⬆️ data 解析异常, 帧类型: {}", versionLabel, frameType, e);
		} finally {
			YkcAckUtil.release(msgData.getMsgBody());
			msgData.setMsgBody(null);
		}
	}
}
