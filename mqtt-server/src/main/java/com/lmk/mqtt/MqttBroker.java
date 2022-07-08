package com.lmk.mqtt;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;


/**
 * @author lmk
 * @version 1.0.0
 * @ClassName MqttBroker.java
 * @Description TODO
 * @createTime 2022-06-18 13:27:11
 */
@Component
public class MqttBroker {

    @Autowired
    private ChannelGroup channelGroup;

    @Autowired
    private NioEventLoopGroup bossGroup;

    @Autowired
    private NioEventLoopGroup workGroup;

    @Autowired
    private MqttChannelInboundHandler mqttChannelInboundHandler;

    private final Logger logger = LoggerFactory.getLogger(MqttBroker.class);


    /**
     * 服务启动
     *
     * @author lmk
     * @Date 2022/6/23 14:50
     */
    @PostConstruct
    public void startUp() {
//        channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        ServerBootstrap bootstrap = new ServerBootstrap();
//        bossGroup = new NioEventLoopGroup(1);
//        workGroup = new NioEventLoopGroup();

        bootstrap.group(bossGroup, workGroup);

        bootstrap.channel(NioServerSocketChannel.class);


        bootstrap.option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.SO_RCVBUF, 10485760);
        bootstrap.childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ChannelPipeline channelPipeline = ch.pipeline();
                // 设置读写空闲超时时间
                channelPipeline.addLast(new IdleStateHandler(600, 600, 1200));
                channelPipeline.addLast("encoder", MqttEncoder.INSTANCE);
                channelPipeline.addLast("decoder", new MqttDecoder());
//                channelPipeline.addLast(new MqttChannelInboundHandler(channelGroup));
                channelPipeline.addLast(mqttChannelInboundHandler);
            }
        });

        try {
            ChannelFuture f = bootstrap.bind(1883).sync();
            f.channel().closeFuture().sync();
            logger.info("mqtt服务器启动成功");
        } catch (InterruptedException e) {
            e.printStackTrace();
            logger.error("mqtt服务器启动失败");
        }
    }

    public void shutDown(){
        if (workGroup!=null&&bossGroup!=null){
            workGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            logger.info("mqtt服务器关闭");
        }
    }
}
