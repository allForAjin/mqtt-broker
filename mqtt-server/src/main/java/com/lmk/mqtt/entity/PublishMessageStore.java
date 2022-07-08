package com.lmk.mqtt.entity;

import java.util.Arrays;

/**
 * @author lmk
 * @version 1.0.0
 * @ClassName PublishMessageStore.java
 * @Description TODO
 * @createTime 2022-06-23 18:11:16
 */
public class PublishMessageStore {
    private String clientId;

    private String topic;

    private int mqttQoS;

    private int messageId;

    private String message;

    public PublishMessageStore() {
    }

    public PublishMessageStore(String clientId, String topic, int mqttQoS, int messageId, String message) {
        this.clientId = clientId;
        this.topic = topic;
        this.mqttQoS = mqttQoS;
        this.messageId = messageId;
        this.message = message;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getMqttQoS() {
        return mqttQoS;
    }

    public void setMqttQoS(int mqttQoS) {
        this.mqttQoS = mqttQoS;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "PublishMessageStore{" +
                "clientId='" + clientId + '\'' +
                ", topic='" + topic + '\'' +
                ", mqttQoS=" + mqttQoS +
                ", messageId=" + messageId +
                ", message=" + message +
                '}';
    }
}
