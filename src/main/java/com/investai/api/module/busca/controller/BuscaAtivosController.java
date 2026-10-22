package com.investai.api.module.busca.controller;

import com.investai.api.module.busca.dto.BuscaAtivosRequestDTO;
import com.investai.api.module.busca.dto.ResultadoBuscaDTO;
import com.investai.api.module.busca.service.BuscaAtivosService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/busca")
@RequiredArgsConstructor
public class BuscaAtivosController {

    private final BuscaAtivosService buscaAtivosService;

    @GetMapping
    public ResponseEntity<List<ResultadoBuscaDTO>> buscar(@Valid BuscaAtivosRequestDTO request) {
        return ResponseEntity.ok(buscaAtivosService.buscar(request.getTermo(), request.getLimite()));
    }
}
