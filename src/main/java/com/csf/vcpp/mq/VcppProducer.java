package com.csf.vcpp.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VcppProducer {

	private final RabbitTemplate rabbitTemplate;

	public void sendMsg(String exchange, String routingKey, String msg) {
		try {
			rabbitTemplate.convertAndSend(exchange, routingKey, msg);
		} catch (Exception e) {
			log.error("==========> mq消息推送异常: ", e);
		}
	}

}
