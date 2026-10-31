package com.csf.vcpp.ykc;

import com.csf.vcpp.utils.CrcUtil;
import com.csf.vcpp.ykc.cmd.v15.down.YkcV15LoginDownCmd;
import com.csf.vcpp.ykc.cmd.v15.down.YkcV15TimeSyncDownCmd;
import com.csf.vcpp.ykc.cmd.v15.up.YkcV15LoginUpCmd;
import com.csf.vcpp.ykc.context.YkcServerContext;
import com.csf.vcpp.ykc.executor.YkcCmdExecutor;
import com.csf.vcpp.ykc.router.YkcUpDataRouter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 上行链路冒烟测试：拆帧 -> 路由 -> 执行器 -> 组应答帧，并验证 ByteBuf 无泄漏。
 */
class YkcUpLinkSmokeTest {

	private static final String DEVICE_NO_BCD = "12345678901234";

	@AfterEach
	void tearDown() {
		YkcServerContext.EXECUTER_MAP.clear();
	}

	@Test
	void loginFrameIsDecodedAckedAndReleased() {
		YkcCmdExecutor loginUp = new YkcV15LoginUpCmd(new YkcV15LoginDownCmd(), new YkcV15TimeSyncDownCmd());
		YkcServerContext.EXECUTER_MAP.put("V15_1", loginUp);

		YkcUpDataRouter router = new YkcUpDataRouter();
		EmbeddedChannel channel = new EmbeddedChannel();

		ByteBuf frame = buildLoginFrame();
		router.route(channel, "V15", frame);

		// 1. 入参 ByteBuf 已被释放
		assertEquals(0, frame.refCnt(), "上行 ByteBuf 必须被释放");

		// 2. 产生了登录应答帧 0x02
		Object outbound = channel.readOutbound();
		assertNotNull(outbound, "必须下发登录应答帧");
		assertTrue(outbound instanceof ByteBuf);

		ByteBuf ack = (ByteBuf) outbound;
		try {
			assertEquals((byte) 0x68, ack.getByte(0));      // 起始符
			assertEquals(4 + 15, ack.getByte(1));           // 数据长度 = 序列号2+加密1+类型1 + 消息体15
			assertEquals((byte) 0x00, ack.getByte(4));      // 加密标志
			assertEquals((byte) 0x02, ack.getByte(5));      // 帧类型 0x02 登录应答
			assertEquals(DEVICE_NO_BCD, hex(ack, 6, 7));    // 桩号回显
			assertEquals((byte) 0x00, ack.getByte(13));     // 登录结果：成功
			assertEquals(23, ack.readableBytes());          // 起始符1 + 长度1 + 数据域19 + CRC2
		} finally {
			ack.release();
		}

		// 3. 桩号与 Channel 建立映射
		YkcServerContext.bind(channel, DEVICE_NO_BCD);
		assertEquals(channel, YkcServerContext.getChannel(DEVICE_NO_BCD));
		assertEquals(DEVICE_NO_BCD, YkcServerContext.getDeviceNo(channel));

		// 4. 解绑后映射消失，且重复解绑安全
		YkcServerContext.unbind(channel);
		assertNull(YkcServerContext.getDeviceNo(channel));
		assertNull(YkcServerContext.getChannel(DEVICE_NO_BCD));
	}

	@Test
	void crcErrorFrameIsDiscardedAndReleased() {
		YkcCmdExecutor loginUp = new YkcV15LoginUpCmd(new YkcV15LoginDownCmd(), new YkcV15TimeSyncDownCmd());
		YkcServerContext.EXECUTER_MAP.put("V15_1", loginUp);

		YkcUpDataRouter router = new YkcUpDataRouter();
		EmbeddedChannel channel = new EmbeddedChannel();

		ByteBuf frame = buildLoginFrame();
		// 破坏 CRC
		frame.setByte(frame.writerIndex() - 1, 0x00);

		router.route(channel, "V15", frame);

		assertEquals(0, frame.refCnt(), "CRC 校验失败的帧也必须释放");
		assertNull(channel.readOutbound(), "CRC 校验失败不应下发应答");
	}

	private ByteBuf buildLoginFrame() {
		ByteBuf body = Unpooled.buffer();
		body.writeBytes(hexToBytes(DEVICE_NO_BCD)); // 桩编号 BCD 7B
		body.writeByte(0x00);                       // 桩类型：直流
		body.writeByte(1);                          // 枪数量
		body.writeByte(0x15);                       // 通信协议版本 BCD 1.5
		body.writeBytes("V1.0.0.0".getBytes(StandardCharsets.US_ASCII)); // mcu 程序版本 8B ASCII
		body.writeByte(0x00);                       // 网络连接类型：SIM 卡
		body.writeZero(10);                         // SIM 卡号 BCD 10B
		body.writeByte(0x00);                       // 运营商：移动

		int dataLength = 4 + body.readableBytes(); // 序列号2 + 加密1 + 帧类型1 + 消息体
		ByteBuf frame = Unpooled.buffer();
		frame.writeByte(0x68);
		frame.writeByte(dataLength);
		frame.writeShortLE(1);       // 序列号
		frame.writeByte(0x00);       // 加密标志
		frame.writeByte(0x01);       // 帧类型：登录
		frame.writeBytes(body);
		body.release();

		byte[] data = new byte[dataLength];
		frame.getBytes(2, data);
		// CRC16-Modbus 结果小端写入：低字节在前（与 YkcAckUtil 组帧保持一致）
		int crc = CrcUtil.crc16(data);
		frame.writeByte((crc >> 8) & 0xFF);
		frame.writeByte(crc & 0xFF);
		return frame;
	}

	private static String hex(ByteBuf buf, int index, int length) {
		byte[] bytes = new byte[length];
		buf.getBytes(index, bytes);
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(String.format("%02X", b));
		}
		return sb.toString();
	}

	private static byte[] hexToBytes(String text) {
		byte[] bytes = new byte[text.length() / 2];
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = (byte) Integer.parseInt(text.substring(i * 2, i * 2 + 2), 16);
		}
		return bytes;
	}
}
