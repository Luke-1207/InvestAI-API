package com.investai.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class DashboardExecutorConfig {

    @Bean("dashboardExecutor")
    public Executor dashboardExecutor() {
        return Executors.newFixedThreadPool(4);
    }
}