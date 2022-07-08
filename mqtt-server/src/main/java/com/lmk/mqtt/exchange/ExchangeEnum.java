package com.lmk.mqtt.exchange;


public enum ExchangeEnum {
    DEFAULT_EXCHANGE("default_exchange",ExchangeType.direct,true,"默认交换机，direct"),
    DEFAULT_DIRECT_EXCHANGE("default_direct_exchange",ExchangeType.direct,true,"默认交换机，direct"),
    DEFAULT_FANOUT_EXCHANGE("default_fanout_exchange",ExchangeType.fanout,true,"默认FANOUT交换机"),
    DEFAULT_TOPIC_EXCHANGE("default_topic_exchange",ExchangeType.topic,true,"默认TOPIC交换机"),
    DEFAULT_DEAD_EXCHANGE("default_dead_exchange",ExchangeType.direct,true,"默认死信交换机"),

    ;


    private String exchangeName;

    private ExchangeType exchangeType;


    private boolean durable;

    private String description;

    ExchangeEnum(String exchangeName, ExchangeType exchangeType, boolean durable, String description) {
        this.exchangeName = exchangeName;
        this.exchangeType = exchangeType;
        this.durable = durable;
        this.description = description;
    }

    public String getExchangeName() {
        return exchangeName;
    }

    public ExchangeType getExchangeType() {
        return exchangeType;
    }

    public boolean isDurable() {
        return durable;
    }

    public String getDescription() {
        return description;
    }

}
