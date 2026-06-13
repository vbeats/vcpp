package com.csf.vcpp.ykc;

import cn.hutool.core.util.HexUtil;
import com.csf.vcpp.ykc.router.YkcDataRouter;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class YkcChannelHandler extends ChannelInitializer<SocketChannel> {

	private final YkcDataRouter ykcDataRouter;
	private final String versionLabel;

	public YkcChannelHandler(YkcDataRouter ykcDataRouter, String versionLabel) {
		this.ykcDataRouter = ykcDataRouter;
		this.versionLabel = versionLabel;
	}

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();

		pipeline.addLast(new LengthFieldBasedFrameDecoder(65538, 1, 1, 2, 0, true));

		// todo 打开idle debug
		// pipeline.addLast(new IdleStateHandler(30L, 0L, 0L, TimeUnit.SECONDS));

		pipeline.addLast(new ByteToMessageDecoder() {
			@Override
			protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
				int length = in.readableBytes();
				if (length > 0) {
					byte[] bytes = new byte[length];
					in.readBytes(bytes);
					out.add(HexUtil.encodeHexStr(bytes, true));
				}
			}
		});

		pipeline.addLast(new YkcChannelInboundHandler(ykcDataRouter, versionLabel));
	}
}
