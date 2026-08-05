package com.investai.api.module.auth.service;
import com.investai.api.infra.exception.BusinessException;
import com.investai.api.infra.mail.MailService;
import com.investai.api.module.auth.dto.EsqueciSenhaRequestDTO;
import com.investai.api.module.auth.dto.RedefinirSenhaRequestDTO;
import com.investai.api.module.auth.entity.PasswordResetToken;
import com.investai.api.module.auth.entity.Usuario;
import com.investai.api.module.auth.repository.PasswordResetTokenRepository;
import com.investai.api.module.auth.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenService refreshTokenService;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${security.reset-token-expiration-minutes:30}")
    private long expirationMinutes;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Transactional
    public void solicitarRecuperacao(EsqueciSenhaRequestDTO dto) {
        String email = dto.getEmail().toLowerCase().trim();

        usuarioRepository.findByEmail(email)
                .filter(Usuario::isEnabled)
                .ifPresent(usuario -> {
                    passwordResetTokenRepository.invalidarTokensAnteriores(usuario.getId());

                    PasswordResetToken token = PasswordResetToken.builder()
                            .usuario(usuario)
                            .token(UUID.randomUUID().toString())
                            .usado(false)
                            .expiraEm(LocalDateTime.now().plusMinutes(expirationMinutes))
                            .build();

                    passwordResetTokenRepository.save(token);

                    String link = frontendUrl + "/redefinir-senha?token=" + token.getToken();
                    mailService.enviarEmailRecuperacaoSenha(usuario.getEmail(), usuario.getNome(), link);
                });
    }

    @Transactional
    public void redefinirSenha(RedefinirSenhaRequestDTO dto) {
        PasswordResetToken token = passwordResetTokenRepository.findByToken(dto.getToken())
                .orElseThrow(() -> new BusinessException("Token inválido ou expirado"));

        if (!token.estaValido()) {
            throw new BusinessException("Token inválido ou expirado");
        }

        if (!dto.getNovaSenha().equals(dto.getConfirmarNovaSenha())) {
            throw new BusinessException("As senhas não conferem");
        }

        Usuario usuario = token.getUsuario();
        usuario.setSenha(passwordEncoder.encode(dto.getNovaSenha()));
        usuarioRepository.save(usuario);

        token.setUsado(true);
        passwordResetTokenRepository.save(token);

        refreshTokenService.revogarTodos(usuario);
    }
}
