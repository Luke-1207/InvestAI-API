package com.investai.api.module.perfil.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "quiz_pergunta")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizPergunta {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private Integer ordem;

    @Column(nullable = false, length = 255)
    private String texto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoPergunta tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "campo_perfil", nullable = false, length = 30)
    private CampoPerfilQuiz campoPerfil;

    @Column(nullable = false)
    private boolean obrigatoria = true;

    @Column(nullable = false)
    private boolean ativa = true;

    @Builder.Default
    @OneToMany(mappedBy = "quizPergunta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("ordem ASC")
    private List<QuizOpcao> opcoes = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;
}