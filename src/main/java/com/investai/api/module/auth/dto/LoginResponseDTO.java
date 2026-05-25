package com.investai.api.module.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponseDTO {

    private String accessToken;
    private String refreshToken;
    private long expiresIn;

}
