package com.lmk.mqtt;

import com.lmk.mqtt.cache.ChannelCache;
import com.lmk.mqtt.entity.*;
import com.lmk.mqtt.exception.NullChannelException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * @author lmk
 * @version 1.0.0
 * @ClassName MqttMessageBack.java
 * @Description TODO
 * @createTime 2022-06-18 14:00:03
 */
public class MqttMessageBack {
    private static final Logger logger = LoggerFactory.getLogger(MqttMessageBack.class);
    private static ChannelGroup channelGroup;

    public static void connectMessageHandler(Channel channel, MqttMessage mqttMessage) {
        MqttConnectPayload payload = (MqttConnectPayload) mqttMessage.payload();
        MqttConnectMessage connectMessage;
        try {
            connectMessage = (MqttConnectMessage) mqttMessage;
        }catch (ClassCastException e){
            e.printStackTrace();
            return;
        }


        channel.attr(AttributeKey.valueOf("clientId")).set(connectMessage.payload().clientIdentifier());
        if (ChannelCache.SESSION_STORE_MAP.containsKey(connectMessage.payload().clientIdentifier())) {
            SessionStore sessionStore = ChannelCache.SESSION_STORE_MAP.get(connectMessage.payload().clientIdentifier());
            String clientId = sessionStore.getClientId();
            boolean cleanSession = connectMessage.variableHeader().isCleanSession();
            if (cleanSession) {
                ChannelCache.SESSION_STORE_MAP.remove(clientId);
                ChannelCache.CLIENT_SUBSCRIBE_CACHE.remove(clientId);
                ChannelCache.PUBLISH_MESSAGE_STORE_MAP.remove(clientId);
                ChannelCache.PUB_REL_STORE_MAP.remove(clientId);
            }
            logger.info("客户端id:" + payload.clientIdentifier() + "存在session");
            ChannelId channelId = ChannelCache.CHANNEL_ID_MAP.get(sessionStore.getChannelId());
            if (channelId != null) {
                Channel previous = channelGroup.find(channelId);
                if (previous != null) {
                    previous.close();
                }
            }

        } else {
            ChannelCache.CLIENT_SUBSCRIBE_CACHE.remove(connectMessage.payload().clientIdentifier());
            ChannelCache.PUBLISH_MESSAGE_STORE_MAP.remove(connectMessage.payload().clientIdentifier());
            ChannelCache.PUB_REL_STORE_MAP.remove(connectMessage.payload().clientIdentifier());
        }


        SessionStore sessionStore = new SessionStore(connectMessage.payload().clientIdentifier(), channel.id().asLongText(), connectMessage.variableHeader().isCleanSession(), null);
        ChannelCache.SESSION_STORE_MAP.put(connectMessage.payload().clientIdentifier(), sessionStore);
        logger.info("CHANNEL_ID_MAP---" + ChannelCache.CHANNEL_ID_MAP);
        logger.info("SESSION_STORE_MAP---" + ChannelCache.SESSION_STORE_MAP);

        if (!connectMessage.variableHeader().isCleanSession()) {
            List<PublishMessageStore> publishMessageStores = ChannelCache.PUBLISH_MESSAGE_STORE_MAP.get(connectMessage.payload().clientIdentifier());
            List<PubRelMessageStore> pubRelMessageStores = ChannelCache.PUB_REL_STORE_MAP.get(connectMessage.payload().clientIdentifier());
            if (publishMessageStores != null) {
                publishMessageStores.forEach(message -> {
                    MqttPublishMessage publishMessage = (MqttPublishMessage) MqttMessageFactory.newMessage(
                            new MqttFixedHeader(MqttMessageType.PUBLISH, true, MqttQoS.valueOf(message.getMqttQoS()), false, 0),
                            new MqttPublishVariableHeader(message.getTopic(), message.getMessageId()), Unpooled.buffer().writeBytes(message.getMessage().getBytes()));
                    channel.writeAndFlush(publishMessage);
                });
            }

            if (pubRelMessageStores != null) {
                pubRelMessageStores.forEach(message -> {
                    MqttMessage pubRelMessage = MqttMessageFactory.newMessage(
                            new MqttFixedHeader(MqttMessageType.PUBREL, true, MqttQoS.AT_MOST_ONCE, false, 0),
                            MqttMessageIdVariableHeader.from(message.getMessageId()), null);
                    channel.writeAndFlush(pubRelMessage);
                });
            }


        }

    }


    /**
     * 确认连接响应
     *
     * @param channel     通道
     * @param mqttMessage mqtt消息
     * @return void
     * @author lmk
     * @Date 2022/6/21 18:10
     */
    public static void connack(Channel channel, MqttMessage mqttMessage) {
        MqttConnectMessage mqttConnectMessage = (MqttConnectMessage) mqttMessage;
        MqttFixedHeader mqttFixedHeaderConnect = mqttConnectMessage.fixedHeader();
        MqttConnectVariableHeader mqttConnectVariableHeader = mqttConnectMessage.variableHeader();
        boolean sessionPresent = ChannelCache.SESSION_STORE_MAP.containsKey(mqttConnectMessage.payload().clientIdentifier()) && !mqttConnectMessage.variableHeader().isCleanSession();

        //mqtt返回报文，可变报头
        //MqttConnectReturnCode.CONNECTION_ACCEPTED接收连接
        //mqttConnectVariableHeader.isCleanSession()获取请求报文头cleanSession
        MqttConnAckVariableHeader mqttConnAckVariableHeader = new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_ACCEPTED, sessionPresent);

        //mqtt返回报文，固定报头
        MqttFixedHeader mqttFixedHeaderBack = new MqttFixedHeader(MqttMessageType.CONNACK,
                mqttFixedHeaderConnect.isDup(),
                MqttQoS.AT_MOST_ONCE,
                mqttFixedHeaderConnect.isRetain(),
                2);

        MqttConnAckMessage connAckMessage = new MqttConnAckMessage(mqttFixedHeaderBack, mqttConnAckVariableHeader);
        logger.info("响应客户端connack--------" + connAckMessage);
        channel.writeAndFlush(connAckMessage);
    }

    /**
     * 心跳机制 ping响应
     *
     * @param channel     通道
     * @param mqttMessage mqtt消息
     * @return void
     * @author lmk
     * @Date 2022/6/21 13:59
     */
    public static void pingResp(Channel channel, MqttMessage mqttMessage) {
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttMessage mqttMessageBack = new MqttMessage(mqttFixedHeader);
        logger.info("pingResp------" + mqttMessageBack);
        channel.writeAndFlush(mqttMessageBack);
    }

    /**
     * 订阅确认
     *
     * @param channel     通道
     * @param mqttMessage mqtt消息
     * @author lmk
     * @Date 2022/6/21 13:58
     */
    public static void subscribeAck(Channel channel, MqttMessage mqttMessage) {
        String clientId = (String) channel.attr(AttributeKey.valueOf("clientId")).get();
        //订阅列表初始化
        if (!ChannelCache.CLIENT_SUBSCRIBE_CACHE.containsKey(clientId)) {
            ChannelCache.CLIENT_SUBSCRIBE_CACHE.put(clientId, new ArrayList<>());
        }


        MqttSubscribeMessage subscribeMessage = (MqttSubscribeMessage) mqttMessage;
        //获取subscribe报文可变报头
        MqttMessageIdVariableHeader variableHeader = subscribeMessage.variableHeader();

        //设置suback返回报文可变报头
        MqttMessageIdVariableHeader variableHeaderBack = MqttMessageIdVariableHeader.from(variableHeader.messageId());

        //订阅列表map
        Map<String, SubscribeStore> map = subscribeMessage.payload()
                .topicSubscriptions()
                .stream()
                .map(sub -> new SubscribeStore(clientId, sub.topicName(), sub.qualityOfService().value()))
                .collect(Collectors.toMap(SubscribeStore::getTopicName, subscribeStore -> subscribeStore));
        logger.info("订阅map：" + map);
        //grantedQoSLevels用于有效载荷
        List<Integer> grantedQoSLevels = map.values().stream().map(SubscribeStore::getMqttQoS).collect(Collectors.toList());


        //suback报文需要有效载荷
        MqttSubAckPayload subAckPayload = new MqttSubAckPayload(grantedQoSLevels);
        //返回报文固定报头
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.SUBACK, false, MqttQoS.AT_MOST_ONCE, false, map.size() + 2);
        MqttSubAckMessage mqttSubAckMessage = new MqttSubAckMessage(fixedHeader, variableHeaderBack, subAckPayload);
        channel.writeAndFlush(mqttSubAckMessage);
        List<SubscribeStore> subscribeStores = ChannelCache.CLIENT_SUBSCRIBE_CACHE.get(clientId);

        map.forEach((key, value) -> {
            List<SubscribeStore> collect = subscribeStores.stream().filter(sub -> !sub.getTopicName().equals(key)).collect(Collectors.toList());
            collect.add(value);
            ChannelCache.CLIENT_SUBSCRIBE_CACHE.put(clientId, collect);
            publishRetainedMessage(value.getTopicName(), value.getMqttQoS(), clientId);
        });
        logger.info("订阅后服务端订阅列表：" + ChannelCache.CLIENT_SUBSCRIBE_CACHE);
    }

    /**
     * 取消订阅确认
     *
     * @param channel     通道
     * @param mqttMessage mqtt消息
     * @return void
     * @author lmk
     * @Date 2022/6/21 13:59
     */
    public static void unSubAck(Channel channel, MqttMessage mqttMessage) {
        MqttUnsubscribeMessage mqttUnsubscribeMessage = (MqttUnsubscribeMessage) mqttMessage;
        MqttMessageIdVariableHeader variableHeader = mqttUnsubscribeMessage.variableHeader();
        List<String> unSubTopicList = mqttUnsubscribeMessage.payload().topics();
        logger.info("取消订阅列表------" + unSubTopicList);


        MqttMessageIdVariableHeader variableHeaderBack = MqttMessageIdVariableHeader.from(variableHeader.messageId());
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.UNSUBACK, false, MqttQoS.AT_MOST_ONCE, false, 2);
        MqttUnsubAckMessage mqttUnsubAckMessage = new MqttUnsubAckMessage(mqttFixedHeader, variableHeaderBack);
        List<SubscribeStore> cacheTopicList = ChannelCache.CLIENT_SUBSCRIBE_CACHE.get(channel.attr(AttributeKey.valueOf("clientId")).get());

        if (cacheTopicList != null) {
            //过滤掉要被取消订阅的主题
            List<SubscribeStore> result = cacheTopicList.stream()
                    .filter(cacheTopic -> !unSubTopicList.contains(cacheTopic.getTopicName()))
                    .collect(Collectors.toList());
            ChannelCache.CLIENT_SUBSCRIBE_CACHE.put((String) channel.attr(AttributeKey.valueOf("clientId")).get(), result);
            logger.info("取消订阅后服务端订阅列表-------" + ChannelCache.CLIENT_SUBSCRIBE_CACHE);
        }


        channel.writeAndFlush(mqttUnsubAckMessage);
    }

    /**
     * QoS为1时的服务端确认
     *
     * @param channel     通道
     * @param mqttMessage mqtt消息
     * @return void
     * @author lmk
     * @Date 2022/6/21 18:09
     */
    public static void pubAckOrPubRec(Channel channel, MqttMessage mqttMessage) {
        MqttPublishMessage mqttPublishMessage = (MqttPublishMessage) mqttMessage;
        byte[] bytes = new byte[mqttPublishMessage.payload().readableBytes()];
        mqttPublishMessage.payload().readBytes(bytes);
        String message = new String(bytes);
        String topic = mqttPublishMessage.variableHeader().topicName();
        boolean retained = mqttPublishMessage.fixedHeader().isRetain();

        MqttQoS mqttQoS = mqttPublishMessage.fixedHeader().qosLevel();
        //固定报头
        MqttFixedHeader fixedHeader;
        //可变报头
        MqttMessageIdVariableHeader mqttMessageIdVariableHeaderBack = MqttMessageIdVariableHeader.from(mqttPublishMessage.variableHeader().packetId());

        //根据QoS选择对应的返回报文
        switch (mqttQoS) {
            case AT_MOST_ONCE:
                //QoS为0时直接发送消息，无返回报文
                break;
            case AT_LEAST_ONCE:
                //QoS为1时返回puback报文
                fixedHeader = new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 2);
                MqttPubAckMessage mqttPubAckMessage = new MqttPubAckMessage(fixedHeader, mqttMessageIdVariableHeaderBack);
                channel.writeAndFlush(mqttPubAckMessage);
                break;
            case EXACTLY_ONCE:
                //QoS为2时返回pubrec报文
                fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREC, false, MqttQoS.AT_LEAST_ONCE, false, 2);
                MqttMessage mqttMessagePubRec = new MqttMessage(fixedHeader, mqttMessageIdVariableHeaderBack);
                channel.writeAndFlush(mqttMessagePubRec);
                break;
            default:
                break;
        }
        //服务端向客户端推送消息
        publishMessage(message, topic, mqttQoS.value(), retained, mqttPublishMessage.variableHeader());
    }


    public static void processPubAck(Channel channel, MqttMessage mqttMessage) {
        String clientId = (String) channel.attr(AttributeKey.valueOf("clientId")).get();
        MqttMessageIdVariableHeader mqttMessageIdVariableHeader = (MqttMessageIdVariableHeader) mqttMessage.variableHeader();
        int messageId = mqttMessageIdVariableHeader.messageId();
        List<PublishMessageStore> publishMessageStoreList = ChannelCache.PUBLISH_MESSAGE_STORE_MAP.get(clientId);
        if (publishMessageStoreList == null) {
            publishMessageStoreList = new ArrayList<>();
        }
        publishMessageStoreList = publishMessageStoreList.stream()
                .filter(message -> message.getMessageId() != messageId)
                .collect(Collectors.toList());
        ChannelCache.PUBLISH_MESSAGE_STORE_MAP.put(clientId, publishMessageStoreList);

    }

    /**
     * QoS为2时服务端向客户端发送的确认
     * 服务端向客户端发送pubRel
     *
     * @param channel     通道
     * @param mqttMessage mqtt消息
     * @return void
     * @author lmk
     * @Date 2022/6/21 19:22
     */
    public static void pubRel(Channel channel, MqttMessage mqttMessage) {
        MqttMessageIdVariableHeader mqttMessageIdVariableHeaderBack = (MqttMessageIdVariableHeader) mqttMessage.variableHeader();
        String clientId = (String) channel.attr(AttributeKey.valueOf("clientId")).get();
        List<PublishMessageStore> publishMessageStoreList = ChannelCache.PUBLISH_MESSAGE_STORE_MAP.get(clientId);
        if (publishMessageStoreList == null) {
            publishMessageStoreList = new ArrayList<>();
        }
        publishMessageStoreList = publishMessageStoreList.stream()
                .filter(message -> message.getMessageId() != mqttMessageIdVariableHeaderBack.messageId())
                .collect(Collectors.toList());
        ChannelCache.PUBLISH_MESSAGE_STORE_MAP.put(clientId, publishMessageStoreList);
        PubRelMessageStore pubRelMessageStore = new PubRelMessageStore(clientId, mqttMessageIdVariableHeaderBack.messageId());
        List<PubRelMessageStore> relList = ChannelCache.PUB_REL_STORE_MAP.get(clientId);
        if (relList == null) {
            relList = new ArrayList<>();
        }
        relList.add(pubRelMessageStore);
        ChannelCache.PUB_REL_STORE_MAP.put(clientId, relList);
        MqttFixedHeader fixedHeaderBack = new MqttFixedHeader(MqttMessageType.PUBREL, false, MqttQoS.AT_LEAST_ONCE, false, 2);
        MqttMessage mqttMessagePubRel = new MqttMessage(fixedHeaderBack, mqttMessageIdVariableHeaderBack);
        channel.writeAndFlush(mqttMessagePubRel);
    }

    /**
     * QoS为2客户端端发布消息时的二次确认
     * 服务端向客服端发送pubcomp，客户端发布消息时使用
     *
     * @param channel     通道
     * @param mqttMessage mqtt消息
     * @author lmk
     * @Date 2022/6/21 18:08
     */
    public static void pubComp(Channel channel, MqttMessage mqttMessage) {
        logger.info("pubcomp------" + mqttMessage);
        MqttMessageIdVariableHeader mqttMessageIdVariableHeaderBack = (MqttMessageIdVariableHeader) mqttMessage.variableHeader();
        MqttFixedHeader mqttFixedHeaderBack = new MqttFixedHeader(MqttMessageType.PUBCOMP, false, MqttQoS.AT_MOST_ONCE, false, 2);
        MqttMessage mqttMessagePubComp = new MqttMessage(mqttFixedHeaderBack, mqttMessageIdVariableHeaderBack);
        channel.writeAndFlush(mqttMessagePubComp);
    }

    public static void processComp(Channel channel, MqttMessage mqttMessage) {
        MqttMessageIdVariableHeader mqttMessageIdVariableHeader = (MqttMessageIdVariableHeader) mqttMessage.variableHeader();
        String clientId = (String) channel.attr(AttributeKey.valueOf("clientId")).get();
        List<PubRelMessageStore> pubRelMessageStores = ChannelCache.PUB_REL_STORE_MAP.get(clientId);
        pubRelMessageStores = pubRelMessageStores.stream()
                .filter(message -> message.getMessageId() != mqttMessageIdVariableHeader.messageId())
                .collect(Collectors.toList());
        ChannelCache.PUB_REL_STORE_MAP.put(clientId, pubRelMessageStores);
        logger.info("processComp-qos=2服务端确认完成--" + ChannelCache.PUB_REL_STORE_MAP);
    }

    /**
     * 断开客户端连接
     *
     * @param channel     客户端实例
     * @param mqttMessage mqtt报文
     * @author lmk
     * @Date 2022/6/23 14:43
     */
    public static void disconnect(Channel channel, MqttMessage mqttMessage) {
        String clientId = (String) channel.attr(AttributeKey.valueOf("clientId")).get();
        logger.info("disconnect-------" + mqttMessage);
        SessionStore sessionStore = ChannelCache.SESSION_STORE_MAP.get(clientId);
        //如果cleanSession为ture，则移除
        if (sessionStore != null && sessionStore.isCleanSession()) {
            ChannelCache.CLIENT_SUBSCRIBE_CACHE.remove(clientId);
            ChannelCache.PUB_REL_STORE_MAP.remove(clientId);
            ChannelCache.PUBLISH_MESSAGE_STORE_MAP.remove(clientId);
        }
        ChannelCache.SESSION_STORE_MAP.remove(clientId);
        channel.close();
        logger.info("断开连接后CLIENT_SUBSCRIBE_CACHE-----" + ChannelCache.CLIENT_SUBSCRIBE_CACHE);
        logger.info("断开连接后SESSION_STORE_MAP-----" + ChannelCache.SESSION_STORE_MAP);
    }

    /**
     * 服务端发布消息
     *
     * @param message  消息内容
     * @param topic    主题
     * @param qos      QoS
     * @param retained 保留消息
     * @param header   可变报头（包含：目标topic，packetId)
     * @author lmk
     * @Date 2022/6/23 14:41
     */
    private static void publishMessage(String message, String topic, int qos, boolean retained, MqttPublishVariableHeader header) {
        if (retained) {
            ChannelCache.RETAINED_MESSAGE_CACHE.put(topic, new RetainedMessage(message, topic, qos, header.packetId()));
            if (message == null || "".equals(message)) {
                ChannelCache.RETAINED_MESSAGE_CACHE.remove(topic);
            }
        }
        logger.info("ChannelCache.CLIENT_SUBSCRIBE_CACHE" + ChannelCache.CLIENT_SUBSCRIBE_CACHE);
        //首先遍历缓存中的订阅列表
        ChannelCache.CLIENT_SUBSCRIBE_CACHE.forEach((clientId, subTopicList) -> {
            //遍历订阅列表中是否有匹配的主题
            subTopicList.forEach(subTopic -> {
                //找到了匹配的主题
                if (subTopic.getTopicName().equals(topic)) {
                    //计算qos
                    MqttQoS mqttQoS = getMinQos(subTopic.getMqttQoS(), qos);
                    doPublish(message, mqttQoS, false, header, clientId);
                }
            });
        });
    }

    /**
     * 执行发布
     *
     * @param message  消息内容
     * @param mqttQoS  QoS
     * @param retained 保留消息
     * @param clientId
     * @author lmk
     * @Date 2022/6/23 14:38
     */
    private static void doPublish(String message, MqttQoS mqttQoS, boolean retained, MqttPublishVariableHeader header, String clientId) {
        List<PublishMessageStore> publishMessageStoreList = ChannelCache.PUBLISH_MESSAGE_STORE_MAP.get(clientId);
        if (publishMessageStoreList == null) {
            publishMessageStoreList = new ArrayList<>();
        }
        if (mqttQoS.equals(MqttQoS.AT_LEAST_ONCE) || mqttQoS.equals(MqttQoS.EXACTLY_ONCE)) {
            publishMessageStoreList.add(new PublishMessageStore(clientId, header.topicName(), mqttQoS.value(), header.packetId(), message));
        }
        SessionStore sessionStore = ChannelCache.SESSION_STORE_MAP.get(clientId);
        ChannelId channelId = ChannelCache.CHANNEL_ID_MAP.get(sessionStore.getChannelId());
        if (channelId==null){
            throw new NullChannelException("channelId can not be null,client maybe offline but session still exist");
        }
        Channel channel = channelGroup.find(channelId);
        //设置发送内容payload
        ByteBuf payload = Unpooled.buffer();
        payload.writeBytes(message.getBytes());
        //设置固定报头
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false, mqttQoS, retained, 2);
        //publish报文，可变报头为发布消息的客户端的可变报头
        MqttPublishMessage pubToClientMessage = new MqttPublishMessage(mqttFixedHeader, header, payload);
        channel.writeAndFlush(pubToClientMessage);
    }

    /**
     * 发送保留消息
     *
     * @param subTopic 目标主题
     * @param qos      QoS
     * @param clientId clientId
     * @return void
     * @author lmk
     * @Date 2022/6/23 14:35
     */
    private static void publishRetainedMessage(String subTopic, int qos, String clientId) {
        logger.info("保留消息列表------" + ChannelCache.RETAINED_MESSAGE_CACHE);
        if (!ChannelCache.RETAINED_MESSAGE_CACHE.containsKey(subTopic)) {
            return;
        }
        RetainedMessage retainedMessage = ChannelCache.RETAINED_MESSAGE_CACHE.get(subTopic);
        if (retainedMessage == null) {
            return;
        }
        if ("".equals(retainedMessage.getMessage())){
            ChannelCache.RETAINED_MESSAGE_CACHE.remove(subTopic);
        }
        logger.info("发送保留消息------" + retainedMessage);
        MqttQoS mqttQoS = getMinQos(retainedMessage.getQos(), qos);
        MqttPublishVariableHeader header = new MqttPublishVariableHeader(subTopic, retainedMessage.getPacketId());
        doPublish(retainedMessage.getMessage(), mqttQoS, true, header, clientId);

    }


    /**
     * 获取两个qos中较小的那个
     * 主要用于发布消息时的服务质量降级
     *
     * @param qos1 qos
     * @param qos2 qos
     * @return io.netty.handler.codec.mqtt.MqttQoS
     * @author lmk
     * @Date 2022/6/21 14:47
     */
    private static MqttQoS getMinQos(int qos1, int qos2) {
        int qos = Math.min(qos1, qos2);
        switch (qos) {
            case 0:
                return MqttQoS.AT_MOST_ONCE;
            case 1:
                return MqttQoS.AT_LEAST_ONCE;
            case 2:
                return MqttQoS.EXACTLY_ONCE;
            default:
                return MqttQoS.FAILURE;
        }
    }

    public static void setChannelGroup(ChannelGroup channelGroupBean) {
        MqttMessageBack.channelGroup = channelGroupBean;
    }


}
