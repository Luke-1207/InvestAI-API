package com.investai.api.module.auth.controller;

import com.investai.api.module.auth.dto.CadastroRequestDTO;
import com.investai.api.module.auth.dto.CadastroResponseDTO;
import com.investai.api.module.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/cadastro")
    public ResponseEntity<CadastroResponseDTO> cadastrar(
            @Valid @RequestBody CadastroRequestDTO dto
    ) {
        CadastroResponseDTO response = authService.cadastrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
