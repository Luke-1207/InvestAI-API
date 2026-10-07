package com.investai.api.module.comparacao.controller;

import com.investai.api.module.comparacao.dto.TipoAtivoComparacao;
import com.investai.api.module.comparacao.dto.VereditoComparacaoResponseDTO;
import com.investai.api.module.comparacao.service.ComparacaoVereditoService;
import com.investai.api.shared.security.UsuarioAutenticadoHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/comparacao")
@RequiredArgsConstructor
public class ComparacaoVereditoController {

    private final ComparacaoVereditoService comparacaoVereditoService;
    private final UsuarioAutenticadoHelper usuarioAutenticadoHelper;

    @GetMapping("/veredito")
    public ResponseEntity<VereditoComparacaoResponseDTO> obterVeredito(
            @RequestParam TipoAtivoComparacao tipoA,
            @RequestParam String identificadorA,
            @RequestParam TipoAtivoComparacao tipoB,
            @RequestParam String identificadorB
    ) {
        return ResponseEntity.ok(comparacaoVereditoService.obterVeredito(
                usuarioAutenticadoHelper.getIdUsuarioLogado(), tipoA, identificadorA, tipoB, identificadorB));
    }
}
