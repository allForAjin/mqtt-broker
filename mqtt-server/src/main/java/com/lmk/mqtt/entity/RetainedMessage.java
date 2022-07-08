package com.lmk.mqtt.entity;

/**
 * @author lmk
 * @version 1.0.0
 * @ClassName RetainedMessage.java
 * @Description TODO
 * @createTime 2022-06-21 15:09:29
 */
public class RetainedMessage {
    private final String message;
    private final String topic;
    private final Integer qos;
    private final Integer packetId;

    public RetainedMessage(String message, String topic, Integer qos, Integer packetId) {
        this.message = message;
        this.topic = topic;
        this.qos = qos;
        this.packetId = packetId;
    }

    public String getMessage() {
        return message;
    }

    public String getTopic() {
        return topic;
    }

    public Integer getQos() {
        return qos;
    }

    public Integer getPacketId() {
        return packetId;
    }

    @Override
    public String toString() {
        return "RetainedMessage{" +
                "message='" + message + '\'' +
                ", topic='" + topic + '\'' +
                ", qos=" + qos +
                '}';
    }
}
