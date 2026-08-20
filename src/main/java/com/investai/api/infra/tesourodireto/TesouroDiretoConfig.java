package com.investai.api.infra.tesourodireto;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class TesouroDiretoConfig {

    @Bean
    public RestClient tesouroDiretoRestClient() {
        return RestClient.builder().build();
    }
}