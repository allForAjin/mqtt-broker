package com.lmk.mqtt.service.impl;

import com.lmk.mqtt.exchange.ExchangeEnum;
import com.lmk.mqtt.service.api.ExchangeService;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExchangeServiceImpl implements ExchangeService {

    @Autowired
    private RabbitAdmin rabbitAdmin;
    @Override
    public void createDirectExchange(ExchangeEnum exchangeEnum) {
        rabbitAdmin.declareExchange(
                new DirectExchange(
                        exchangeEnum.getExchangeName(),
                        exchangeEnum.isDurable(),
                        false,
                        null)
        );
    }

    @Override
    public void createFanoutExchange(ExchangeEnum exchangeEnum) {
        rabbitAdmin.declareExchange(
                new FanoutExchange(
                        exchangeEnum.getExchangeName(),
                        exchangeEnum.isDurable(),
                        false,
                        null)
        );
    }

    @Override
    public void createTopicExchange(ExchangeEnum exchangeEnum) {
        rabbitAdmin.declareExchange(
                new TopicExchange(
                        exchangeEnum.getExchangeName(),
                        exchangeEnum.isDurable(),
                        false,
                        null)
        );

    }
}
