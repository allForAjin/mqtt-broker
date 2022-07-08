package com.lmk.mqtt.consumer;

import com.lmk.mqtt.constrant.MqttConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class TopicConsumer {
    private final Logger logger = LoggerFactory.getLogger(TopicConsumer.class);

//    @RabbitListener(bindings = @QueueBinding(
//            value = @Queue(name = MqttConstant.MQ_QUEUE_NAME,durable = "true"),
//            exchange = @Exchange(name = MqttConstant.EXCHANGE_NAME,type = ExchangeTypes.TOPIC),
//            key = {MqttConstant.ROUTING_KEY}
//    ))
    public void handlerMessage(byte[] msg){
        logger.info("消费者消费消息：{}",new String(msg));
    }
}
