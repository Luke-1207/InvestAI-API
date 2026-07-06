package com.investai.api.module.auth.service;

import com.investai.api.infra.exception.BusinessException;
import com.investai.api.infra.exception.ConflictException;
import com.investai.api.module.auth.dto.AlterarSenhaRequestDTO;
import com.investai.api.module.auth.dto.AtualizarUsuarioRequestDTO;
import com.investai.api.module.auth.dto.UsuarioResponseDTO;
import com.investai.api.module.auth.entity.Usuario;
import com.investai.api.module.auth.repository.UsuarioRepository;
import com.investai.api.shared.security.UsuarioAutenticadoHelper;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioAutenticadoHelper usuarioAutenticadoHelper;

    @Transactional(readOnly = true)
    public UsuarioResponseDTO obter() {
        Usuario usuario = usuarioAutenticadoHelper.getUsuarioLogado();
        return toResponseDTO(usuario);
    }

    @Transactional
    public UsuarioResponseDTO atualizar(AtualizarUsuarioRequestDTO dto) {
        Usuario usuario = usuarioAutenticadoHelper.getUsuarioLogado();
        String novoEmail = dto.getEmail().toLowerCase().trim();

        if (!novoEmail.equals(usuario.getEmail())) {
            if (usuarioRepository.existsByEmail(novoEmail)) {
                throw new ConflictException("E-mail já está em uso");
            }
        }

        usuario.setNome(dto.getNome());
        usuario.setEmail(novoEmail);
        usuarioRepository.save(usuario);

        return toResponseDTO(usuario);
    }

    @Transactional
    public void alterarSenha(AlterarSenhaRequestDTO dto) {
        Usuario usuario = usuarioAutenticadoHelper.getUsuarioLogado();

        if (!passwordEncoder.matches(dto.getSenhaAtual(), usuario.getSenha())) {
            throw new BusinessException("Senha atual incorreta");
        }

        if (!dto.getNovaSenha().equals(dto.getConfirmarNovaSenha())) {
            throw new BusinessException("As senhas não conferem");
        }

        if (passwordEncoder.matches(dto.getNovaSenha(), usuario.getSenha())) {
            throw new BusinessException("A nova senha deve ser diferente da senha atual");
        }

        usuario.setSenha(passwordEncoder.encode(dto.getNovaSenha()));
        usuarioRepository.save(usuario);
    }

    private UsuarioResponseDTO toResponseDTO(Usuario usuario) {
        return UsuarioResponseDTO.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .role(usuario.getRole())
                .ativo(usuario.isAtivo())
                .criadoEm(usuario.getCriadoEm())
                .atualizadoEm(usuario.getAtualizadoEm())
                .build();
    }

}
