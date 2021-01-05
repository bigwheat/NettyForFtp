/**
 * fshows.com
 * Copyright (C) 2020-2021 All Rights Reserved.
 */

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;

/**
 * @author yangj
 * @version Pipeline.java, v 0.1 2021-01-05 2:36 下午 yangj
 */
public class Pipeline extends ChannelInitializer<SocketChannel> {

    private EventExecutorGroup businessEventExecutorGroup = new DefaultEventExecutorGroup(10);
    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        /**
         * http服务器端对response编码
         */
        pipeline.addLast("encoder", new HttpResponseEncoder());
        /**
         * http服务器端对request解码3.
         */
        pipeline.addLast("decoder", new HttpRequestDecoder());
        pipeline.addLast("aggregator", new HttpObjectAggregator(655300000));
        pipeline.addLast(businessEventExecutorGroup, new FileServerHandle());
    }
}
