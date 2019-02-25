package org.crudboy.miniapp.apigateway.service;

import org.crudboy.miniapp.apigateway.error.ServiceNotAvailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceRepository {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private Map<String, List<AppInstance>> appMap;

    public ServiceRepository() {
        appMap = new HashMap<>();
    }

    public void registryService(List<AppInstance> appInstances) {
        for (AppInstance appInstance : appInstances) {
            String appName = appInstance.getAppName();
            appMap.putIfAbsent(appName, new ArrayList<>());
            appMap.get(appName).add(appInstance);
        }
    }

    public List<AppInstance> getServices(String appName) {
        List<AppInstance> services = appMap.get(appName);
        if (null == services || 0 == services.size()) {
            logger.error("no app instance found for " + appName);
            throw new ServiceNotAvailableException("no app instance found for " + appName);
        }
        return services;
    }


}
