package com.investai.api.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String RANKING_REQUEST  = "ia.ranking.request";
    public static final String RANKING_RESPONSE = "ia.ranking.response";
    public static final String RESUMO_REQUEST   = "ia.resumo.request";
    public static final String RESUMO_RESPONSE  = "ia.resumo.response";
    public static final String COMPARACAO_REQUEST  = "ia.comparacao.request";
    public static final String COMPARACAO_RESPONSE = "ia.comparacao.response";

    @Bean
    public Queue rankingRequestQueue() {
        return new Queue(RANKING_REQUEST, true);
    }

    @Bean
    public Queue rankingResponseQueue() {
        return new Queue(RANKING_RESPONSE, true);
    }

    @Bean
    public Queue resumoRequestQueue() {
        return new Queue(RESUMO_REQUEST, true);
    }

    @Bean
    public Queue resumoResponseQueue() {
        return new Queue(RESUMO_RESPONSE, true);
    }

    @Bean
    public Queue comparacaoRequestQueue() {
        return new Queue(COMPARACAO_REQUEST, true);
    }

    @Bean
    public Queue comparacaoResponseQueue() {
        return new Queue(COMPARACAO_RESPONSE, true);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}