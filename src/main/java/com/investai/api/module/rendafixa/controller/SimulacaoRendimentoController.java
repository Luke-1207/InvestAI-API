package com.investai.api.module.rendafixa.controller;

import com.investai.api.module.rendafixa.dto.SimulacaoRendimentoRequestDTO;
import com.investai.api.module.rendafixa.dto.SimulacaoRendimentoResponseDTO;
import com.investai.api.module.rendafixa.service.SimuladorRendimentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/renda-fixa")
@RequiredArgsConstructor
public class SimulacaoRendimentoController {

    private final SimuladorRendimentoService simuladorRendimentoService;

    @GetMapping("/titulos/{id}/simulacao")
    public ResponseEntity<SimulacaoRendimentoResponseDTO> simularTituloPrivado(
            @PathVariable UUID id,
            @Valid SimulacaoRendimentoRequestDTO request
    ) {
        return ResponseEntity.ok(simuladorRendimentoService.simularTituloPrivado(id, request));
    }

    @GetMapping("/tesouro/{codigo}/simulacao")
    public ResponseEntity<SimulacaoRendimentoResponseDTO> simularTesouro(
            @PathVariable String codigo,
            @Valid SimulacaoRendimentoRequestDTO request
    ) {
        return ResponseEntity.ok(simuladorRendimentoService.simularTesouro(codigo, request));
    }
}
