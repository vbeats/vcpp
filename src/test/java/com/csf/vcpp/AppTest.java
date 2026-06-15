package com.csf.vcpp;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;

@Slf4j
@SpringBootTest
public class AppTest {

	@Autowired
	private MqttClient mqttClient;

	@Test
	public void test() throws MqttException {
		while (true) {
			mqttClient.publish("vcpp/cmd/111", "t1".getBytes(StandardCharsets.UTF_8), 0, false);
		}
	}

}
