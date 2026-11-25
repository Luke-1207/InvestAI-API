package com.investai.api.module.notificacao.dto;

import com.investai.api.module.favorito.entity.TipoAtivoFavorito;
import com.investai.api.module.notificacao.entity.TipoNotificacao;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class NotificacaoResponseDTO {

    private UUID id;
    private TipoNotificacao tipo;
    private String titulo;
    private String mensagem;
    private TipoAtivoFavorito tipoAtivo;
    private UUID ativoId;
    private String codigoAtivo;
    private boolean lida;
    private LocalDateTime lidaEm;
    private LocalDateTime criadoEm;
}
