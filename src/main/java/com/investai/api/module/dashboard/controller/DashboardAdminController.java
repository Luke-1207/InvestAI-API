package com.investai.api.module.dashboard.controller;

import com.investai.api.module.dashboard.dto.DashboardAdminResponseDTO;
import com.investai.api.module.dashboard.dto.StatusIaResponseDTO;
import com.investai.api.module.dashboard.service.DashboardAdminService;
import com.investai.api.module.integracao.dto.SaudeIntegracoesResponseDTO;
import com.investai.api.module.integracao.dto.StatusIntegracaoDTO;
import com.investai.api.module.integracao.entity.Integracao;
import com.investai.api.module.integracao.service.SaudeIntegracoesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/dashboard/admin")
@RequiredArgsConstructor
public class DashboardAdminController {

    private final DashboardAdminService dashboardAdminService;
    private final SaudeIntegracoesService saudeIntegracoesService;

    @GetMapping
    public ResponseEntity<DashboardAdminResponseDTO> obterMetricasAdmin() {
        return ResponseEntity.ok(dashboardAdminService.obterMetricasAdmin());
    }

    @GetMapping("/ia-status")
    public ResponseEntity<StatusIaResponseDTO> obterStatusIa() {
        return ResponseEntity.ok(dashboardAdminService.obterStatusIa());
    }

    @GetMapping("/integracoes")
    public ResponseEntity<SaudeIntegracoesResponseDTO> obterSaudeIntegracoes() {
        return ResponseEntity.ok(saudeIntegracoesService.verificar());
    }

    @PostMapping("/integracoes/verificar")
    public ResponseEntity<SaudeIntegracoesResponseDTO> verificarIntegracoes() {
        return ResponseEntity.ok(saudeIntegracoesService.verificarAgora());
    }

    @PostMapping("/integracoes/{chave}/verificar")
    public ResponseEntity<StatusIntegracaoDTO> verificarIntegracao(@PathVariable Integracao chave) {
        return ResponseEntity.ok(saudeIntegracoesService.verificarAgora(chave));
    }
}