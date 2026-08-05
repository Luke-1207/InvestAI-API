package com.investai.api.module.auth.controller;

import com.investai.api.module.auth.dto.*;
import com.investai.api.module.auth.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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

    @GetMapping
    public ResponseEntity<Page<UsuarioDetalheResponseDTO>> listarUsuarios(
            @ModelAttribute UsuarioFiltroDTO filtro,
            @PageableDefault(size = 20, sort = "nome") Pageable pageable
    ) {
        return ResponseEntity.ok(usuarioService.listarUsuarios(filtro, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDetalheResponseDTO> buscarUsuarioPorId(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(usuarioService.buscarUsuarioPorId(id));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<UsuarioDetalheResponseDTO> alterarRole(
            @PathVariable UUID id,
            @Valid @RequestBody AlterarRoleRequestDTO dto
    ) {
        return ResponseEntity.ok(usuarioService.alterarRole(id, dto.getRole()));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UsuarioDetalheResponseDTO> alterarStatus(
            @PathVariable UUID id,
            @Valid @RequestBody AlterarStatusRequestDTO dto
    ) {
        return ResponseEntity.ok(usuarioService.alterarStatus(id, dto.getAtivo()));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> excluirMinhaConta(
            @Valid @RequestBody ExcluirContaRequestDTO dto
    ) {
        usuarioService.excluirConta(dto);
        return ResponseEntity.noContent().build();
    }
}
