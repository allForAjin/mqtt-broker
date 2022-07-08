package com.lmk.mqtt.config;

import com.lmk.mqtt.exchange.ExchangeEnum;
import com.lmk.mqtt.queue.DelayQueueEnum;
import com.lmk.mqtt.queue.QueueEnum;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class QueueConfig {
    @Bean(name = "normalQueue")
    public Queue createNormalQueue() {
        QueueEnum queueEnum = QueueEnum.DEFAULT_QUEUE;
        return new Queue(queueEnum.getQueueName(), queueEnum.isDurable(), true, false, queueEnum.getArguments());
    }

//    @Bean(name = "deadQueue")
//    public Queue createDeadQueue() {
//        QueueEnum queueEnum = QueueEnum.DEFAULT_DEAD_QUEUE;
//        return new Queue(queueEnum.getQueueName(), queueEnum.isDurable(), false, false, queueEnum.getArguments());
//    }

//    /**
//     * 创建该队列同时需要创建死信队列
//     */
//    @Bean(name = "delayQueue")
//    public Queue createDelayQueue() {
//        DelayQueueEnum delayQueueEnum = DelayQueueEnum.DEFAULT_DELAY_QUEUE;
//        Map<String, Object> arguments = DelayQueueEnum.deadQueueArgumentSetting(delayQueueEnum.getDeadExchangeEnum().getExchangeName(), "dead", delayQueueEnum.getTtl());
//        return new Queue(delayQueueEnum.getName(), delayQueueEnum.isDurable(), false, false, arguments);
//    }
}
