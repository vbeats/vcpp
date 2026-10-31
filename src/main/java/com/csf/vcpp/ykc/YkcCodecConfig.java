package com.csf.vcpp.ykc;

import com.csf.vcpp.config.AppConfig;
import com.csf.vcpp.ykc.context.YkcServerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * 连接上下文参数装配：把 {@code vcpp.*} 配置下发到 {@link YkcServerContext}。
 * <p>
 * 各版本 TCP 服务 Bootstrap 通过 {@code @DependsOn("ykcCodecConfig")} 保证配置先于端口监听生效。
 * </p>
 * <p>
 * 注：帧编解码的字节序（上行小端 / 下行大端）已由真实设备报文验证并固化在 {@code YkcAckUtil}，
 * 不提供配置开关，详见 AGENTS.md。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class YkcCodecConfig implements InitializingBean {

	private final AppConfig appConfig;

	@Override
	public void afterPropertiesSet() {
		YkcServerContext.applyExpectedDevices(appConfig.getYkcExpectedDevices());
	}
}
