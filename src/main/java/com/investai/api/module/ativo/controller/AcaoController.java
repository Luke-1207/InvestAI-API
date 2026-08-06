package com.investai.api.module.ativo.controller;

import com.investai.api.module.ativo.dto.AcaoResponseDTO;
import com.investai.api.module.ativo.dto.AtualizarAcaoRequestDTO;
import com.investai.api.module.ativo.dto.CadastroAcaoRequestDTO;
import com.investai.api.module.ativo.service.AcaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/acoes")
@RequiredArgsConstructor
public class AcaoController {

    private final AcaoService acaoService;

    @PostMapping
    public ResponseEntity<AcaoResponseDTO> cadastrar(
            @Valid @RequestBody CadastroAcaoRequestDTO dto
    ) {
        AcaoResponseDTO response = acaoService.cadastrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AcaoResponseDTO> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody AtualizarAcaoRequestDTO dto
    ) {
        return ResponseEntity.ok(acaoService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable UUID id) {
        acaoService.desativar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AcaoResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(acaoService.buscarPorId(id));
    }
}