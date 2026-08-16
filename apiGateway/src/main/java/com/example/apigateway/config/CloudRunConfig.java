package com.example.apigateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class CloudRunConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(CloudRunConfig.class);
    
    @Value("${server.port}")
    private String serverPort;
    
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        String port = System.getenv("PORT");
        if (port == null) {
            port = "8080";
        }
        
        logger.info("==============================================");
        logger.info("API Gateway started successfully!");
        logger.info("Server port from config: {}", serverPort);
        logger.info("PORT environment variable: {}", System.getenv("PORT"));
        logger.info("Server binding to: 0.0.0.0:{}", port);
        logger.info("Health check available at: http://0.0.0.0:{}/health", port);
        logger.info("==============================================");
    }
}