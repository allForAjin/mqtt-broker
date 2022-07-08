package com.lmk.mqtt.config;

import com.lmk.mqtt.MqttChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BrokerConfig {
    @Bean(name = "bossGroup")
    public NioEventLoopGroup bossGroup(){
        return new NioEventLoopGroup(1);
    }

    @Bean(name = "workGroup")
    public NioEventLoopGroup workGroup(){
        return new NioEventLoopGroup();
    }

    @Bean(name = "channelGroup")
    public ChannelGroup channelGroup(){
        return new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    }

    @Bean(name = "mqttChannelInboundHandler")
    public MqttChannelInboundHandler mqttChannelInboundHandler(){
        return new MqttChannelInboundHandler(channelGroup());
    }
}
