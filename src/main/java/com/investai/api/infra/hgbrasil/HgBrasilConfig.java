package com.investai.api.infra.hgbrasil;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class HgBrasilConfig {

    @Bean
    public RestClient hgBrasilRestClient() {
        return RestClient.builder().build();
    }
}