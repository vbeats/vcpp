package com.csf.vcpp.mqtt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class VcppMqttConfig {

	@Value("${vcpp.mqtt.host}")
	private String host;

	@Value("${vcpp.mqtt.client-id}")
	private String clientId;

	private final MqttCmdListener mqttCmdListener;

	@Bean
	public MqttClient mqttClient() {
		try {
			MqttClient client = new MqttClient(host, clientId, new MemoryPersistence());
			MqttConnectionOptions options = new MqttConnectionOptions();

			options.setCleanStart(false); // 恢复会话
			options.setAutomaticReconnect(true);

			options.setSessionExpiryInterval(30 * 60L); // server 会话消息存储过期时间 30分钟
			options.setUserName("admin");
			options.setPassword("admin123456".getBytes(StandardCharsets.UTF_8));

			client.connect(options);

			if (client.isConnected()) {
				log.info("mqtt client connected to {}", host);

				client.setCallback(mqttCmdListener);

				client.subscribe("vcpp/cmd/+", 0);
			}

			return client;
		} catch (Exception e) {
			log.error("mqtt client connect error: ", e);
			System.exit(-1);
			return null;
		}
	}
}
