package com.lmk.mqtt.entity;

import io.netty.channel.ChannelId;
import io.netty.handler.codec.mqtt.MqttPublishMessage;

import javax.xml.soap.SAAJMetaFactory;

/**
 * @author lmk
 * @version 1.0.0
 * @ClassName SessionStore.java
 * @Description TODO
 * @createTime 2022-06-23 18:01:31
 */
public class SessionStore {
    private String clientId;
    private String channelId;
    private boolean cleanSession;
    private MqttPublishMessage willMessage;

    public SessionStore() {
    }

    public SessionStore(String clientId, String channelId, boolean cleanSession, MqttPublishMessage willMessage) {
        this.clientId = clientId;
        this.channelId = channelId;
        this.cleanSession = cleanSession;
        this.willMessage = willMessage;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public boolean isCleanSession() {
        return cleanSession;
    }

    public void setCleanSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
    }

    public MqttPublishMessage getWillMessage() {
        return willMessage;
    }

    public void setWillMessage(MqttPublishMessage willMessage) {
        this.willMessage = willMessage;
    }

    @Override
    public String toString() {
        return "SessionStore{" +
                "clientId='" + clientId + '\'' +
                ", channelId='" + channelId + '\'' +
                ", cleanSession=" + cleanSession +
                ", willMessage=" + willMessage +
                '}';
    }
}
