package com.investai.api.module.rendafixa.controller;

import com.investai.api.module.rendafixa.dto.*;
import com.investai.api.module.rendafixa.service.TituloPrivadoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/renda-fixa/titulos")
@RequiredArgsConstructor
public class TituloPrivadoController {

    private final TituloPrivadoService tituloPrivadoService;

    @PostMapping
    public ResponseEntity<TituloPrivadoResponseDTO> cadastrar(@Valid @RequestBody CadastroTituloPrivadoRequestDTO dto) {
        TituloPrivadoResponseDTO response = tituloPrivadoService.cadastrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TituloPrivadoResponseDTO> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody AtualizarTituloPrivadoRequestDTO dto
    ) {
        return ResponseEntity.ok(tituloPrivadoService.atualizar(id, dto));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TituloPrivadoResponseDTO> alterarStatus(
            @PathVariable UUID id,
            @Valid @RequestBody AlterarStatusTituloPrivadoRequestDTO dto
    ) {
        return ResponseEntity.ok(tituloPrivadoService.alterarStatus(id, dto.getAtivo()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable UUID id) {
        tituloPrivadoService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}