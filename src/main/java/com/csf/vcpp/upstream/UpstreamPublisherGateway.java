package com.csf.vcpp.upstream;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 上行数据上报门面。
 * <p>
 * 容器中存在多个 {@link UpstreamPublisher} 实现时依次调用；
 * 当前仅有只打日志的默认实现（MQTT 上行能力预留）。
 * </p>
 */
@Component
public class UpstreamPublisherGateway implements UpstreamPublisher {

	private final List<UpstreamPublisher> publishers;

	public UpstreamPublisherGateway(ObjectProvider<UpstreamPublisher> provider) {
		this.publishers = provider.orderedStream()
				.filter(p -> !(p instanceof UpstreamPublisherGateway))
				.toList();
	}

	@Override
	public void publishData(String deviceNo, String event, Object payload) {
		publishers.forEach(p -> p.publishData(deviceNo, event, payload));
	}

	@Override
	public void publishOps(String deviceNo, String event, Object payload) {
		publishers.forEach(p -> p.publishOps(deviceNo, event, payload));
	}

	@Override
	public void publishTrade(String deviceNo, String event, Object payload) {
		publishers.forEach(p -> p.publishTrade(deviceNo, event, payload));
	}
}
