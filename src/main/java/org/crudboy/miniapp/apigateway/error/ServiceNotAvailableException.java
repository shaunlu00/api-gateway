package org.crudboy.miniapp.apigateway.error;

public class ServiceNotAvailableException extends RuntimeException {

    public ServiceNotAvailableException(String msg) {
        super(msg);
    }

}
