package com.investai.api.module.rendafixa.controller;

import com.investai.api.module.rendafixa.dto.RendaFixaListagemResponseDTO;
import com.investai.api.module.rendafixa.service.RendaFixaUnificadaService;
import com.investai.api.shared.security.UsuarioAutenticadoHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/renda-fixa")
@RequiredArgsConstructor
public class RendaFixaUnificadaController {

    private final RendaFixaUnificadaService rendaFixaUnificadaService;
    private final UsuarioAutenticadoHelper usuarioAutenticadoHelper;

    @GetMapping
    public ResponseEntity<List<RendaFixaListagemResponseDTO>> listar(
            @RequestParam(required = false, defaultValue = "livre") String modo
    ) {
        UUID usuarioId = usuarioAutenticadoHelper.getIdUsuarioLogado();
        return ResponseEntity.ok(rendaFixaUnificadaService.listar(modo, usuarioId));
    }
}