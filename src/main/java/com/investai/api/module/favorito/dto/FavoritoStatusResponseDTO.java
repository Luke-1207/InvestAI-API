package com.investai.api.module.favorito.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class FavoritoStatusResponseDTO {

    private boolean favoritado;
    private UUID favoritoId;
}
