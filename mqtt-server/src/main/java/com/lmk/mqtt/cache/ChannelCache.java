package com.lmk.mqtt.cache;

import com.lmk.mqtt.entity.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author lmk
 * @version 1.0.0
 * @ClassName ChannelCache.java
 * @Description TODO
 * @createTime 2022-06-21 09:33:21
 */
public class ChannelCache {
    public static final Map<String, ChannelId> CHANNEL_ID_MAP = new ConcurrentHashMap<>();
    public static final Map<String, List<SubscribeStore>> CLIENT_SUBSCRIBE_CACHE = new ConcurrentHashMap<>();
    public static final Map<String, RetainedMessage> RETAINED_MESSAGE_CACHE = new ConcurrentHashMap<>();
    public static final Map<String, SessionStore> SESSION_STORE_MAP = new ConcurrentHashMap<>();
    public static final Map<String, List<PublishMessageStore>> PUBLISH_MESSAGE_STORE_MAP = new ConcurrentHashMap<>();
    public static final Map<String, List<PubRelMessageStore>> PUB_REL_STORE_MAP = new ConcurrentHashMap<>();

    public static final Executor threadPool =
            new ThreadPoolExecutor(8,
                    16,
                    60,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(16),
                    Executors.defaultThreadFactory(),
                    new ThreadPoolExecutor.AbortPolicy());
}
