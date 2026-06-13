package com.csf.vcpp.ykc.cmd;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.config.AppConfig;
import com.csf.vcpp.ykc.YkcChannelHandler;
import com.csf.vcpp.ykc.context.YkcServerContext;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcCmdExecutor;
import com.csf.vcpp.ykc.router.YkcDataRouter;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.Set;

@Slf4j
public abstract class AbstractYkcServerBootstrap implements InitializingBean, DisposableBean {

	protected final AppConfig appConfig;
	protected final YkcDataRouter ykcDataRouter;

	private Channel channel;

	protected AbstractYkcServerBootstrap(AppConfig appConfig, YkcDataRouter ykcDataRouter) {
		this.appConfig = appConfig;
		this.ykcDataRouter = ykcDataRouter;
	}

	@Override
	public void destroy() throws Exception {
		if (channel == null) {
			return;
		}

		try {
			channel.close().sync();
		} catch (Exception e) {
			log.error("【Server】 Close Failed : ", e);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.initCmdExecutor();
		ThreadUtil.execAsync(() -> startServer(getPort()));
	}

	// 初始化 cmd executor 实例
	private void initCmdExecutor() {
		Set<Class<?>> cmdClasses = ClassUtil.scanPackageByAnnotation(ClassUtil.getPackage(this.getClass()), ProtocolCmd.class);

		cmdClasses.forEach(cmdClass -> {
			ProtocolCmd annotation = cmdClass.getAnnotation(ProtocolCmd.class);

			FrameType frameType = annotation.value();
			String[] protocols = annotation.protocols();

			try {

				YkcCmdExecutor executor = (YkcCmdExecutor) SpringUtil.getBean(cmdClass);

				for (String protocol : protocols) {
					String key = protocol + "_" + frameType.getType();
					YkcServerContext.EXECUTER_MAP.put(key, executor);
					log.info("【云快充】 cmd executor 初始化成功: {} -> {}", key, executor.getClass().getName());
				}

			} catch (Exception e) {
				log.error("【云快充】 初始化cmd executor 异常: ", e);
				throw new RuntimeException(e);
			}
		});
	}

	protected abstract int getPort();

	protected abstract String getVersionLabel();

	// tcp server
	private void startServer(int port) {
		EventLoopGroup boss = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
		EventLoopGroup worker = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

		ServerBootstrap bootstrap = new ServerBootstrap();

		try {
			bootstrap.group(boss, worker)
				.channel(NioServerSocketChannel.class)
				.childOption(ChannelOption.SO_KEEPALIVE, true)
				.childOption(ChannelOption.SO_REUSEADDR, true)
				.childOption(ChannelOption.SO_LINGER, 0)
				.childHandler(new YkcChannelHandler(ykcDataRouter, getVersionLabel()))
			;

			ChannelFuture future = bootstrap.bind("0.0.0.0", port).sync();
			channel = future.channel();

			log.info("【云快充】 {}服务启动, 监听端口： {}", getVersionLabel(), port);
			channel.closeFuture().sync();
		} catch (Exception e) {
			log.error("【Server】 Start Failed : ", e);
			throw new RuntimeException(e);
		} finally {
			boss.shutdownGracefully();
			worker.shutdownGracefully();
		}
	}
}
