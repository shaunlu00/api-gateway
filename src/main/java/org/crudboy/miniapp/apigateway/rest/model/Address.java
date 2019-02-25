package org.crudboy.miniapp.apigateway.rest.model;

public class Address {

    private String host;

    private int port;

    public Address(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return host + ":" + String.valueOf(port);
    }
}
