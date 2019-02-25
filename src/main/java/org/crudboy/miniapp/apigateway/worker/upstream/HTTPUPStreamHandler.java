package org.crudboy.miniapp.apigateway.worker.upstream;

import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import joptsimple.internal.Strings;
import org.crudboy.miniapp.apigateway.error.HTTPRequestException;
import org.crudboy.miniapp.apigateway.rest.model.Address;
import org.crudboy.miniapp.apigateway.GatewayConfig;
import org.crudboy.miniapp.apigateway.service.AppInstance;
import org.crudboy.miniapp.apigateway.rest.model.RequestInfo;
import org.crudboy.miniapp.apigateway.service.AppServiceManager;
import org.crudboy.miniapp.apigateway.worker.event.ChannelClosedEvent;
import org.crudboy.miniapp.apigateway.worker.proxy.HTTPProxyMaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class HTTPUPStreamHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private RequestInfo requestInfo;

    private GatewayConfig config;
    private AppServiceManager serviceManager;
    private HTTPProxyMaster proxyMaster;


    // The proxy channel which forwards request
    private Channel proxyChannel;
    // Http request decoder
    private ChannelHandler httpServerCodec;
    // Http request aggregator
    private ChannelHandler httpObjectAggregator;


    public HTTPUPStreamHandler(GatewayConfig config, AppServiceManager serviceManager, HTTPProxyMaster proxyMaster) {
        this.config = config;
        this.serviceManager = serviceManager;
        this.proxyMaster = proxyMaster;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        logger.info("upstream handler added,from{}:{}", remoteAddress.getHostName(), remoteAddress.getPort());

        // A combination of HttpRequestDecoder and HttpResponseEncoder
        httpServerCodec = new HttpServerCodec();
        // Aggregates HTTPMessage and HTTPContent into HTTPRequest or HTTPResponse
        httpObjectAggregator = new HttpObjectAggregator(config.getHttpMaxContentLength());
        ctx.pipeline()
                .addBefore(ctx.name(), null, httpServerCodec)
                .addBefore(ctx.name(), null, httpObjectAggregator);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        logger.info("upstream handler removed,from{}:{}", remoteAddress.getHostName(), remoteAddress.getPort());

        ctx.pipeline().remove(httpServerCodec).remove(httpObjectAggregator);
        if (null != proxyChannel) {
            proxyChannel.close();
            proxyChannel = null;
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof ChannelClosedEvent) {
            ChannelClosedEvent event = (ChannelClosedEvent) evt;
            if (requestInfo.equals(event.getRequestInfo())) {
                ctx.close();
            } else {
                proxyChannel = null;
            }
        }
        ctx.fireUserEventTriggered(evt);
    }

    /**
     * Handle Http request
     *
     * @param ctx
     * @param request
     * @throws Exception
     */
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        Address sourceAddr = new Address(remoteAddress.getHostName(), remoteAddress.getPort());
        this.requestInfo = serviceManager.resolveRequestInfo(sourceAddr, request.uri());

        handleProxyConnection(ctx, request);
    }


    private void handleProxyConnection(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {

        if (null != proxyChannel && !proxyChannel.isActive()) {
            proxyChannel.close();
            proxyChannel = null;
        }

        if (null == proxyChannel) {
            proxyChannel = proxyMaster.createProxyChannel(ctx, requestInfo).channel();
        }

        FullHttpRequest newRequest = request.copy();
        newRequest.headers().set("Host", requestInfo.getAppInstance().getServerAddr().toString());
        newRequest.setUri("/" + requestInfo.getAppInstance().getServiceName() + requestInfo.getRequestPath());


        logger.info("[Source ({})] => [Target ({})] : {}", requestInfo.getSourceAddr(), requestInfo.getAppInstance().getServerAddr(), newRequest);
        proxyChannel.writeAndFlush(newRequest);
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("upstream handler error", cause);
        ctx.close();
    }
}
