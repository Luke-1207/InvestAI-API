package com.investai.api.module.auth.service;

import com.investai.api.infra.exception.BusinessException;
import com.investai.api.infra.exception.ConflictException;
import com.investai.api.module.auth.dto.*;
import com.investai.api.module.auth.entity.RefreshToken;
import com.investai.api.module.auth.entity.Role;
import com.investai.api.module.auth.entity.Usuario;
import com.investai.api.module.auth.repository.UsuarioRepository;
import com.investai.api.module.perfil.entity.PerfilInvestidor;
import com.investai.api.module.perfil.repository.PerfilInvestidorRepository;
import com.investai.api.shared.security.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PerfilInvestidorRepository perfilInvestidorRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;

    @Transactional
    public CadastroResponseDTO cadastrar(CadastroRequestDTO dto) {
        if(!dto.getSenha().equals(dto.getConfirmarSenha())) {
            throw new BusinessException("As senhas não conferem");
        }

        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new ConflictException("E-mail já cadastrado");
        }

        Usuario usuario = Usuario.builder()
                .nome(dto.getNome())
                .email(dto.getEmail().toLowerCase().trim())
                .senha(passwordEncoder.encode(dto.getSenha()))
                .role(Role.USUARIO)
                .ativo(true)
                .build();

        usuarioRepository.save(usuario);

        PerfilInvestidor perfil = PerfilInvestidor.builder()
                .usuario(usuario)
                .perfilPreenchido(false)
                .build();

        perfilInvestidorRepository.save(perfil);

        return CadastroResponseDTO.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .role(usuario.getRole())
                .build();
    }

    @Transactional
    public LoginResponseDTO login(LoginRequestDTO dto) {

        Usuario usuario = usuarioRepository.findByEmail(dto.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new BusinessException("Credenciais inválidas"));

        if (!passwordEncoder.matches(dto.getSenha(), usuario.getSenha())) {
            throw new BusinessException("Credenciais inválidas");
        }

        if (!usuario.isEnabled()) {
            throw new BusinessException("Credenciais inválidas");
        }

        String accessToken = jwtUtil.gerarToken(usuario);
        RefreshToken refreshToken = refreshTokenService.criar(usuario);

        return LoginResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .expiresIn(jwtUtil.getExpirationMs() / 1000)
                .build();
    }

    @Transactional
    public LoginResponseDTO refresh(RefreshRequestDTO dto){
        RefreshToken tokenAtual = refreshTokenService.buscarValido(dto.getRefreshToken());
        Usuario usuario = tokenAtual.getUsuario();

        refreshTokenService.revogar(dto.getRefreshToken());

        String novoAccessToken = jwtUtil.gerarToken(usuario);
        RefreshToken novoRefreshToken = refreshTokenService.criar(usuario);

        return LoginResponseDTO.builder()
                .accessToken(novoAccessToken)
                .refreshToken(novoRefreshToken.getToken())
                .expiresIn(jwtUtil.getExpirationMs() / 1000)
                .build();
    }

}
