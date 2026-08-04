package com.investai.api.module.auth.service;

import com.investai.api.infra.exception.BusinessException;
import com.investai.api.infra.exception.ConflictException;
import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.auth.dto.*;
import com.investai.api.module.auth.entity.Role;
import com.investai.api.module.auth.entity.Usuario;
import com.investai.api.module.auth.repository.UsuarioRepository;
import com.investai.api.module.perfil.repository.PerfilInvestidorRepository;
import com.investai.api.shared.security.UsuarioAutenticadoHelper;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioAutenticadoHelper usuarioAutenticadoHelper;
    private final PerfilInvestidorRepository perfilInvestidorRepository;

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

    @Transactional(readOnly = true)
    public Page<UsuarioDetalheResponseDTO> listarUsuarios(
            UsuarioFiltroDTO filtro,
            Pageable pageable
    ) {
        String roleStr = filtro.getRole() != null ? filtro.getRole().name() : null;

        return usuarioRepository
                .buscarComFiltros(filtro.getBusca(), roleStr, filtro.getAtivo(), pageable)
                .map(this::toDetalheResponseDTO);
    }

    @Transactional(readOnly = true)
    public UsuarioDetalheResponseDTO buscarUsuarioPorId(UUID id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        return toDetalheResponseDTO(usuario);
    }

    @Transactional
    public UsuarioDetalheResponseDTO alterarRole(UUID id, Role novaRole) {
        Usuario gestor = usuarioAutenticadoHelper.getUsuarioLogado();
        Usuario alvo = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (gestor.getId().equals(alvo.getId())) {
            throw new BusinessException("Você não pode alterar a própria role");
        }

        alvo.setRole(novaRole);
        usuarioRepository.save(alvo);

        return toDetalheResponseDTO(alvo);
    }

    @Transactional
    public UsuarioDetalheResponseDTO alterarStatus(UUID id, boolean ativo) {
        Usuario gestor = usuarioAutenticadoHelper.getUsuarioLogado();
        Usuario alvo = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (gestor.getId().equals(alvo.getId())) {
            throw new BusinessException("Você não pode alterar o status da própria conta");
        }

        alvo.setAtivo(ativo);
        usuarioRepository.save(alvo);

        return toDetalheResponseDTO(alvo);
    }

    private UsuarioDetalheResponseDTO toDetalheResponseDTO(Usuario usuario) {
        boolean perfilPreenchido = perfilInvestidorRepository
                .findByUsuarioId(usuario.getId())
                .map(p -> p.isPerfilPreenchido())
                .orElse(false);

        return UsuarioDetalheResponseDTO.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .role(usuario.getRole())
                .ativo(usuario.isAtivo())
                .perfilPreenchido(perfilPreenchido)
                .criadoEm(usuario.getCriadoEm())
                .atualizadoEm(usuario.getAtualizadoEm())
                .deletadoEm(usuario.getDeletadoEm())
                .build();
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
