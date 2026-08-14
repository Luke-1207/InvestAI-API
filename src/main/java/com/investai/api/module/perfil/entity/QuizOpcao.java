package com.investai.api.module.perfil.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "quiz_opcao")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizOpcao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_pergunta_id", nullable = false)
    private QuizPergunta quizPergunta;

    @Column(nullable = false)
    private Integer ordem;

    @Column(nullable = false, length = 255)
    private String texto;

    @Column(length = 10)
    private String emoji;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "mapeamento_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> mapeamentoJson;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;
}