package org.crudboy.miniapp.apigateway;

import joptsimple.internal.Strings;
import org.crudboy.miniapp.apigateway.rest.model.Address;
import org.crudboy.miniapp.apigateway.service.AppInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class GatewayConfig {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private String host;

    private int port;

    private int httpMaxContentLength;

    private List<AppInstance> appInstances;


    public GatewayConfig() {
        String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
        Properties appProps = new Properties();
        try {
            appProps.load(new FileInputStream(rootPath + "gateway-config.properties"));
        } catch (IOException e) {
            logger.error("load properties error", e);
        }
        this.host = appProps.getProperty("server.host");
        this.port = Integer.valueOf(appProps.getProperty("server.port"));
        this.httpMaxContentLength = Integer.valueOf(appProps.getProperty("server.http.maxcontentlength"));

        // init app instance list
        this.appInstances = new ArrayList<>();
        String[] apps = appProps.getProperty("apps").split(",");
        for (String appName : apps) {
            String serverList = appProps.getProperty(appName+".server");
            String schema = appProps.getProperty(appName+".schema");
            String serviceName = appProps.getProperty(appName+".servicename");
            if (!Strings.isNullOrEmpty(serverList)) {
                String[] serverListStrs = serverList.split(",");
                for (String serverStr : serverListStrs) {
                    String ip = serverStr.split(":")[0];
                    int port = Integer.valueOf(serverStr.split(":")[1]);
                    AppInstance appInstance = new AppInstance(appName, schema, new Address(ip, port), serviceName);
                    appInstances.add(appInstance);
                }
            }
        }
    }

    public int getHttpMaxContentLength() {
        return httpMaxContentLength;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public List<AppInstance> getAppInstances() {
        return appInstances;
    }
}
