package com.investai.api.module.favorito.dto;

import com.investai.api.module.favorito.entity.TipoAtivoFavorito;

import java.time.LocalDate;
import java.util.UUID;

public record AtivoFavoritavelDTO(
        UUID id,
        TipoAtivoFavorito tipo,
        String codigo,
        String nome,
        LocalDate vencimento
) {
}
