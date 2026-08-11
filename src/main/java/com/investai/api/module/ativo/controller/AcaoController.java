package com.investai.api.module.ativo.controller;

import com.investai.api.module.ativo.dto.*;
import com.investai.api.module.ativo.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/acoes")
@RequiredArgsConstructor
public class AcaoController {

    private final AcaoService acaoService;
    private final CotacaoService cotacaoService;
    private final AcaoListagemService acaoListagemService;
    private final ComparacaoService comparacaoService;
    private final HistoricoService historicoService;

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

    @GetMapping("/{codigo}/cotacao")
    public ResponseEntity<CotacaoResponseDTO> obterCotacao(@PathVariable String codigo) {
        return ResponseEntity.ok(cotacaoService.obterCotacao(codigo));
    }

    @GetMapping
    public ResponseEntity<Page<AcaoListagemResponseDTO>> listar(
            @ModelAttribute AcaoListagemFiltroDTO filtro
    ) {
        return ResponseEntity.ok(acaoListagemService.listar(filtro));
    }

    @GetMapping("/comparar")
    public ResponseEntity<ComparacaoResponseDTO> comparar(
            @RequestParam List<String> codigos
    ) {
        return ResponseEntity.ok(comparacaoService.comparar(codigos));
    }

    @GetMapping("/{codigo}/historico")
    public ResponseEntity<HistoricoPrecoResponseDTO> obterHistorico(
            @PathVariable String codigo,
            @RequestParam(defaultValue = "1M") String periodo
    ) {
        return ResponseEntity.ok(historicoService.obterHistorico(codigo, periodo));
    }
}