package org.crudboy.miniapp.apigateway.test;

import org.mockserver.integration.ClientAndServer;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class MockServer implements Runnable{

    ClientAndServer mockServer1;
    ClientAndServer mockServer2;

    @Override
    public void run() {
        mockServer1 = ClientAndServer.startClientAndServer(9001);
        mockServer2 = ClientAndServer.startClientAndServer(9002);
        mockServer1.when(request().withPath("/app1/getUserById")).respond(response().withBody("User John from mock server 1"));
        mockServer2.when(request().withPath("/app1/getUserById")).respond(response().withBody("User John from mock server 2"));
    }
}
