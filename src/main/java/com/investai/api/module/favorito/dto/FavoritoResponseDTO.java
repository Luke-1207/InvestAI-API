package com.investai.api.module.favorito.dto;

import com.investai.api.module.favorito.entity.TipoAtivoFavorito;
import com.investai.api.module.notificacao.entity.TipoNotificacao;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class FavoritoResponseDTO {

    private UUID id;
    private TipoAtivoFavorito tipoAtivo;
    private UUID ativoId;
    private String codigo;
    private String nome;
    private boolean alertasAtivos;
    private List<TipoNotificacao> tiposAlertaAtivos;
    private List<TipoNotificacao> tiposAlertaDisponiveis;
    private BigDecimal limiarVariacao;
    private LocalDateTime criadoEm;
}
