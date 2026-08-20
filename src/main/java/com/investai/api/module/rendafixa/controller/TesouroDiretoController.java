package com.investai.api.module.rendafixa.controller;

import com.investai.api.module.rendafixa.dto.TituloTesouroDetalheResponseDTO;
import com.investai.api.module.rendafixa.dto.TituloTesouroListagemFiltroDTO;
import com.investai.api.module.rendafixa.dto.TituloTesouroListagemResponseDTO;
import com.investai.api.module.rendafixa.service.TituloTesouroDetalheService;
import com.investai.api.module.rendafixa.service.TituloTesouroListagemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/renda-fixa/tesouro")
@RequiredArgsConstructor
public class TesouroDiretoController {

    private final TituloTesouroListagemService tituloTesouroListagemService;
    private final TituloTesouroDetalheService tituloTesouroDetalheService;

    @GetMapping
    public ResponseEntity<Page<TituloTesouroListagemResponseDTO>> listar(TituloTesouroListagemFiltroDTO filtro) {
        return ResponseEntity.ok(tituloTesouroListagemService.listar(filtro));
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<TituloTesouroDetalheResponseDTO> obterDetalhe(@PathVariable String codigo) {
        return ResponseEntity.ok(tituloTesouroDetalheService.obterDetalhe(codigo));
    }
}