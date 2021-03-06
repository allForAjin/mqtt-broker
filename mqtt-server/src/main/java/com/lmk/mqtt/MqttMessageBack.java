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
            logger.info("?????????id:" + payload.clientIdentifier() + "??????session");
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
     * ??????????????????
     *
     * @param channel     ??????
     * @param mqttMessage mqtt??????
     * @return void
     * @author lmk
     * @Date 2022/6/21 18:10
     */
    public static void connack(Channel channel, MqttMessage mqttMessage) {
        MqttConnectMessage mqttConnectMessage = (MqttConnectMessage) mqttMessage;
        MqttFixedHeader mqttFixedHeaderConnect = mqttConnectMessage.fixedHeader();
        MqttConnectVariableHeader mqttConnectVariableHeader = mqttConnectMessage.variableHeader();
        boolean sessionPresent = ChannelCache.SESSION_STORE_MAP.containsKey(mqttConnectMessage.payload().clientIdentifier()) && !mqttConnectMessage.variableHeader().isCleanSession();

        //mqtt???????????????????????????
        //MqttConnectReturnCode.CONNECTION_ACCEPTED????????????
        //mqttConnectVariableHeader.isCleanSession()?????????????????????cleanSession
        MqttConnAckVariableHeader mqttConnAckVariableHeader = new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_ACCEPTED, sessionPresent);

        //mqtt???????????????????????????
        MqttFixedHeader mqttFixedHeaderBack = new MqttFixedHeader(MqttMessageType.CONNACK,
                mqttFixedHeaderConnect.isDup(),
                MqttQoS.AT_MOST_ONCE,
                mqttFixedHeaderConnect.isRetain(),
                2);

        MqttConnAckMessage connAckMessage = new MqttConnAckMessage(mqttFixedHeaderBack, mqttConnAckVariableHeader);
        logger.info("???????????????connack--------" + connAckMessage);
        channel.writeAndFlush(connAckMessage);
    }

    /**
     * ???????????? ping??????
     *
     * @param channel     ??????
     * @param mqttMessage mqtt??????
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
     * ????????????
     *
     * @param channel     ??????
     * @param mqttMessage mqtt??????
     * @author lmk
     * @Date 2022/6/21 13:58
     */
    public static void subscribeAck(Channel channel, MqttMessage mqttMessage) {
        String clientId = (String) channel.attr(AttributeKey.valueOf("clientId")).get();
        //?????????????????????
        if (!ChannelCache.CLIENT_SUBSCRIBE_CACHE.containsKey(clientId)) {
            ChannelCache.CLIENT_SUBSCRIBE_CACHE.put(clientId, new ArrayList<>());
        }


        MqttSubscribeMessage subscribeMessage = (MqttSubscribeMessage) mqttMessage;
        //??????subscribe??????????????????
        MqttMessageIdVariableHeader variableHeader = subscribeMessage.variableHeader();

        //??????suback????????????????????????
        MqttMessageIdVariableHeader variableHeaderBack = MqttMessageIdVariableHeader.from(variableHeader.messageId());

        //????????????map
        Map<String, SubscribeStore> map = subscribeMessage.payload()
                .topicSubscriptions()
                .stream()
                .map(sub -> new SubscribeStore(clientId, sub.topicName(), sub.qualityOfService().value()))
                .collect(Collectors.toMap(SubscribeStore::getTopicName, subscribeStore -> subscribeStore));
        logger.info("??????map???" + map);
        //grantedQoSLevels??????????????????
        List<Integer> grantedQoSLevels = map.values().stream().map(SubscribeStore::getMqttQoS).collect(Collectors.toList());


        //suback????????????????????????
        MqttSubAckPayload subAckPayload = new MqttSubAckPayload(grantedQoSLevels);
        //????????????????????????
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
        logger.info("?????????????????????????????????" + ChannelCache.CLIENT_SUBSCRIBE_CACHE);
    }

    /**
     * ??????????????????
     *
     * @param channel     ??????
     * @param mqttMessage mqtt??????
     * @return void
     * @author lmk
     * @Date 2022/6/21 13:59
     */
    public static void unSubAck(Channel channel, MqttMessage mqttMessage) {
        MqttUnsubscribeMessage mqttUnsubscribeMessage = (MqttUnsubscribeMessage) mqttMessage;
        MqttMessageIdVariableHeader variableHeader = mqttUnsubscribeMessage.variableHeader();
        List<String> unSubTopicList = mqttUnsubscribeMessage.payload().topics();
        logger.info("??????????????????------" + unSubTopicList);


        MqttMessageIdVariableHeader variableHeaderBack = MqttMessageIdVariableHeader.from(variableHeader.messageId());
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.UNSUBACK, false, MqttQoS.AT_MOST_ONCE, false, 2);
        MqttUnsubAckMessage mqttUnsubAckMessage = new MqttUnsubAckMessage(mqttFixedHeader, variableHeaderBack);
        List<SubscribeStore> cacheTopicList = ChannelCache.CLIENT_SUBSCRIBE_CACHE.get(channel.attr(AttributeKey.valueOf("clientId")).get());

        if (cacheTopicList != null) {
            //????????????????????????????????????
            List<SubscribeStore> result = cacheTopicList.stream()
                    .filter(cacheTopic -> !unSubTopicList.contains(cacheTopic.getTopicName()))
                    .collect(Collectors.toList());
            ChannelCache.CLIENT_SUBSCRIBE_CACHE.put((String) channel.attr(AttributeKey.valueOf("clientId")).get(), result);
            logger.info("????????????????????????????????????-------" + ChannelCache.CLIENT_SUBSCRIBE_CACHE);
        }


        channel.writeAndFlush(mqttUnsubAckMessage);
    }

    /**
     * QoS???1?????????????????????
     *
     * @param channel     ??????
     * @param mqttMessage mqtt??????
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
        //????????????
        MqttFixedHeader fixedHeader;
        //????????????
        MqttMessageIdVariableHeader mqttMessageIdVariableHeaderBack = MqttMessageIdVariableHeader.from(mqttPublishMessage.variableHeader().packetId());

        //??????QoS???????????????????????????
        switch (mqttQoS) {
            case AT_MOST_ONCE:
                //QoS???0???????????????????????????????????????
                break;
            case AT_LEAST_ONCE:
                //QoS???1?????????puback??????
                fixedHeader = new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 2);
                MqttPubAckMessage mqttPubAckMessage = new MqttPubAckMessage(fixedHeader, mqttMessageIdVariableHeaderBack);
                channel.writeAndFlush(mqttPubAckMessage);
                break;
            case EXACTLY_ONCE:
                //QoS???2?????????pubrec??????
                fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREC, false, MqttQoS.AT_LEAST_ONCE, false, 2);
                MqttMessage mqttMessagePubRec = new MqttMessage(fixedHeader, mqttMessageIdVariableHeaderBack);
                channel.writeAndFlush(mqttMessagePubRec);
                break;
            default:
                break;
        }
        //?????????????????????????????????
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
     * QoS???2???????????????????????????????????????
     * ???????????????????????????pubRel
     *
     * @param channel     ??????
     * @param mqttMessage mqtt??????
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
     * QoS???2??????????????????????????????????????????
     * ???????????????????????????pubcomp?????????????????????????????????
     *
     * @param channel     ??????
     * @param mqttMessage mqtt??????
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
        logger.info("processComp-qos=2?????????????????????--" + ChannelCache.PUB_REL_STORE_MAP);
    }

    /**
     * ?????????????????????
     *
     * @param channel     ???????????????
     * @param mqttMessage mqtt??????
     * @author lmk
     * @Date 2022/6/23 14:43
     */
    public static void disconnect(Channel channel, MqttMessage mqttMessage) {
        String clientId = (String) channel.attr(AttributeKey.valueOf("clientId")).get();
        logger.info("disconnect-------" + mqttMessage);
        SessionStore sessionStore = ChannelCache.SESSION_STORE_MAP.get(clientId);
        //??????cleanSession???ture????????????
        if (sessionStore != null && sessionStore.isCleanSession()) {
            ChannelCache.CLIENT_SUBSCRIBE_CACHE.remove(clientId);
            ChannelCache.PUB_REL_STORE_MAP.remove(clientId);
            ChannelCache.PUBLISH_MESSAGE_STORE_MAP.remove(clientId);
        }
        ChannelCache.SESSION_STORE_MAP.remove(clientId);
        channel.close();
        logger.info("???????????????CLIENT_SUBSCRIBE_CACHE-----" + ChannelCache.CLIENT_SUBSCRIBE_CACHE);
        logger.info("???????????????SESSION_STORE_MAP-----" + ChannelCache.SESSION_STORE_MAP);
    }

    /**
     * ?????????????????????
     *
     * @param message  ????????????
     * @param topic    ??????
     * @param qos      QoS
     * @param retained ????????????
     * @param header   ??????????????????????????????topic???packetId)
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
        //????????????????????????????????????
        ChannelCache.CLIENT_SUBSCRIBE_CACHE.forEach((clientId, subTopicList) -> {
            //?????????????????????????????????????????????
            subTopicList.forEach(subTopic -> {
                //????????????????????????
                if (subTopic.getTopicName().equals(topic)) {
                    //??????qos
                    MqttQoS mqttQoS = getMinQos(subTopic.getMqttQoS(), qos);
                    doPublish(message, mqttQoS, false, header, clientId);
                }
            });
        });
    }

    /**
     * ????????????
     *
     * @param message  ????????????
     * @param mqttQoS  QoS
     * @param retained ????????????
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
        //??????????????????payload
        ByteBuf payload = Unpooled.buffer();
        payload.writeBytes(message.getBytes());
        //??????????????????
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false, mqttQoS, retained, 2);
        //publish???????????????????????????????????????????????????????????????
        MqttPublishMessage pubToClientMessage = new MqttPublishMessage(mqttFixedHeader, header, payload);
        channel.writeAndFlush(pubToClientMessage);
    }

    /**
     * ??????????????????
     *
     * @param subTopic ????????????
     * @param qos      QoS
     * @param clientId clientId
     * @return void
     * @author lmk
     * @Date 2022/6/23 14:35
     */
    private static void publishRetainedMessage(String subTopic, int qos, String clientId) {
        logger.info("??????????????????------" + ChannelCache.RETAINED_MESSAGE_CACHE);
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
        logger.info("??????????????????------" + retainedMessage);
        MqttQoS mqttQoS = getMinQos(retainedMessage.getQos(), qos);
        MqttPublishVariableHeader header = new MqttPublishVariableHeader(subTopic, retainedMessage.getPacketId());
        doPublish(retainedMessage.getMessage(), mqttQoS, true, header, clientId);

    }


    /**
     * ????????????qos??????????????????
     * ????????????????????????????????????????????????
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
