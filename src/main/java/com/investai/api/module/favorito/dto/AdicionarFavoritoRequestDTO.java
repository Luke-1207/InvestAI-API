package com.investai.api.module.favorito.dto;

import com.investai.api.module.favorito.entity.TipoAtivoFavorito;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdicionarFavoritoRequestDTO {

    @NotNull(message = "O tipo do ativo é obrigatório")
    private TipoAtivoFavorito tipoAtivo;

    private String codigo;

    private UUID ativoId;
}
