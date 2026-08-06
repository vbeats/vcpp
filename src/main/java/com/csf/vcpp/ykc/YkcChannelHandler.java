package com.csf.vcpp.ykc;

import cn.hutool.core.util.HexUtil;
import com.csf.vcpp.ykc.consts.YkcProtocolConst;
import com.csf.vcpp.ykc.router.YkcUpDataRouter;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class YkcChannelHandler extends ChannelInitializer<SocketChannel> {

	private final YkcUpDataRouter ykcUpDataRouter;
	private final String versionLabel;

	public YkcChannelHandler(YkcUpDataRouter ykcUpDataRouter, String versionLabel) {
		this.ykcUpDataRouter = ykcUpDataRouter;
		this.versionLabel = versionLabel;
	}

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();

		if (Arrays.asList(YkcProtocolConst.V21).contains(versionLabel)) {
			pipeline.addLast(new LengthFieldBasedFrameDecoder(65540, 1, 2, 3, 0, true));
		} else {
			pipeline.addLast(new LengthFieldBasedFrameDecoder(65540, 1, 1, 2, 0, true));
		}

		pipeline.addLast(new IdleStateHandler(30L, 0L, 0L, TimeUnit.SECONDS));

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

		pipeline.addLast(new YkcChannelInboundHandler(ykcUpDataRouter, versionLabel));
	}
}
