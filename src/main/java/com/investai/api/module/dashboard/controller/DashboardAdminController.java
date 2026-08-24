package com.investai.api.module.dashboard.controller;

import com.investai.api.module.dashboard.dto.DashboardAdminResponseDTO;
import com.investai.api.module.dashboard.service.DashboardAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/dashboard/admin")
@RequiredArgsConstructor
public class DashboardAdminController {

    private final DashboardAdminService dashboardAdminService;

    @GetMapping
    public ResponseEntity<DashboardAdminResponseDTO> obterMetricasAdmin() {
        return ResponseEntity.ok(dashboardAdminService.obterMetricasAdmin());
    }
}