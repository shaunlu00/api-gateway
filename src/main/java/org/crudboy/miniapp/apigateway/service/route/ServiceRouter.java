package org.crudboy.miniapp.apigateway.service.route;

import org.crudboy.miniapp.apigateway.service.AppInstance;

import java.util.List;


public interface ServiceRouter {

    AppInstance route(List<AppInstance> appInstanceList);
}
