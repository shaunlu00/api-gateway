package org.crudboy.miniapp.apigateway.worker.proxy;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;
import org.crudboy.miniapp.apigateway.rest.model.RequestInfo;
import org.crudboy.miniapp.apigateway.worker.event.ChannelClosedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

public class HTTPProxyHandler extends SimpleChannelInboundHandler<HttpObject> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private RequestInfo requestInfo;

    private Channel sourceChannel;

    private DelayOutboundHandler delayOutboundHandler;

    private volatile HttpRequest currentRequest;

    public HTTPProxyHandler(RequestInfo requestInfo, Channel sourceChannel){
        this.requestInfo = requestInfo;
        this.sourceChannel = sourceChannel;
        this.delayOutboundHandler = new DelayOutboundHandler();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        logger.info("channelActive from{}:{}", remoteAddress.getHostName(), remoteAddress.getPort());
        delayOutboundHandler.next();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        logger.info("channelInactive from{}:{}", remoteAddress.getHostName(), remoteAddress.getPort());

        delayOutboundHandler.release();
        sourceChannel.pipeline().fireUserEventTriggered(new ChannelClosedEvent(requestInfo));
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        ctx.pipeline()
                .addBefore(ctx.name(), null, new HttpClientCodec())
                .addBefore(ctx.name(), null, delayOutboundHandler);
    }



    protected void channelRead0(ChannelHandlerContext ctx, HttpObject httpObject) {
        logger.info("[Source ({})] <= [Target ({})] : {}", requestInfo.getSourceAddr(), requestInfo.getAppInstance().getServerAddr(), httpObject);
        sourceChannel.writeAndFlush(ReferenceCountUtil.retain(httpObject));
        if (httpObject instanceof HttpResponse) {
            currentRequest = null;
            delayOutboundHandler.next();
        }
    }


    private class DelayOutboundHandler extends ChannelOutboundHandlerAdapter {
        private Deque<RequestPromise> pendings = new ConcurrentLinkedDeque<>();
        private ChannelHandlerContext thisCtx;

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
            thisCtx = ctx.pipeline().context(this);
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            // add error handling
            promise.addListener(new ChannelFutureListener() {
                public void operationComplete(ChannelFuture future) {
                    if (!future.isSuccess()) {
                        logger.error("error {send request from proxy to target}", future.cause());
                        // close proxy to target channel
                        future.channel().close();
                        // close source to proxy channel
                        sourceChannel.close();
                    }
                }
            });

            if (msg instanceof FullHttpRequest) {
                logger.info("[Source ({})] => [Target ({})] : (PENDING) {}", requestInfo.getSourceAddr(), requestInfo.getAppInstance().getServerAddr(), msg);
                HttpRequest request = (HttpRequest) msg;
                pendings.offer(new RequestPromise(request, promise));
                next();
            } else if (msg instanceof HttpObject) {
                throw new IllegalStateException("Cannot handled message: " + msg.getClass());
            } else {
                ctx.write(msg, promise);
            }
        }

        private void next() {
            if (currentRequest != null || !thisCtx.channel().isActive() || pendings.isEmpty()) {
                return;
            }
            RequestPromise requestPromise = pendings.poll();
            currentRequest = requestPromise.request;
            logger.info("[Source ({})] => [Target ({})] : {}", requestInfo.getSourceAddr(), requestInfo.getAppInstance().getServerAddr(), requestPromise.request);
            thisCtx.writeAndFlush(requestPromise.request, requestPromise.promise);
        }

        private void release() {
            while (!pendings.isEmpty()) {
                RequestPromise requestPromise = pendings.poll();
                requestPromise.promise.setFailure(new IOException("Cannot send request to server"));
                ReferenceCountUtil.release(requestPromise.request);
            }
        }
    }

    private static class RequestPromise {
        private HttpRequest request;
        private ChannelPromise promise;

        private RequestPromise(HttpRequest request, ChannelPromise promise) {
            this.request = request;
            this.promise = promise;
        }
    }

}
