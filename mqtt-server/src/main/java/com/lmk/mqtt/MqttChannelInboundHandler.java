package com.lmk.mqtt;

import com.alibaba.fastjson2.JSON;
import com.lmk.mqtt.cache.ChannelCache;
import com.lmk.mqtt.entity.BrokerMqttMessage;
import com.lmk.mqtt.entity.SessionStore;
import com.lmk.mqtt.exchange.ExchangeEnum;
import com.lmk.mqtt.service.api.MqService;
import com.sun.org.apache.bcel.internal.generic.BREAKPOINT;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


/**
 * @author lmk
 * @version 1.0.0
 * @ClassName MqttChannelInboundHandler.java
 * @Description TODO
 * @createTime 2022-06-18 13:35:19
 */
@Component
public class MqttChannelInboundHandler extends ChannelInboundHandlerAdapter {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ChannelGroup channelGroup;

    @Autowired
    private MqService mqService;

    public MqttChannelInboundHandler(ChannelGroup channelGroup) {
        this.channelGroup = channelGroup;
        MqttMessageBack.setChannelGroup(channelGroup);
    }


    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        channelGroup.add(ctx.channel());
        ChannelCache.CHANNEL_ID_MAP.put(ctx.channel().id().asLongText(),ctx.channel().id());
        logger.info("?????????????????????---"+ctx.channel().id().asLongText());
        logger.info("?????????CHANNEL_ID_MAP-----" + ChannelCache.CHANNEL_ID_MAP);
        logger.info("ctx---" + ctx);
        super.channelRegistered(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        channelGroup.remove(ctx.channel());
        ChannelCache.CHANNEL_ID_MAP.remove(ctx.channel().id().asLongText());
        logger.info("?????????????????????---"+ctx.channel().id().asLongText());
        logger.info("???????????????CHANNEL_ID_MAP-----" + ChannelCache.CHANNEL_ID_MAP);
        super.channelUnregistered(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg != null) {
            MqttMessage mqttMessage = (MqttMessage) msg;
            logger.info("?????????????????????-------?????????" + mqttMessage.fixedHeader().messageType() + ",???????????????" + mqttMessage);
            MqttFixedHeader mqttFixedHeader = mqttMessage.fixedHeader();
            Channel channel = ctx.channel();
            switch (mqttFixedHeader.messageType()) {
                //?????????????????????
                case CONNECT:
                    MqttMessageBack.connectMessageHandler(channel, mqttMessage);
                    MqttMessageBack.connack(channel, mqttMessage);
                    break;
                //?????????????????????
                case SUBSCRIBE:
                    MqttMessageBack.subscribeAck(channel, mqttMessage);
                    break;
                //???????????????????????????
                case PUBLISH:
                    publishToMq(mqttMessage);
                    MqttMessageBack.pubAckOrPubRec(channel, mqttMessage);
                    break;
                case PUBACK:
                    MqttMessageBack.processPubAck(channel,mqttMessage);
                    break;
                //QoS???2???????????????????????????????????????????????????????????????pubrec?????????????????????pubrel??????
                case PUBREC:
                    MqttMessageBack.pubRel(channel, mqttMessage);
                    break;
                //QoS???2????????????????????????????????????????????????pubrel??????????????????????????????pubcomp??????
                case PUBREL:
                    MqttMessageBack.pubComp(channel, mqttMessage);
                    break;
                //QoS???2????????????????????????????????????????????????pubcomp??????
                case PUBCOMP:
                    MqttMessageBack.processComp(channel,mqttMessage);
                    break;
                //????????????
                case UNSUBSCRIBE:
                    MqttMessageBack.unSubAck(channel, mqttMessage);
                    break;
                //????????????ping????????????
                case PINGREQ:
                    MqttMessageBack.pingResp(channel, mqttMessage);
                    break;
                //?????????????????????
                case DISCONNECT:
                    MqttMessageBack.disconnect(channel, mqttMessage);
                    break;
                default:
                    logger.info("????????????????????????------" + mqttFixedHeader.messageType());
                    break;
            }
        }
    }

    private void publishToMq(MqttMessage mqttMessage){
        BrokerMqttMessage brokerMqttMessage = new BrokerMqttMessage(UUID.randomUUID().toString(),
                mqttMessage.fixedHeader(),mqttMessage.variableHeader(),mqttMessage.payload(),
                mqttMessage.decoderResult());
        logger.info("???????????????{}", JSON.toJSONString(brokerMqttMessage));
        mqService.send(JSON.toJSONString(brokerMqttMessage), ExchangeEnum.DEFAULT_FANOUT_EXCHANGE,"");
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof IOException){
            ctx.close();
        }else {
            super.exceptionCaught(ctx, cause);
        }
    }


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent){
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            if (idleStateEvent.state()== IdleState.ALL_IDLE){
                Channel channel = ctx.channel();
                String clientId = (String) channel.attr(AttributeKey.valueOf("clientId")).get();

                if (ChannelCache.SESSION_STORE_MAP.containsKey(clientId)){
                    SessionStore sessionStore = ChannelCache.SESSION_STORE_MAP.get(clientId);
                    if (sessionStore.getWillMessage()!=null){
                        //????????????
                    }
                }
                ctx.close();
            }
        }else {
            super.userEventTriggered(ctx, evt);
        }

    }
}
