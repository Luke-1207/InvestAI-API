package com.investai.api.module.dashboard.controller;

import com.investai.api.module.dashboard.dto.AtualizarSelicIpcaRequestDTO;
import com.investai.api.module.dashboard.dto.IndicadoresMercadoResponseDTO;
import com.investai.api.module.dashboard.service.IndicadoresMercadoSincronizacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/dashboard/indicadores")
@RequiredArgsConstructor
public class IndicadoresMercadoController {

    private final IndicadoresMercadoSincronizacaoService indicadoresMercadoSincronizacaoService;

    @PatchMapping("/selic-ipca")
    public ResponseEntity<IndicadoresMercadoResponseDTO> atualizarSelicIpcaManualmente(
            @Valid @RequestBody AtualizarSelicIpcaRequestDTO dto
    ) {
        return ResponseEntity.ok(indicadoresMercadoSincronizacaoService.atualizarSelicIpcaManualmente(
                dto.getSelicAtual(), dto.getIpcaAcumulado12m()));
    }
}