package com.lmk.mqtt.config;

import com.lmk.mqtt.exchange.ExchangeEnum;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExchangeConfig {
    @Bean
    public DirectExchange defaultExchange(){
        ExchangeEnum exchangeEnum = ExchangeEnum.DEFAULT_EXCHANGE;
        return new DirectExchange(exchangeEnum.getExchangeName(),exchangeEnum.isDurable(),false,null);
    }

    @Bean
    public DirectExchange directExchange(){
        ExchangeEnum exchangeEnum = ExchangeEnum.DEFAULT_DIRECT_EXCHANGE;
        return new DirectExchange(exchangeEnum.getExchangeName(),exchangeEnum.isDurable(),false,null);
    }

    @Bean
    public TopicExchange topicExchange(){
        ExchangeEnum exchangeEnum= ExchangeEnum.DEFAULT_TOPIC_EXCHANGE;
        return new TopicExchange(exchangeEnum.getExchangeName(),exchangeEnum.isDurable(),false,null);
    }

    @Bean
    public TopicExchange testTopicExchange(){
        ExchangeEnum exchangeEnum = ExchangeEnum.MQ_TOPIC_EXCHANGE;
        return new TopicExchange(exchangeEnum.getExchangeName(),exchangeEnum.isDurable(),false,null);
    }

    @Bean
    public DirectExchange deadExchange(){
        ExchangeEnum exchangeEnum = ExchangeEnum.DEFAULT_DEAD_EXCHANGE;
        return new DirectExchange(exchangeEnum.getExchangeName(),exchangeEnum.isDurable(),false,null);
    }
}
