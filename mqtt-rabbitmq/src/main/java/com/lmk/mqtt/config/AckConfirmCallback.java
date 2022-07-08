package com.lmk.mqtt.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class AckConfirmCallback implements RabbitTemplate.ConfirmCallback {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    private final Logger logger = LoggerFactory.getLogger(AckConfirmCallback.class);

    @PostConstruct
    public void init(){
        rabbitTemplate.setConfirmCallback(this);
    }


    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        //未设置发送应答
        if (correlationData == null) {
            logger.info("未设置发送应答");
            return;
        }
        logger.info("是否确认发送成功ack = {}  失败原因cause={}", ack, cause);

        if (ack) {
            //确认应答
            logger.info("消息确认发送成功：correlationDataId = {}", correlationData.getId());
        } else {
            //未收到确认应答处理
            logger.error("消息确认发送失败：correlationDataId = {}", correlationData.getId());
        }
    }
}
