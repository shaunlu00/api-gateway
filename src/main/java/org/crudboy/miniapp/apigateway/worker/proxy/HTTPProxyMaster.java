package org.crudboy.miniapp.apigateway.worker.proxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import org.crudboy.miniapp.apigateway.rest.model.RequestInfo;

public class HTTPProxyMaster {

    private ChannelFuture connect(ChannelHandlerContext fromCtx, RequestInfo requestInfo, ChannelHandler handler) throws Exception {
        return new Bootstrap()
                .group(fromCtx.channel().eventLoop())
                .channel(fromCtx.channel().getClass())
                .handler(handler)
                .connect(requestInfo.getAppInstance().getServerAddr().getHost(),
                        requestInfo.getAppInstance().getServerAddr().getPort());
    }

    /**
     * Create proxy channel from source channel context
     * @param sourceChannelCtx
     * @param requestInfo
     * @return
     */
    public ChannelFuture createProxyChannel(final ChannelHandlerContext sourceChannelCtx, RequestInfo requestInfo) throws Exception {
        ChannelFuture future = connect(sourceChannelCtx, requestInfo, new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) {
                ch.pipeline().addLast(new HTTPProxyHandler(requestInfo, sourceChannelCtx.channel()));
            }
        });
        return future;
    }
}
