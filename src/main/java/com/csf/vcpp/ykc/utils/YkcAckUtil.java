package com.csf.vcpp.ykc.utils;

import cn.hutool.core.io.checksum.crc16.CRC16Modbus;
import com.csf.vcpp.ykc.enums.FrameType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class YkcAckUtil {  // 应答工具类

	/**
	 * 应答数据包
	 * 帧结构: [0x68(1B)] [dataLen(1B)] [sequence(2B)] [encryptFlag(1B)] [frameType(1B)] [msgBody(nB)] [crc16(2B)]
	 *
	 * @param sequence    序列号
	 * @param encryptFlag 加密标志
	 * @param frameType   帧类型
	 * @param msgBody     消息体
	 * @return 应答数据 ByteBuf
	 */
	public static ByteBuf createAck(int sequence, short encryptFlag, FrameType frameType, ByteBuf msgBody) {

		int msgBodyLength = msgBody.readableBytes();
		// 总长度 = head(2) + info(4) + msgBody(n) + crc(2)
		int totalLength = 2 + 4 + msgBodyLength + 2;

		// 一次性分配完整缓冲区，避免中间对象和多次拷贝
		ByteBuf ack = Unpooled.buffer(totalLength, totalLength);

		// head: 起始符 + 数据域长度
		ack.writeByte(0x68);
		ack.writeByte(msgBodyLength + 4); // data length = info(4) + msgBody(n)

		// info: 序列号 + 加密标志 + 帧类型
		ack.writeShort(sequence);
		ack.writeByte(encryptFlag);
		ack.writeByte(frameType.getType());

		// 记录 data 域起始位置，用于 CRC 计算
		int dataStartIndex = 2; // data 从第3个字节开始 (跳过 head)
		int dataLength = 4 + msgBodyLength;

		// msgBody
		ack.writeBytes(msgBody);

		// 计算 CRC16：对 data 域（info + msgBody）计算
		byte[] dataBytes = new byte[dataLength];
		ack.getBytes(dataStartIndex, dataBytes);
		CRC16Modbus modbus = new CRC16Modbus();
		modbus.update(dataBytes);
		long crcValue = modbus.getValue();

		// 写入 CRC（小端序：低字节在前，高字节在后）
		ack.writeByte((int) (crcValue & 0xFF));
		ack.writeByte((int) ((crcValue >> 8) & 0xFF));

		return ack;
	}
}
