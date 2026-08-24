package com.investai.api.infra.ia;

import com.investai.api.infra.ia.dto.IaHealthResponseDTO;
import com.investai.api.infra.ia.dto.IaHealthStatusDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class IaHealthClientImpl implements IaHealthClient {

    private final RestClient iaHealthRestClient;

    @Value("${ia.health.base-url}")
    private String baseUrl;

    @Override
    public IaHealthStatusDTO verificarStatus() {
        try {
            IaHealthResponseDTO response = iaHealthRestClient.get()
                    .uri(baseUrl + "/health")
                    .retrieve()
                    .body(IaHealthResponseDTO.class);

            boolean disponivel = response != null && "ok".equalsIgnoreCase(response.getStatus());

            return IaHealthStatusDTO.builder()
                    .disponivel(disponivel)
                    .rabbitmqConectado(response != null ? response.getRabbitmq() : null)
                    .build();

        } catch (Exception e) {
            log.warn("Falha ao consultar /health do microsserviço IA: {}", e.getMessage());
            return IaHealthStatusDTO.builder().disponivel(false).rabbitmqConectado(null).build();
        }
    }
}