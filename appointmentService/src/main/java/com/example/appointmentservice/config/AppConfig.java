package com.example.appointmentservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
        // // Configure timeouts for inter-service calls
        // RequestConfig requestConfig = RequestConfig.custom()
        //     .setConnectTimeout(5_000)
        //     .setConnectionRequestTimeout(5_000)
        //     .setSocketTimeout(5_000)
        //     .build();

        // CloseableHttpClient httpClient = HttpClientBuilder.create()
        //     .setDefaultRequestConfig(requestConfig)
        //     .build();

        // HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        // return new RestTemplate(factory);
    }
}