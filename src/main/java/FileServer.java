/**
 * fshows.com
 * Copyright (C) 2020-2021 All Rights Reserved.
 */
import constant.FtpConstant;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.io.File;
import java.net.InetSocketAddress;

/**
 * @author yangj
 * @version FileServer.java, v 0.1 2021-01-05 2:35 下午 yangj
 */
public class FileServer {
    private final int port;
    public FileServer(int port) {
        this.port = port;
    }

    public void start() throws InterruptedException {
        File file = new File(FtpConstant.FILE_PATH);
        if(!file .exists()) {
            file.mkdirs();
        }

        EventLoopGroup group = new NioEventLoopGroup();
        Pipeline pipeline = new Pipeline();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(group)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(port))
                    .childHandler(pipeline);
            ChannelFuture f = b.bind().sync();
            f.channel().closeFuture().sync();
        } finally {
            // 关闭线程组
            group.shutdownGracefully().sync();
        }

    }
}
