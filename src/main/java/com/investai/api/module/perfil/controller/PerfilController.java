package com.investai.api.module.perfil.controller;

import com.investai.api.module.perfil.dto.QuizResponseDTO;
import com.investai.api.module.perfil.service.PerfilQuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/perfil")
@RequiredArgsConstructor
public class PerfilController {

    private final PerfilQuizService perfilQuizService;

    @GetMapping("/quiz")
    public ResponseEntity<QuizResponseDTO> obterQuiz() {
        return ResponseEntity.ok(perfilQuizService.obterQuiz());
    }
}