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
        logger.info("客户端建立连接---"+ctx.channel().id().asLongText());
        logger.info("连接后CHANNEL_ID_MAP-----" + ChannelCache.CHANNEL_ID_MAP);
        logger.info("ctx---" + ctx);
        super.channelRegistered(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        channelGroup.remove(ctx.channel());
        ChannelCache.CHANNEL_ID_MAP.remove(ctx.channel().id().asLongText());
        logger.info("客户端断开连接---"+ctx.channel().id().asLongText());
        logger.info("断开连接后CHANNEL_ID_MAP-----" + ChannelCache.CHANNEL_ID_MAP);
        super.channelUnregistered(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg != null) {
            MqttMessage mqttMessage = (MqttMessage) msg;
            logger.info("收到客户端请求-------类型：" + mqttMessage.fixedHeader().messageType() + ",消息内容：" + mqttMessage);
            MqttFixedHeader mqttFixedHeader = mqttMessage.fixedHeader();
            Channel channel = ctx.channel();
            switch (mqttFixedHeader.messageType()) {
                //客户端连接请求
                case CONNECT:
                    MqttMessageBack.connectMessageHandler(channel, mqttMessage);
                    MqttMessageBack.connack(channel, mqttMessage);
                    break;
                //客户端订阅请求
                case SUBSCRIBE:
                    MqttMessageBack.subscribeAck(channel, mqttMessage);
                    break;
                //客户端发布消息请求
                case PUBLISH:
                    publishToMq(mqttMessage);
                    MqttMessageBack.pubAckOrPubRec(channel, mqttMessage);
                    break;
                case PUBACK:
                    MqttMessageBack.processPubAck(channel,mqttMessage);
                    break;
                //QoS为2时，服务端发布消息，收到客户端的第一次确认pubrec报文，返回一个pubrel报文
                case PUBREC:
                    MqttMessageBack.pubRel(channel, mqttMessage);
                    break;
                //QoS为2时，客户端发布消息，收到客户端的pubrel报文，返回第二次确认pubcomp报文
                case PUBREL:
                    MqttMessageBack.pubComp(channel, mqttMessage);
                    break;
                //QoS为2时，服务端发布消息，收到客户端的pubcomp报文
                case PUBCOMP:
                    MqttMessageBack.processComp(channel,mqttMessage);
                    break;
                //取消订阅
                case UNSUBSCRIBE:
                    MqttMessageBack.unSubAck(channel, mqttMessage);
                    break;
                //心跳机制ping请求响应
                case PINGREQ:
                    MqttMessageBack.pingResp(channel, mqttMessage);
                    break;
                //客户端断开连接
                case DISCONNECT:
                    MqttMessageBack.disconnect(channel, mqttMessage);
                    break;
                default:
                    logger.info("接收报文类型错误------" + mqttFixedHeader.messageType());
                    break;
            }
        }
    }

    private void publishToMq(MqttMessage mqttMessage){
        BrokerMqttMessage brokerMqttMessage = new BrokerMqttMessage(UUID.randomUUID().toString(),
                mqttMessage.fixedHeader(),mqttMessage.variableHeader(),mqttMessage.payload(),
                mqttMessage.decoderResult());
        logger.info("发送消息：{}", JSON.toJSONString(brokerMqttMessage));
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
                        //遗嘱处理
                    }
                }
                ctx.close();
            }
        }else {
            super.userEventTriggered(ctx, evt);
        }

    }
}
