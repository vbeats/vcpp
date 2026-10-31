package com.csf.vcpp.upstream;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 上行数据上报默认实现：仅打印调试日志，不做真实投递。
 * <p>
 * 与上层平台（MQTT / Kafka）的对接暂不实现（能力预留）；接入时只需提供另一个
 * {@link UpstreamPublisher} Bean 即可替换本默认实现。
 * </p>
 */
@Slf4j
@Service("noopUpstreamPublisher")
public class NoopUpstreamPublisher implements UpstreamPublisher {

	@Override
	public void publishData(String deviceNo, String event, Object payload) {
		log.debug("[upstream] data/{} {} => {}", deviceNo, event, payload);
	}

	@Override
	public void publishOps(String deviceNo, String event, Object payload) {
		log.debug("[upstream] ops/{} {} => {}", deviceNo, event, payload);
	}

	@Override
	public void publishTrade(String deviceNo, String event, Object payload) {
		log.debug("[upstream] trade/{} {} => {}", deviceNo, event, payload);
	}
}
