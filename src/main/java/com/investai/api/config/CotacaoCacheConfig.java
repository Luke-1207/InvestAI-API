package com.investai.api.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.investai.api.module.ativo.dto.CotacaoResponseDTO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CotacaoCacheConfig {

    @Bean
    public Cache<String, CotacaoResponseDTO> cotacaoCaffeineCache() {
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(20, TimeUnit.MINUTES)
                .build();
    }
}