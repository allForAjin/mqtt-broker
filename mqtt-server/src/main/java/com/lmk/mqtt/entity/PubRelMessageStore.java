package com.lmk.mqtt.entity;

/**
 * @author lmk
 * @version 1.0.0
 * @ClassName PubRelMessageStore.java
 * @Description TODO
 * @createTime 2022-06-23 20:53:10
 */
public class PubRelMessageStore {
    private String clientId;

    private int messageId;

    public PubRelMessageStore() {
    }

    public PubRelMessageStore(String clientId, int messageId) {
        this.clientId = clientId;
        this.messageId = messageId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    @Override
    public String toString() {
        return "PubRelMessageStore{" +
                "clientId='" + clientId + '\'' +
                ", messageId=" + messageId +
                '}';
    }
}
