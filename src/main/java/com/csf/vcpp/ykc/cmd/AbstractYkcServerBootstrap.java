package com.csf.vcpp.ykc.cmd;

import cn.hutool.core.thread.ThreadUtil;
import com.csf.vcpp.config.AppConfig;
import com.csf.vcpp.ykc.YkcChannelHandler;
import com.csf.vcpp.ykc.context.YkcServerContext;
import com.csf.vcpp.ykc.router.YkcUpDataRouter;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.concurrent.TimeUnit;

/**
 * 云快充 TCP 服务启动模板。
 * <p>
 * 指令注册统一由 {@link com.csf.vcpp.ykc.context.YkcCmdRegistry} 完成（不再按包扫描），
 * 本类只负责端口监听与线程组生命周期；停机时先关闭本版本的全部连接，再释放线程组。
 * </p>
 */
@Slf4j
public abstract class AbstractYkcServerBootstrap implements InitializingBean, DisposableBean {

	protected final AppConfig appConfig;

	protected final YkcUpDataRouter ykcUpDataRouter;

	private EventLoopGroup bossGroup;

	private EventLoopGroup workerGroup;

	private volatile Channel serverChannel;

	protected AbstractYkcServerBootstrap(AppConfig appConfig, YkcUpDataRouter ykcUpDataRouter) {
		this.appConfig = appConfig;
		this.ykcUpDataRouter = ykcUpDataRouter;
	}

	@Override
	public void afterPropertiesSet() {
		ThreadUtil.execAsync(this::startServer);
	}

	@Override
	public void destroy() {
		String versionLabel = getVersionLabel();

		// 1. 先关闭该版本上的全部设备连接，保证桩侧能立刻感知并重连
		YkcServerContext.closeByVersion(versionLabel);

		// 2. 关闭监听
		Channel channel = this.serverChannel;
		if (channel != null) {
			try {
				channel.close().sync();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				log.error("【Server】{} Close Failed : ", versionLabel, e);
			}
		}

		// 3. 释放线程组
		if (bossGroup != null) {
			bossGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);
		}
		if (workerGroup != null) {
			workerGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);
		}
		log.info("【云快充】 {}服务已停止", versionLabel);
	}

	protected abstract int getPort();

	protected abstract String getVersionLabel();

	/**
	 * TCP 服务启动
	 */
	private void startServer() {
		int port = getPort();
		String versionLabel = getVersionLabel();

		bossGroup = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
		workerGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

		ServerBootstrap bootstrap = new ServerBootstrap();
		try {
			bootstrap.group(bossGroup, workerGroup)
					.channel(NioServerSocketChannel.class)
					.option(ChannelOption.SO_BACKLOG, 10240)
					.childOption(ChannelOption.SO_KEEPALIVE, true)
					.childOption(ChannelOption.SO_REUSEADDR, true)
					.childOption(ChannelOption.TCP_NODELAY, true)
					.childOption(ChannelOption.SO_LINGER, 0)
					.childHandler(new YkcChannelHandler(ykcUpDataRouter, versionLabel, readerIdleSeconds()));

			ChannelFuture future = bootstrap.bind("0.0.0.0", port).sync();
			serverChannel = future.channel();
			log.info("【云快充】 {}服务启动, 监听端口: {}", versionLabel, port);
			serverChannel.closeFuture().sync();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.warn("【云快充】 {}服务监听被中断, 端口: {}", versionLabel, port);
		} catch (Exception e) {
			log.error("【Server】{} Start Failed, port: {} : ", versionLabel, port, e);
			throw new IllegalStateException("云快充服务启动失败: " + versionLabel + ", port=" + port, e);
		} finally {
			if (bossGroup != null) {
				bossGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);
			}
			if (workerGroup != null) {
				workerGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);
			}
		}
	}

	/**
	 * 读空闲判定为离线的秒数；桩心跳周期为 10s，留足余量
	 */
	protected int readerIdleSeconds() {
		Integer seconds = appConfig.getYkcReaderIdleSeconds();
		return seconds == null || seconds <= 0 ? 30 : seconds;
	}
}
