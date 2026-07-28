package com.csf.vcpp.mq;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CmdDownListener { // 平台cmd 指令监听

	@RabbitListener(queues = {"#{'vcpp.cmd.queue.'+${vcpp.node-id}}"})
	public void handle(String msg, Message message, Channel channel) throws IOException {
		log.info("【vcpp】 cmd ⬇️ 指令监听 - 收到指令: {}", msg);

		long deliveryTag = message.getMessageProperties().getDeliveryTag();

		try {


		} catch (Exception e) {
			log.error("【vcpp】 cmd ⬇️ 指令监听 - 处理指令异常: ", e);
		} finally {
			channel.basicAck(deliveryTag, false);
		}
	}

}
