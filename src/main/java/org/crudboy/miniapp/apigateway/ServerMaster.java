package org.crudboy.miniapp.apigateway;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.crudboy.miniapp.apigateway.rest.model.Address;
import org.crudboy.miniapp.apigateway.service.AppServiceManager;
import org.crudboy.miniapp.apigateway.test.MockServer;
import org.crudboy.miniapp.apigateway.worker.proxy.HTTPProxyMaster;
import org.crudboy.miniapp.apigateway.worker.upstream.HTTPUPStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;


public class ServerMaster {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private GatewayConfig config;
    private AppServiceManager appServiceManager;
    private HTTPProxyMaster proxyMaster;

    public ServerMaster() {
        config = new GatewayConfig();
        appServiceManager = new AppServiceManager();
        appServiceManager.initAppServiceRepo(config.getAppInstances());
        proxyMaster = new HTTPProxyMaster();
    }

    public void start() {
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.DEBUG))
                    .childHandler(new SimpleChannelInitializer(config, appServiceManager, proxyMaster));
            Channel channel = bootstrap
                    .bind(config.getHost(), config.getPort())
                    .sync()
                    .channel();

            System.out.printf("gateway is listened at http://%s:%d\n", config.getHost(), config.getPort());

            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            logger.error("application error", e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private static class SimpleChannelInitializer extends ChannelInitializer<Channel> {

        private final Logger logger = LoggerFactory.getLogger(SimpleChannelInitializer.class);

        private GatewayConfig config;
        private AppServiceManager appServiceManager;
        private HTTPProxyMaster proxyMaster;

        public SimpleChannelInitializer(GatewayConfig config, AppServiceManager appServiceManager, HTTPProxyMaster proxyMaster) {
            this.config = config;
            this.appServiceManager = appServiceManager;
            this.proxyMaster = proxyMaster;
        }

        @Override
        protected void initChannel(Channel channel) {
            InetSocketAddress address = (InetSocketAddress) channel.remoteAddress();
            Address sourceAddress = new Address(address.getHostName(), address.getPort());
            channel.pipeline().addLast(
                    new HTTPUPStreamHandler(config, appServiceManager, proxyMaster),
                    new SimpleChannelInboundHandler<Object>() {
                        @Override
                        protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o)
                                throws Exception {
                            logger.info("[Client ({})] => Unhandled inbound: {}", sourceAddress, o);
                        }
                    });
        }
    }

    public static void main(String args[]) {
        // start mock server for test
        new Thread(new MockServer()).start();
        // start api gateway
        new ServerMaster().start();
    }
}
