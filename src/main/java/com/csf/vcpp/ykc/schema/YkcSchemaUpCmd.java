package com.csf.vcpp.ykc.schema;

import com.csf.vcpp.upstream.UpstreamPublisher;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcCmdExecutor;
import com.csf.vcpp.ykc.model.YkcMsgData;
import com.csf.vcpp.ykc.utils.YkcSchemaValue;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 声明式上行帧执行器。
 * <p>
 * 适用于「解析 + 记录 + 上报」即可满足的上行帧（BMS 过程帧、各类设置应答等），
 * 由 {@link FrameSchema} 描述消息体结构，避免为每个帧类型手写一遍解析代码。
 * 需要业务决策的帧（登录、计费模型、交易记录等）仍使用独立执行器。
 * </p>
 */
@Slf4j
public class YkcSchemaUpCmd extends YkcCmdExecutor {

	/**
	 * 上报通道分类
	 */
	public enum Topic {
		DATA, OPS, TRADE
	}

	private final FrameType frameType;

	private final FrameSchema schema;

	private final UpstreamPublisher publisher;

	private final Topic topic;

	public YkcSchemaUpCmd(FrameType frameType, FrameSchema schema, UpstreamPublisher publisher) {
		this(frameType, schema, publisher, Topic.DATA);
	}

	public YkcSchemaUpCmd(FrameType frameType, FrameSchema schema, UpstreamPublisher publisher, Topic topic) {
		this.frameType = frameType;
		this.schema = schema;
		this.publisher = publisher;
		this.topic = topic;
	}

	@Override
	public void execute(Channel channel, String versionLabel, YkcMsgData data) {
		List<YkcSchemaValue> values = schema.parse(data.getMsgBody());
		Map<String, Object> payload = new LinkedHashMap<>();
		for (YkcSchemaValue value : values) {
			payload.put(value.getName(), value.getValue());
		}

		String deviceNo = deviceNo(values);
		if (log.isInfoEnabled()) {
			log.info("【云快充】{} ⬆️ {}{}", versionLabel, frameType.getDesc(), render(values));
		}

		if (publisher != null && StringUtils.hasText(deviceNo)) {
			payload.put("deviceNo", deviceNo);
			payload.put("versionLabel", versionLabel);
			switch (topic) {
				case TRADE -> publisher.publishTrade(deviceNo, frameType.name(), payload);
				case OPS -> publisher.publishOps(deviceNo, frameType.name(), payload);
				default -> publisher.publishData(deviceNo, frameType.name(), payload);
			}
		}
	}

	private String deviceNo(List<YkcSchemaValue> values) {
		for (YkcSchemaValue value : values) {
			if ("桩编号".equals(value.getName()) || "桩编号(主枪)".equals(value.getName())) {
				return value.getText();
			}
		}
		return null;
	}

	private String render(List<YkcSchemaValue> values) {
		if (values == null || values.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder(64 + values.size() * 24);
		sb.append('\n');
		for (YkcSchemaValue value : values) {
			sb.append(value.getName()).append(": ").append(value.getText()).append('\n');
		}
		sb.append("-----");
		return sb.toString();
	}

	public FrameType getFrameType() {
		return frameType;
	}

	public List<YkcSchemaValue> emptyValues() {
		return Collections.emptyList();
	}
}
