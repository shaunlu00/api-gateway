package org.crudboy.miniapp.apigateway.rest.model;

import com.google.common.base.Strings;
import org.crudboy.miniapp.apigateway.service.AppInstance;

public class RequestInfo {

    private Address sourceAddr;

    private AppInstance appInstance;

    private String requestPath;

    public RequestInfo(Address sourceAddr, AppInstance appInstance, String requestPath) {
        this.sourceAddr = sourceAddr;
        this.appInstance = appInstance;
        this.requestPath = requestPath;
    }

    public Address getSourceAddr() {
        return sourceAddr;
    }

    public void setSourceAddr(Address sourceAddr) {
        this.sourceAddr = sourceAddr;
    }


    @Override
    public String toString() {
        return Strings.lenientFormat("RequestInfo {from=%s:%s, to=%s://%s:%s%s}",
                sourceAddr.getHost(),
                sourceAddr.getPort(),
                appInstance.getSchema(),
                appInstance.getServerAddr().getHost(),
                appInstance.getServerAddr().getPort(),
                requestPath);
    }

    public AppInstance getAppInstance() {
        return appInstance;
    }

    public void setAppInstance(AppInstance appInstance) {
        this.appInstance = appInstance;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }
}
