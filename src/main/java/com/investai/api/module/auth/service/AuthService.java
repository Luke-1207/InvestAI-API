package com.investai.api.module.auth.service;

import com.investai.api.infra.exception.BusinessException;
import com.investai.api.infra.exception.ConflictException;
import com.investai.api.module.auth.dto.CadastroRequestDTO;
import com.investai.api.module.auth.dto.CadastroResponseDTO;
import com.investai.api.module.auth.entity.Role;
import com.investai.api.module.auth.entity.Usuario;
import com.investai.api.module.auth.repository.UsuarioRepository;
import com.investai.api.module.perfil.entity.PerfilInvestidor;
import com.investai.api.module.perfil.repository.PerfilInvestidorRepository;
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

    @Transactional
    public CadastroResponseDTO cadastrar(CadastroRequestDTO dto) {
        if(!dto.getSenha().equals(dto.getConfirmarSenha())) {
            throw new BusinessException("As senhas não conferem");
        }

        // TODO - Verificar Segurança - expor e-mail já cadastrado?
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

}
