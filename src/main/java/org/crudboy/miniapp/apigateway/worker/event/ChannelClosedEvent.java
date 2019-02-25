package org.crudboy.miniapp.apigateway.worker.event;

import org.crudboy.miniapp.apigateway.rest.model.RequestInfo;

public class ChannelClosedEvent {

    private RequestInfo requestInfo;

    public ChannelClosedEvent(RequestInfo requestInfo) {
        this.requestInfo = requestInfo;
    }

    public RequestInfo getRequestInfo() {
        return requestInfo;
    }
}
