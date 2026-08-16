package com.example.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        // Ensure the application binds to all interfaces for Cloud Run
        System.setProperty("server.address", "0.0.0.0");
        
        SpringApplication app = new SpringApplication(ApiGatewayApplication.class);
        app.run(args);
    }
}