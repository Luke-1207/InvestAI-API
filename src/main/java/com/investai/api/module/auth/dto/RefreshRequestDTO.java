package com.investai.api.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RefreshRequestDTO {

    @NotBlank(message = "Refresh token é obrigatório")
    private String refreshToken;
}
