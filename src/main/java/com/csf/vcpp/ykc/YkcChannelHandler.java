package com.csf.vcpp.ykc;

import com.csf.vcpp.ykc.enums.YkcVersion;
import com.csf.vcpp.ykc.router.YkcUpDataRouter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * 云快充 TCP 通道装配。
 * <p>
 * 拆帧参数由 {@link YkcVersion} 统一提供：
 * </p>
 * <pre>
 * 帧总长 = 1(起始符) + 数据长度域(1B/2B) + 数据域(dataLength) + 2(CRC)
 * LengthFieldBasedFrameDecoder 计算: lengthFieldEndOffset + dataLength + lengthAdjustment
 * => lengthAdjustment 恒为 2（CRC 两字节）
 * </pre>
 */
@Slf4j
public class YkcChannelHandler extends ChannelInitializer<SocketChannel> {

	/**
	 * 单帧最大长度：数据域最大 65535 + 起始符 1 + 长度域 2 + CRC 2
	 */
	private static final int MAX_FRAME_LENGTH = 65535 + 1 + 2 + 2;

	private final YkcUpDataRouter ykcUpDataRouter;

	private final YkcVersion version;

	private final int readerIdleSeconds;

	public YkcChannelHandler(YkcUpDataRouter ykcUpDataRouter, String versionLabel, int readerIdleSeconds) {
		this.ykcUpDataRouter = ykcUpDataRouter;
		this.version = YkcVersion.of(versionLabel);
		this.readerIdleSeconds = readerIdleSeconds;
	}

	public YkcChannelHandler(YkcUpDataRouter ykcUpDataRouter, String versionLabel) {
		this(ykcUpDataRouter, versionLabel, 30);
	}

	@Override
	protected void initChannel(SocketChannel ch) {
		ChannelPipeline pipeline = ch.pipeline();

		pipeline.addLast(new LengthFieldBasedFrameDecoder(
				MAX_FRAME_LENGTH,
				1,
				version.getLengthFieldLength(),
				2,
				0,
				true));

		pipeline.addLast(new IdleStateHandler(readerIdleSeconds, 0L, 0L, TimeUnit.SECONDS));

		pipeline.addLast(new YkcChannelInboundHandler(ykcUpDataRouter, version.getLabel()));
	}
}
