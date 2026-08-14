package com.investai.api.module.perfil.controller;

import com.investai.api.module.perfil.dto.QuizResponseDTO;
import com.investai.api.module.perfil.dto.QuizSubmissaoResponseDTO;
import com.investai.api.module.perfil.dto.SubmeterQuizRequestDTO;
import com.investai.api.module.perfil.service.PerfilQuizService;
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

    @GetMapping("/quiz")
    public ResponseEntity<QuizResponseDTO> obterQuiz() {
        return ResponseEntity.ok(perfilQuizService.obterQuiz());
    }

    @PutMapping("/quiz")
    public ResponseEntity<QuizSubmissaoResponseDTO> submeterQuiz(@Valid @RequestBody SubmeterQuizRequestDTO dto) {
        UUID usuarioId = usuarioAutenticadoHelper.getIdUsuarioLogado();
        return ResponseEntity.ok(perfilQuizService.submeterQuiz(usuarioId, dto));
    }
}