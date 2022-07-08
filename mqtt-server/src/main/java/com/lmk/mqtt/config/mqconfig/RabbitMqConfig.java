package com.lmk.mqtt.config.mqconfig;

import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class RabbitMqConfig {
    @Autowired
    @Lazy
    private RabbitTemplate rabbitTemplate;
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitAdmin rabbitAdmin(){
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return new RabbitAdmin(rabbitTemplate);
    }
}
