package com.lmk.mqtt.queue;

import com.lmk.mqtt.exchange.ExchangeEnum;

import java.util.HashMap;
import java.util.Map;

public enum DelayQueueEnum {
    DEFAULT_DELAY_QUEUE("default_delay_queue", true, 10000L, ExchangeEnum.DEFAULT_DEAD_EXCHANGE, QueueEnum.DEFAULT_DEAD_QUEUE, "默认延迟队列");
    //队列名称
    private final String name;
    //持久换
    private final boolean durable;
    //延迟时间ms
    private final Long ttl;
    //监听交换机
    private final ExchangeEnum deadExchangeEnum;
    //监听队列
    private final QueueEnum deadQueueEnum;
    //队列描述
    private final String desc;

    DelayQueueEnum(String name, boolean durable, Long ttl, ExchangeEnum deadExchangeEnum, QueueEnum deadQueueEnum, String desc) {
        this.name = name;
        this.durable = durable;
        this.ttl = ttl;
        this.deadExchangeEnum = deadExchangeEnum;
        this.deadQueueEnum = deadQueueEnum;
        this.desc = desc;
    }

    /**
     * 延迟队列 绑定死信
     * 消息配置
     *
     * @param deadExchange 延迟交换机
     * @param deadRoutingKey    延迟队列
     * @param ttl          延迟时间
     */
    public static Map<String, Object> deadQueueArgumentSetting(String deadExchange, String deadRoutingKey, Long ttl) {
        // reply_to 队列
        Map<String, Object> map = new HashMap<>();
        //设置消息的过期时间 单位毫秒
        map.put("x-message-ttl", ttl);
        //设置附带的死信交换机
        map.put("x-dead-letter-exchange", deadExchange);
        //指定重定向的路由建 消息作废之后可以决定需不需要更改他的路由建 如果需要 就在这里指定
        map.put("x-dead-letter-routing-key", deadRoutingKey);
        return map;
    }

    public String getName() {
        return name;
    }

    public boolean isDurable() {
        return durable;
    }

    public Long getTtl() {
        return ttl;
    }

    public ExchangeEnum getDeadExchangeEnum() {
        return deadExchangeEnum;
    }

    public QueueEnum getDeadQueueEnum() {
        return deadQueueEnum;
    }

    public String getDesc() {
        return desc;
    }
}
