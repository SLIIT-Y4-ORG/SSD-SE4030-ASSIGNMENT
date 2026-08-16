package com.example.apigateway.config;

import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
public class WebServerConfig implements WebServerFactoryCustomizer<NettyReactiveWebServerFactory> {

    @Override
    public void customize(NettyReactiveWebServerFactory factory) {
        try {
            // Ensure server binds to all interfaces
            factory.setAddress(InetAddress.getByName("0.0.0.0"));
            
            // Set port from environment variable or default to 8080
            String port = System.getenv("PORT");
            if (port != null && !port.isEmpty()) {
                factory.setPort(Integer.parseInt(port));
            }
        } catch (UnknownHostException e) {
            throw new RuntimeException("Failed to set server address", e);
        }
    }
}