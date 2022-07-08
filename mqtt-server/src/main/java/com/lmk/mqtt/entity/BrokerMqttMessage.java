package com.lmk.mqtt.entity;

import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.mqtt.MqttFixedHeader;

public class BrokerMqttMessage {
    private String brokerId;
    private MqttFixedHeader mqttFixedHeader;
    private Object variableHeader;
    private Object payload;
    private DecoderResult decoderResult;

    public BrokerMqttMessage(String brokerId, MqttFixedHeader mqttFixedHeader, Object variableHeader, Object payload, DecoderResult decoderResult) {
        this.brokerId = brokerId;
        this.mqttFixedHeader = mqttFixedHeader;
        this.variableHeader = variableHeader;
        this.payload = payload;
        this.decoderResult = decoderResult;
    }

    public String getBrokerId() {
        return brokerId;
    }

    public void setBrokerId(String brokerId) {
        this.brokerId = brokerId;
    }

    public MqttFixedHeader getMqttFixedHeader() {
        return mqttFixedHeader;
    }

    public void setMqttFixedHeader(MqttFixedHeader mqttFixedHeader) {
        this.mqttFixedHeader = mqttFixedHeader;
    }

    public Object getVariableHeader() {
        return variableHeader;
    }

    public void setVariableHeader(Object variableHeader) {
        this.variableHeader = variableHeader;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public DecoderResult getDecoderResult() {
        return decoderResult;
    }

    public void setDecoderResult(DecoderResult decoderResult) {
        this.decoderResult = decoderResult;
    }

    @Override
    public String toString() {
        return "BrokerMqttMessage{" +
                "brokerId='" + brokerId + '\'' +
                ", mqttFixedHeader=" + mqttFixedHeader +
                ", variableHeader=" + variableHeader +
                ", payload=" + payload +
                ", decoderResult=" + decoderResult +
                '}';
    }
}
