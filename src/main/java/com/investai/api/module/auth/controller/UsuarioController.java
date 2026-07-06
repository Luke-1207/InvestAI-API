package com.investai.api.module.auth.controller;

import com.investai.api.module.auth.dto.AlterarSenhaRequestDTO;
import com.investai.api.module.auth.dto.AtualizarUsuarioRequestDTO;
import com.investai.api.module.auth.dto.UsuarioResponseDTO;
import com.investai.api.module.auth.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponseDTO> obter() {
        return ResponseEntity.ok(usuarioService.obter());
    }

    @PutMapping("/me")
    public ResponseEntity<UsuarioResponseDTO> atualizar(
            @Valid @RequestBody AtualizarUsuarioRequestDTO dto
    ) {
        return ResponseEntity.ok(usuarioService.atualizar(dto));
    }

    @PatchMapping("/me/senha")
    public ResponseEntity<Void> alterarSenha(
            @Valid @RequestBody AlterarSenhaRequestDTO dto
    ) {
        usuarioService.alterarSenha(dto);
        return ResponseEntity.noContent().build();
    }
}
