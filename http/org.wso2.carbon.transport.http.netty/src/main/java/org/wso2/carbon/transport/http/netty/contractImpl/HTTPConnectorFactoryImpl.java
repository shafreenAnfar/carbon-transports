package org.wso2.carbon.transport.http.netty.contractImpl;

import org.wso2.carbon.transport.http.netty.config.TransportProperty;
import org.wso2.carbon.transport.http.netty.contract.HTTPClientConnector;
import org.wso2.carbon.transport.http.netty.contract.HTTPConnectorFactory;
import org.wso2.carbon.transport.http.netty.contract.ServerConnector;
import org.wso2.carbon.transport.http.netty.common.Constants;
import org.wso2.carbon.transport.http.netty.config.ConfigurationBuilder;
import org.wso2.carbon.transport.http.netty.config.ListenerConfiguration;
import org.wso2.carbon.transport.http.netty.config.TransportsConfiguration;
import org.wso2.carbon.transport.http.netty.listener.ServerBootstrapConfiguration;
import org.wso2.carbon.transport.http.netty.listener.ServerConnectorBootstrap;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of HTTPConnectorFactory interface
 */
public class HTTPConnectorFactoryImpl implements HTTPConnectorFactory {

    private static final String NETTY_TRANSPORT_CONF = "transports.netty.conf";

    @Override
    public ServerConnector getServerConnector(Map<String, String> connectorConfig) {

        String nettyConfigFile = System.getProperty(NETTY_TRANSPORT_CONF,
                "conf" + File.separator + "transports" + File.separator + "netty-transports.yml");
        nettyConfigFile = "/home/shaf/Documents/source/public/ballerina/tools-distribution"
                + "/modules/ballerina/conf/netty-transports.yml";

        TransportsConfiguration trpConfig = ConfigurationBuilder.getInstance().getConfiguration(nettyConfigFile);
        ListenerConfiguration listenerConfig = buildListenerConfig("serviceName", connectorConfig);
        ServerBootstrapConfiguration serverBootstrapConfiguration = getServerBootstrapConfiguration(
                trpConfig.getTransportProperties());

        ServerConnectorBootstrap serverConnectorBootstrap = new ServerConnectorBootstrap(listenerConfig);
        serverConnectorBootstrap.addSocketConfiguration(serverBootstrapConfiguration);
        serverConnectorBootstrap.addSecurity(listenerConfig);
        serverConnectorBootstrap.addIdleTimeout(listenerConfig);
        serverConnectorBootstrap.addThreadPools(Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().availableProcessors() * 2);

        return serverConnectorBootstrap.getServerConnector(listenerConfig);
    }

    @Override
    public HTTPClientConnector getClientConnector(Map<String, String> connectorConfig) {
        return null;
    }

    private ListenerConfiguration buildListenerConfig(String id, Map<String, String> properties) {
        String host = properties.get(Constants.HTTP_HOST) != null ?
                properties.get(Constants.HTTP_HOST) : Constants.HTTP_DEFAULT_HOST;
        int port = Integer.parseInt(properties.get(Constants.HTTP_PORT));

        ListenerConfiguration config = new ListenerConfiguration(id, host, port);
        String schema = properties.get(Constants.HTTP_SCHEME);
        if (schema != null && schema.equals("https")) {
            config.setScheme(schema);
            config.setKeyStoreFile(properties.get(Constants.HTTP_KEY_STORE_FILE));
            config.setKeyStorePass(properties.get(Constants.HTTP_KEY_STORE_PASS));
            config.setCertPass(properties.get(Constants.HTTP_CERT_PASS));
            //todo fill truststore stuff
        }
        return config;
    }

    private ServerBootstrapConfiguration getServerBootstrapConfiguration(Set<TransportProperty>
            transportPropertiesSet) {
        Map<String, Object> transportProperties = new HashMap<>();

        if (transportPropertiesSet != null && !transportPropertiesSet.isEmpty()) {
            transportProperties = transportPropertiesSet.stream().collect(
                    Collectors.toMap(TransportProperty::getName, TransportProperty::getValue));
        }
        // Create Bootstrap Configuration from listener parameters
        ServerBootstrapConfiguration.createBootStrapConfiguration(transportProperties);
        return ServerBootstrapConfiguration.getInstance();
    }
}