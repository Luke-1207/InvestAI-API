package com.investai.api.infra.ia;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class IaHealthConfig {

    @Bean
    public RestClient iaHealthRestClient() {
        return RestClient.builder().build();
    }
}