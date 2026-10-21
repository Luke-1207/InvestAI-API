package com.investai.api.module.integracao.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "status_integracao")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusIntegracao {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Integracao integracao;

    @Column(name = "ultimo_sucesso_em")
    private LocalDateTime ultimoSucessoEm;

    @Column(name = "ultima_falha_em")
    private LocalDateTime ultimaFalhaEm;

    @Column(name = "ultimo_erro", length = 500)
    private String ultimoErro;
}
