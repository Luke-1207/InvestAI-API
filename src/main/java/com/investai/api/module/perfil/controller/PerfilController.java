package com.investai.api.module.perfil.controller;

import com.investai.api.module.perfil.dto.*;
import com.investai.api.module.perfil.service.PerfilQuizService;
import com.investai.api.module.perfil.service.PerfilService;
import com.investai.api.shared.security.UsuarioAutenticadoHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/perfil")
@RequiredArgsConstructor
public class PerfilController {

    private final PerfilQuizService perfilQuizService;
    private final UsuarioAutenticadoHelper usuarioAutenticadoHelper;
    private final PerfilService perfilService;

    @GetMapping
    public ResponseEntity<PerfilResponseDTO> obterPerfil() {
        UUID usuarioId = usuarioAutenticadoHelper.getIdUsuarioLogado();
        return ResponseEntity.ok(perfilService.obterPerfil(usuarioId));
    }

    @PutMapping
    public ResponseEntity<PerfilResponseDTO> editarPerfil(@Valid @RequestBody EditarPerfilRequestDTO dto) {
        UUID usuarioId = usuarioAutenticadoHelper.getIdUsuarioLogado();
        return ResponseEntity.ok(perfilService.editarPerfil(usuarioId, dto));
    }

    @GetMapping("/quiz")
    public ResponseEntity<QuizResponseDTO> obterQuiz() {
        return ResponseEntity.ok(perfilQuizService.obterQuiz());
    }

    @PutMapping("/quiz")
    public ResponseEntity<QuizSubmissaoResponseDTO> submeterQuiz(@Valid @RequestBody SubmeterQuizRequestDTO dto) {
        UUID usuarioId = usuarioAutenticadoHelper.getIdUsuarioLogado();
        return ResponseEntity.ok(perfilQuizService.submeterQuiz(usuarioId, dto));
    }

    @PatchMapping("/refazer-quiz")
    public ResponseEntity<PerfilResponseDTO> refazerQuiz() {
        UUID usuarioId = usuarioAutenticadoHelper.getIdUsuarioLogado();
        return ResponseEntity.ok(perfilService.refazerQuiz(usuarioId));
    }

    @GetMapping("/{usuarioId}")
    public ResponseEntity<PerfilResponseDTO> obterPerfilPorUsuarioId(@PathVariable UUID usuarioId) {
        return ResponseEntity.ok(perfilService.obterPerfil(usuarioId));
    }
}