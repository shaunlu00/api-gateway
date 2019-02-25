package org.crudboy.miniapp.apigateway.service.route;

import org.crudboy.miniapp.apigateway.error.ServiceNotAvailableException;
import org.crudboy.miniapp.apigateway.service.AppInstance;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinServiceRouter implements ServiceRouter {

    private final AtomicInteger p = new AtomicInteger(0);

    @Override
    public AppInstance route(final List<AppInstance> appInstanceList) {
        int m = appInstanceList.size();
        AppInstance appInstance = null;
        for (int n = 0; n < appInstanceList.size(); n++) {
            appInstance = appInstanceList.get(p.getAndIncrement() % m);
            if (null != appInstance) break;
        }
        return appInstance;
    }
}
