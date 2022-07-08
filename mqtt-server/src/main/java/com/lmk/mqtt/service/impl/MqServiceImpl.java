package com.lmk.mqtt.service.impl;

import com.lmk.mqtt.exchange.ExchangeEnum;
import com.lmk.mqtt.queue.DelayQueueEnum;
import com.lmk.mqtt.queue.QueueEnum;
import com.lmk.mqtt.service.api.MqService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class MqServiceImpl implements MqService {

    private final Logger logger = LoggerFactory.getLogger(MqServiceImpl.class);
    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private RabbitAdmin rabbitAdmin;

    @Override
    public void send(Object msg) {
        this.send(msg, "");
    }

    @Override
    public void send(Object msg, String routingKey) {
        QueueEnum queueEnum = QueueEnum.DEFAULT_QUEUE;
        this.send(msg, queueEnum, routingKey);
    }


    @Override
    public void send(Object msg, QueueEnum queueEnum, String routingKey) {
        this.send(msg, ExchangeEnum.DEFAULT_EXCHANGE, queueEnum, routingKey);
    }

    @Override
    public void send(Object msg, ExchangeEnum exchangeEnum, String routingKey) {
        rabbitTemplate.convertAndSend(exchangeEnum.getExchangeName(), routingKey, msg);
        logger.info("发送消息成功，目标交换机{}，消息内容{}",exchangeEnum,msg);
    }

    @Override
    public void send(Object msg, ExchangeEnum exchangeEnum, QueueEnum queueEnum, String routingKey) {
        this.bindExchangeAndQueue(exchangeEnum, queueEnum, routingKey);
        rabbitTemplate.convertAndSend(exchangeEnum.getExchangeName(), routingKey, msg);
    }

    @Override
    public void sendDelayMsg(Object msg, ExchangeEnum exchangeEnum, DelayQueueEnum delayQueueEnum, String routingKey,String deadRoutingKey) {
        //绑定延时队列
        this.bindExchangeAndDelayQueue(exchangeEnum,delayQueueEnum,routingKey);
        //绑定死信
        this.bindExchangeAndQueue(delayQueueEnum.getDeadExchangeEnum(),delayQueueEnum.getDeadQueueEnum(),deadRoutingKey);
        rabbitTemplate.convertAndSend(exchangeEnum.getExchangeName(), routingKey, msg);
    }

    private void bindExchangeAndQueue(ExchangeEnum exchangeEnum, QueueEnum queueEnum, String routingKey) {
        rabbitAdmin.declareBinding(
                new Binding(queueEnum.getQueueName(),
                        Binding.DestinationType.QUEUE,
                        exchangeEnum.getExchangeName(),
                        routingKey,
                        null)
        );
    }

    private void bindExchangeAndDelayQueue(ExchangeEnum exchangeEnum, DelayQueueEnum delayQueueEnum, String routingKey) {
        rabbitAdmin.declareBinding(
                new Binding(delayQueueEnum.getName(),
                        Binding.DestinationType.QUEUE,
                        exchangeEnum.getExchangeName(),
                        routingKey,
                        null));
    }
}
