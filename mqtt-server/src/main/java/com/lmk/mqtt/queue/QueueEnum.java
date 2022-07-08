package com.lmk.mqtt.queue;

import java.util.Map;

public enum QueueEnum {
    DEFAULT_QUEUE("default_queue",true,true,false,null,"默认队列"),
    DEFAULT_DEAD_QUEUE("default_dead_queue",true,true,false,null,"默认死信队列")
    ;
    private final String queueName;
    private final boolean durable;
    private final boolean exclusive;
    private final boolean autoDelete;
    private final Map<String,Object> arguments;
    private final String desc;

    QueueEnum(String queueName, boolean durable, boolean exclusive, boolean autoDelete, Map<String, Object> arguments, String desc) {
        this.queueName = queueName;
        this.durable = durable;
        this.exclusive = exclusive;
        this.autoDelete = autoDelete;
        this.arguments = arguments;
        this.desc = desc;
    }

    public String getQueueName() {
        return queueName;
    }

    public boolean isDurable() {
        return durable;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public boolean isAutoDelete() {
        return autoDelete;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public String getDesc() {
        return desc;
    }
}
