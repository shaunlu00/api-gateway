package org.crudboy.miniapp.apigateway.service;


import joptsimple.internal.Strings;
import org.crudboy.miniapp.apigateway.error.HTTPRequestException;
import org.crudboy.miniapp.apigateway.error.ServiceNotAvailableException;
import org.crudboy.miniapp.apigateway.rest.model.Address;
import org.crudboy.miniapp.apigateway.rest.model.RequestInfo;
import org.crudboy.miniapp.apigateway.service.route.RoundRobinServiceRouter;
import org.crudboy.miniapp.apigateway.service.route.ServiceRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AppServiceManager {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private ServiceRouter serviceRouter;

    private ServiceRepository serviceRepository;

    public AppServiceManager() {
        this.serviceRepository = new ServiceRepository();
        this.serviceRouter = new RoundRobinServiceRouter();
    }

    public void initAppServiceRepo(List<AppInstance> appInstances) {
        serviceRepository.registryService(appInstances);
    }

    public AppInstance route(String appName) {
        AppInstance appInstance = serviceRouter.route(serviceRepository.getServices(appName));
        if (null == appInstance) {
            logger.error("no available app instance found " + appName);
            throw new ServiceNotAvailableException("no available app instance found " + appName);
        }
        return appInstance;
    }

    public RequestInfo resolveRequestInfo(Address sourceAddr, String requestURI) {
        if (requestURI.startsWith("/") && requestURI.length() > 1) {
            String[] pathStrs = requestURI.split("/");
            String appName = null;
            String requestPath = "/";
            for (String str : pathStrs) {
                if (!Strings.isNullOrEmpty(str)) {
                    if (Strings.isNullOrEmpty(appName)) {
                        appName = str;
                    } else {
                        requestPath = requestPath + str + "/";
                    }
                }
            }
            while (requestPath.endsWith("/")) {
                requestPath = requestPath.substring(0, requestPath.length() - 1);
            }

            AppInstance appInstance = route(appName);
            return new RequestInfo(sourceAddr, appInstance, requestPath);
        } else {
            logger.error("can not find app name from request path [{}]", requestURI);
            throw new HTTPRequestException("can not find app name from request path");
        }
    }

}
