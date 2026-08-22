package com.investai.api.infra.bcb;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class BcbConfig {

    @Bean
    public RestClient bcbRestClient() {
        return RestClient.builder().build();
    }
}