package org.crudboy.miniapp.apigateway.error;

public class HTTPRequestException extends RuntimeException {

    public HTTPRequestException(String msg) {
        super(msg);
    }

}
