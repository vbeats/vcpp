package com.csf.vcpp.mqtt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ObjectUtils;

import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class VcppMqttConfig implements DisposableBean {

	@Value("${vcpp.mqtt.host}")
	private String host;

	@Value("${vcpp.mqtt.client-id}")
	private String clientId;

	@Value("${vcpp.mqtt.username}")
	private String username;

	@Value("${vcpp.mqtt.password}")
	private String password;

	private final MqttCmdListener mqttCmdListener;

	private MqttClient mqttClient;

	@Bean
	public MqttClient mqttClient() {
		try {
			MqttClient client = new MqttClient(host, clientId, new MemoryPersistence());
			MqttConnectionOptions options = new MqttConnectionOptions();

			options.setCleanStart(false); // 恢复会话

			options.setKeepAliveInterval(60);
			options.setAutomaticReconnect(true);
			options.setAutomaticReconnectDelay(1, 30);

			options.setSessionExpiryInterval(30 * 60L); // server 会话消息存储过期时间 30分钟
			options.setUserName(username);
			options.setPassword(password.getBytes(StandardCharsets.UTF_8));

			client.connect(options);

			if (client.isConnected()) {
				log.info("mqtt client: {} connected to {}", clientId, host);

				client.setCallback(mqttCmdListener);

				client.subscribe("vcpp/cmd", 0);
			}

			this.mqttClient = client;

			return client;
		} catch (Exception e) {
			log.error("mqtt client connect error: ", e);
			System.exit(-1);
			return null;
		}
	}

	@Override
	public void destroy() throws Exception {
		if (ObjectUtils.isEmpty(mqttClient)) {
			return;
		}

		if (mqttClient.isConnected()) {
			mqttClient.disconnect(1000L);
			mqttClient.close();
		}
	}
}
