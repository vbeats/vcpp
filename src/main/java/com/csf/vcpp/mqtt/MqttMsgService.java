package com.csf.vcpp.mqtt;

import com.alibaba.fastjson2.JSON;
import com.csf.vcpp.cmd.CmdMsg;
import com.csf.vcpp.ykc.context.YkcServerContext;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcCmdExecutor;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class MqttMsgService {

	public void handleMsg(String topic, MqttMessage message) {
		String msg = new String(message.getPayload());
		log.info("vcpp rec 平台消息⬇️: {}", msg);

		CmdMsg cmdMsg = JSON.parseObject(msg, CmdMsg.class);

		switch (cmdMsg.getProtocol()) {
			case "ykc" -> handleYkcCmd(cmdMsg);
		}
	}

	private void handleYkcCmd(CmdMsg cmdMsg) {

		Long deviceId = cmdMsg.getDeviceId();
		Channel channel = YkcServerContext.DEVICE_CHANNEL_MAP.get(deviceId);
		if (ObjectUtils.isEmpty(channel)) {
			return;
		}

		FrameType frameType = switch (cmdMsg.getCmdType()) {
			case LOGIN -> FrameType.LOGIN_ACK;
			case BILLING_MODEL -> FrameType.BILLING_MODEL_ACK;
			case TIME_SYNC -> FrameType.TIME_SYNC;
			case RESTART -> FrameType.REMOTE_RESTART;
			case START_CHARGE -> FrameType.REMOTE_CHARGE;
			case PARALLEL_CHARGE -> FrameType.REMOTE_PARALLEL_CHARGE;
			case STOP_CHARGE -> FrameType.REMOTE_STOP;
		};

		String versionLabel = cmdMsg.getVersionLabel();

		YkcCmdExecutor cmdExecutor = YkcServerContext.EXECUTER_MAP.get(versionLabel + "_" + frameType.getType());
		if (ObjectUtils.isEmpty(cmdExecutor)) {
			log.warn("【云快充】{} ⬇️ data 帧类型未实现cmd executor {}", versionLabel, frameType.getType());
			return;
		}

		cmdExecutor.execute(channel, versionLabel, cmdExecutor.covertCmdData2YkcMsgData(cmdMsg));
	}
}
