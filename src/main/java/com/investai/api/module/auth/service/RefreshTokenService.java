package com.investai.api.module.auth.service;

import com.investai.api.infra.exception.BusinessException;
import com.investai.api.module.auth.entity.RefreshToken;
import com.investai.api.module.auth.entity.Usuario;
import com.investai.api.module.auth.repository.RefreshTokenRepository;
import com.investai.api.shared.security.JwtUtil;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    @Transactional
    public RefreshToken criar(Usuario usuario) {
        RefreshToken refreshToken = RefreshToken.builder()
                .usuario(usuario)
                .token(UUID.randomUUID().toString())
                .revogado(false)
                .expiraEm(LocalDateTime.now().plusDays(jwtUtil.getRefreshExpirationDays()))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional(readOnly = true)
    public RefreshToken buscarValido(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("Refresh token inválido"));

        if (!refreshToken.estaValido()) {
            throw new BusinessException("Refresh token expirado ou revogado");
        }

        return refreshToken;
    }

    @Transactional
    public void revogar(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevogado(true);
            refreshTokenRepository.save(rt);
        });
    }

    @Transactional
    public void revogarTodos(Usuario usuario) {
        refreshTokenRepository.revogarTodosPorUsuario(usuario.getId());
    }
}