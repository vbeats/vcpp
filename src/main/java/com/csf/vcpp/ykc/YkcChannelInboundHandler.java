package com.csf.vcpp.ykc;

import com.csf.vcpp.ykc.context.YkcServerContext;
import com.csf.vcpp.ykc.router.YkcUpDataRouter;
import com.csf.vcpp.ykc.utils.YkcAckUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;

/**
 * 云快充上行通道处理器。
 * <p>
 * ByteBuf 生命周期：拆帧器产出的 ByteBuf 在 {@code channelRead0} 返回后由 Netty 自动释放，
 * 由于业务在虚拟线程中异步执行，这里先 {@code retain()} 再加引用计数，
 * 交由 {@link YkcUpDataRouter} 在 finally 中释放，避免异步读取已释放缓冲区。
 * </p>
 * <p>
 * 背压：虚拟线程本身无界，这里用信号量限制「在途帧数量」，
 * 防止设备侧突发灌包导致堆内堆积大量 ByteBuf 引起 OOM。
 * </p>
 */
@Slf4j
public class YkcChannelInboundHandler extends SimpleChannelInboundHandler<ByteBuf> {

	private static final ExecutorService CPP_EXECUTOR =
			Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("vcpp-ykc-", 0).factory());

	/**
	 * 全局在途帧上限，超出则丢弃并告警（保护堆内存）
	 */
	private static final int MAX_IN_FLIGHT = 200_000;

	private static final Semaphore IN_FLIGHT = new Semaphore(MAX_IN_FLIGHT);

	private final YkcUpDataRouter ykcUpDataRouter;

	private final String versionLabel;

	public YkcChannelInboundHandler(YkcUpDataRouter ykcUpDataRouter, String versionLabel) {
		this.ykcUpDataRouter = ykcUpDataRouter;
		this.versionLabel = versionLabel;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		YkcServerContext.attach(ctx.channel(), versionLabel);
		log.info("【云快充】{} 充电桩设备侧client连接: {} | 当前连接数: {} | 在线桩数: {}",
				versionLabel, ctx.channel().remoteAddress(), YkcServerContext.connectionCount(), YkcServerContext.onlineCount());
		ctx.fireChannelActive();
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
		if (evt instanceof IdleStateEvent event && IdleState.READER_IDLE.equals(event.state())) {
			String deviceNo = YkcServerContext.unbind(ctx.channel());
			if (StringUtils.hasText(deviceNo)) {
				log.warn("【云快充】{} 充电桩离线(读空闲): {} | 在线桩数: {}", versionLabel, deviceNo, YkcServerContext.onlineCount());
			}
			ctx.close();
			return;
		}
		ctx.fireUserEventTriggered(evt);
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
		// 业务在虚拟线程执行，此处 +1 引用计数，由 YkcUpDataRouter 负责释放
		ByteBuf frame = msg.retain();
		Channel channel = ctx.channel();

		if (!IN_FLIGHT.tryAcquire()) {
			YkcAckUtil.release(frame);
			log.error("【云快充】{} 在途报文数达到上限 {}, 丢弃当前帧, 对端: {}", versionLabel, MAX_IN_FLIGHT, channel.remoteAddress());
			return;
		}

		try {
			CPP_EXECUTOR.execute(() -> {
				try {
					ykcUpDataRouter.route(channel, versionLabel, frame);
				} finally {
					IN_FLIGHT.release();
				}
			});
		} catch (RejectedExecutionException e) {
			IN_FLIGHT.release();
			YkcAckUtil.release(frame);
			log.error("【云快充】{} 报文派发被拒绝, 对端: {}", versionLabel, channel.remoteAddress(), e);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		log.error("【云快充】{} 连接异常, 对端: {}", versionLabel, ctx.channel().remoteAddress(), cause);
		String deviceNo = YkcServerContext.unbind(ctx.channel());
		if (StringUtils.hasText(deviceNo)) {
			log.warn("【云快充】{} 充电桩异常断开: {} | 在线桩数: {}", versionLabel, deviceNo, YkcServerContext.onlineCount());
		}
		ctx.close();
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) {
		String deviceNo = YkcServerContext.unbind(ctx.channel());
		if (StringUtils.hasText(deviceNo)) {
			log.warn("【云快充】{} 充电桩离线: {} | 在线桩数: {}", versionLabel, deviceNo, YkcServerContext.onlineCount());
		} else {
			log.info("【云快充】{} 未认证连接断开: {} | 当前连接数: {}", versionLabel, ctx.channel().remoteAddress(), YkcServerContext.connectionCount());
		}
		ctx.fireChannelInactive();
	}
}
