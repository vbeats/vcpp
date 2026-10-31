package com.csf.vcpp.ykc.utils;

import cn.hutool.core.io.checksum.crc16.CRC16Modbus;
import com.csf.vcpp.utils.CP56Time2aUtil;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.enums.YkcVersion;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * 云快充应答帧组包工具。
 * <p>
 * 帧结构：
 * </p>
 * <pre>
 * [0x68 起始符 1B] [数据长度 1B/2B] [序列号 2B] [发送时间 7B(仅V2.1)] [加密标志 1B] [帧类型 1B] [消息体 nB] [CRC16 2B]
 * </pre>
 * <p>
 * 内存约定：{@code msgBody} 的所有权在调用 {@link #createAck} 时转移给本方法，
 * 组包结束后（无论成功失败）都会释放，调用方不得再使用或重复释放。
 * </p>
 */
@Slf4j
public final class YkcAckUtil {

	private YkcAckUtil() {
	}

	/**
	 * 组应答帧（V1.5 ~ V1.8：1 字节数据长度，无发送时间域）
	 */
	public static ByteBuf createAck(int sequence, byte encryptFlag, FrameType frameType, ByteBuf msgBody) {
		return createAck(YkcVersion.V15, sequence, encryptFlag, frameType, msgBody);
	}

	/**
	 * 按协议版本组应答帧。
	 *
	 * @param version    协议版本（决定数据长度域宽度与是否存在发送时间域）
	 * @param sequence   序列号
	 * @param encryptFlag 加密标志
	 * @param frameType  帧类型
	 * @param msgBody    消息体（所有权转移，方法内部负责释放）
	 * @return 待发送的应答帧
	 */
	public static ByteBuf createAck(YkcVersion version, int sequence, byte encryptFlag, FrameType frameType, ByteBuf msgBody) {
		int bodyLength = msgBody == null ? 0 : msgBody.readableBytes();

		// 数据域 = 序列号(2) + [发送时间(7)] + 加密标志(1) + 帧类型(1) + 消息体(n)
		int infoLength = 4 + (version.isSendTimeField() ? 7 : 0);
		int dataLength = infoLength + bodyLength;
		int lengthFieldLength = version.getLengthFieldLength();
		int totalLength = 1 + lengthFieldLength + dataLength + 2;

		ByteBuf ack = Unpooled.buffer(totalLength, totalLength);
		try {
			ack.writeByte(0x68);
			if (lengthFieldLength == 2) {
				ack.writeShort(dataLength);
			} else {
				ack.writeByte(dataLength);
			}

			// 上行按小端解析（readUnsignedShortLE），下行固定按大端回写（writeShort），
			// 即同一序列号值在上下行的 wire 字节顺序相反。这是真实桩侧行为，
			// 已用 148 条上行 / 82 条下行真实报文验证（68 组有鉴别力样本 100% 为字节反转），不可更改。
			ack.writeShort(sequence & 0xFFFF);

			if (version.isSendTimeField()) {
				ack.writeBytes(CP56Time2aUtil.encode(LocalDateTime.now()));
			}

			ack.writeByte(encryptFlag);
			ack.writeByte(frameType.getType());

			if (bodyLength > 0) {
				ack.writeBytes(msgBody);
			}

			// CRC16-Modbus：从数据域首字节到消息体末字节，结果小端写入
			int dataStartIndex = 1 + lengthFieldLength;
			byte[] dataBytes = new byte[dataLength];
			ack.getBytes(dataStartIndex, dataBytes);

			CRC16Modbus modbus = new CRC16Modbus();
			modbus.update(dataBytes);
			long crcValue = modbus.getValue();
			ack.writeByte((int) (crcValue & 0xFF));
			ack.writeByte((int) ((crcValue >> 8) & 0xFF));
		} finally {
			release(msgBody);
		}
		return ack;
	}

	/**
	 * 写出应答帧。通道不可用时直接释放，避免堆积。
	 */
	public static void write(Channel channel, ByteBuf ack) {
		if (channel == null || !channel.isActive()) {
			release(ack);
			return;
		}
		channel.writeAndFlush(ack);
	}

	/**
	 * 释放 ByteBuf，忽略已释放 / 不可释放的情形
	 */
	public static void release(ByteBuf buf) {
		if (buf != null && buf.refCnt() > 0) {
			buf.release();
		}
	}

	/**
	 * 应答帧转 HEX（仅用于日志，调用前确保未写出）
	 */
	public static String hex(ByteBuf ack) {
		return ack == null ? "" : ByteBufUtil.hexDump(ack).toUpperCase();
	}
}
