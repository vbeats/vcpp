package com.csf.vcpp.ykc;

import com.csf.vcpp.ykc.context.YkcServerContext;
import com.csf.vcpp.ykc.router.YkcDataRouter;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class YkcChannelInboundHandler extends SimpleChannelInboundHandler<String> {

	private static final ExecutorService CPP_EXECUTOR = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("vcpp-ykc").factory());

	private final YkcDataRouter ykcDataRouter;
	private final String versionLabel;

	public YkcChannelInboundHandler(YkcDataRouter ykcDataRouter, String versionLabel) {
		this.ykcDataRouter = ykcDataRouter;
		this.versionLabel = versionLabel;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		log.info("【云快充】 充电桩设备侧client连接.....: {} | 当前充电桩连接数: {}", ctx.channel().remoteAddress(), YkcServerContext.DEVICE_CHANNEL_MAP.size());
		super.channelActive(ctx);
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent event) {
			if (IdleState.READER_IDLE.equals(event.state())) {
				Long deviceNo = removeChannel(ctx);
				if (deviceNo != null) {
					log.warn("【云快充】 充电桩离线: {} | 当前充电桩连接数: {}", deviceNo, YkcServerContext.DEVICE_CHANNEL_MAP.size());
				}
				ctx.close();
			}
		} else {
			super.userEventTriggered(ctx, evt);
		}
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
		// 提前捕获 channel 引用，避免 lambda 捕获整个 ChannelHandlerContext
		Channel channel = ctx.channel();
		CPP_EXECUTOR.execute(() -> ykcDataRouter.route(channel, versionLabel, msg.toUpperCase()));
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		log.error("【云快充】 数据处理异常:", cause);
		if (cause instanceof SocketException) {
			Long deviceNo = removeChannel(ctx);
			if (deviceNo != null) {
				log.info("【云快充】 充电桩设备侧client断开连接: {}", deviceNo);
			}
			ctx.close();
			return;
		}
		super.exceptionCaught(ctx, cause);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		log.info("【云快充】 充电桩设备侧client断开连接......: {}", ctx.channel().remoteAddress());
		Long deviceNo = removeChannel(ctx);
		if (deviceNo != null) {
			log.warn("【云快充】 充电桩设备侧client: {} 断开连接... | 当前充电桩连接数: {}", deviceNo, YkcServerContext.DEVICE_CHANNEL_MAP.size());
		}
		super.channelInactive(ctx);
	}

	/**
	 * 清理通道映射关系，返回对应的桩号（若存在）
	 */
	private Long removeChannel(ChannelHandlerContext ctx) {
		String channelId = ctx.channel().id().asLongText();
		Long deviceNo = YkcServerContext.CHANNEL_DEVICE_MAP.remove(channelId);
		if (deviceNo != null) {
			YkcServerContext.DEVICE_CHANNEL_MAP.remove(deviceNo, ctx.channel());
		}
		return deviceNo;
	}
}
