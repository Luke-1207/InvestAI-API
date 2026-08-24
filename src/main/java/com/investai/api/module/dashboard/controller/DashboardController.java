package com.investai.api.module.dashboard.controller;

import com.investai.api.module.dashboard.dto.DashboardResponseDTO;
import com.investai.api.module.dashboard.service.DashboardService;
import com.investai.api.shared.security.UsuarioAutenticadoHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final UsuarioAutenticadoHelper usuarioAutenticadoHelper;

    @GetMapping
    public ResponseEntity<DashboardResponseDTO> obterDashboard() {
        UUID usuarioId = usuarioAutenticadoHelper.getIdUsuarioLogado();
        return ResponseEntity.ok(dashboardService.obterDashboard(usuarioId));
    }
}