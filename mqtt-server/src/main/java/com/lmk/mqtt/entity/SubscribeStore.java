package com.lmk.mqtt.entity;

/**
 * @author lmk
 * @version 1.0.0
 * @ClassName SubscribeStore.java
 * @Description TODO
 * @createTime 2022-06-23 18:20:09
 */
public class SubscribeStore {
    private String clientId;

    private String topicName;

    private int mqttQoS;

    public SubscribeStore() {
    }

    public SubscribeStore(String clientId, String topicName, int mqttQoS) {
        this.clientId = clientId;
        this.topicName = topicName;
        this.mqttQoS = mqttQoS;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public int getMqttQoS() {
        return mqttQoS;
    }

    public void setMqttQoS(int mqttQoS) {
        this.mqttQoS = mqttQoS;
    }

    @Override
    public String toString() {
        return "SubscribeStore{" +
                "clientId='" + clientId + '\'' +
                ", topicFilter='" + topicName + '\'' +
                ", mqttQoS=" + mqttQoS +
                '}';
    }
}
