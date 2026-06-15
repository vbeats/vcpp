package com.csf.vcpp.mqtt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MqttCmdListener implements MqttCallback {// 订阅平台指令

	@Override
	public void disconnected(MqttDisconnectResponse disconnectResponse) {

	}

	@Override
	public void mqttErrorOccurred(MqttException exception) {

	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		log.info("vcpp rec 平台指令: {}", new String(message.getPayload()));
	}

	@Override
	public void deliveryComplete(IMqttToken token) {

	}

	@Override
	public void connectComplete(boolean reconnect, String serverURI) {

	}

	@Override
	public void authPacketArrived(int reasonCode, MqttProperties properties) {

	}
}
