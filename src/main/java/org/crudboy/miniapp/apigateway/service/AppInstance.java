package org.crudboy.miniapp.apigateway.service;

import org.crudboy.miniapp.apigateway.rest.model.Address;

public class AppInstance {

    public AppInstance(String appName, String schema, Address serverAddr, String serviceName) {
        this.appName = appName;
        this.schema = schema;
        this.serverAddr = serverAddr;
        this.serviceName = serviceName;
        this.status = Status.ACTIVE;
    }

    private String appName;

    private String instanceId;

    private String schema;

    private Address serverAddr;

    private String serviceName;

    private Status status;

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public Address getServerAddr() {
        return serverAddr;
    }

    public void setServerAddr(Address serverAddr) {
        this.serverAddr = serverAddr;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getSchema() {
        return schema;
    }

    enum Status {
        ACTIVE,
        INACTIVE
    }
}
