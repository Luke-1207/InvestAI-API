package com.investai.api.module.integracao.dto;

import com.investai.api.module.integracao.entity.Integracao;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StatusIntegracaoDTO {
    private Integracao chave;
    private String nome;
    private SituacaoIntegracao situacao;
    private boolean simulado;
    private String detalhe;
    private LocalDateTime ultimoSucessoEm;
    private LocalDateTime ultimaFalhaEm;
    private String ultimoErro;
}
