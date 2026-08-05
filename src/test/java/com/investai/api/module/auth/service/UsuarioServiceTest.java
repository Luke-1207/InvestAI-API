package com.investai.api.module.auth.service;

import com.investai.api.infra.exception.BusinessException;
import com.investai.api.infra.exception.ConflictException;
import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.auth.dto.*;
import com.investai.api.module.auth.entity.Role;
import com.investai.api.module.auth.entity.Usuario;
import com.investai.api.module.auth.repository.UsuarioRepository;
import com.investai.api.module.perfil.entity.PerfilInvestidor;
import com.investai.api.module.perfil.repository.PerfilInvestidorRepository;
import com.investai.api.shared.security.UsuarioAutenticadoHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UsuarioAutenticadoHelper usuarioAutenticadoHelper;

    @Mock
    private PerfilInvestidorRepository perfilInvestidorRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuario;

    @BeforeEach
    void setup() {

        usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .nome("Lucas")
                .email("lucas@email.com")
                .senha("senha-criptografada")
                .role(Role.USUARIO)
                .ativo(true)
                .criadoEm(LocalDateTime.now())
                .atualizadoEm(LocalDateTime.now())
                .build();

    }

    @Test
    void deveObterUsuarioLogado() {

        when(usuarioAutenticadoHelper.getUsuarioLogado())
                .thenReturn(usuario);

        UsuarioResponseDTO response = usuarioService.obter();

        assertNotNull(response);
        assertEquals(usuario.getId(), response.getId());
        assertEquals(usuario.getNome(), response.getNome());
        assertEquals(usuario.getEmail(), response.getEmail());
        assertEquals(usuario.getRole(), response.getRole());
        assertEquals(usuario.isAtivo(), response.isAtivo());

        verify(usuarioAutenticadoHelper).getUsuarioLogado();
    }

    @Test
    void deveAtualizarUsuarioComSucesso() {

        AtualizarUsuarioRequestDTO dto =
                AtualizarUsuarioRequestDTO.builder()
                        .nome("Lucas Silva")
                        .email("lucasnovo@email.com")
                        .build();

        when(usuarioAutenticadoHelper.getUsuarioLogado())
                .thenReturn(usuario);

        when(usuarioRepository.existsByEmail(dto.getEmail().toLowerCase()))
                .thenReturn(false);

        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioResponseDTO response = usuarioService.atualizar(dto);

        assertEquals("Lucas Silva", response.getNome());
        assertEquals("lucasnovo@email.com", response.getEmail());

        verify(usuarioRepository).save(usuario);
    }

    @Test
    void deveAtualizarUsuarioMantendoMesmoEmail() {

        AtualizarUsuarioRequestDTO dto =
                AtualizarUsuarioRequestDTO.builder()
                        .nome("Novo Nome")
                        .email(usuario.getEmail())
                        .build();

        when(usuarioAutenticadoHelper.getUsuarioLogado())
                .thenReturn(usuario);

        when(usuarioRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        usuarioService.atualizar(dto);

        verify(usuarioRepository, never())
                .existsByEmail(anyString());

        verify(usuarioRepository).save(usuario);
    }

    @Test
    void deveSalvarEmailEmLowerCaseAoAtualizar() {

        AtualizarUsuarioRequestDTO dto =
                AtualizarUsuarioRequestDTO.builder()
                        .nome("Lucas")
                        .email("LUCASNOVO@EMAIL.COM")
                        .build();

        when(usuarioAutenticadoHelper.getUsuarioLogado())
                .thenReturn(usuario);

        when(usuarioRepository.existsByEmail(anyString()))
                .thenReturn(false);

        when(usuarioRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        usuarioService.atualizar(dto);

        ArgumentCaptor<Usuario> captor =
                ArgumentCaptor.forClass(Usuario.class);

        verify(usuarioRepository).save(captor.capture());

        assertEquals(
                "lucasnovo@email.com",
                captor.getValue().getEmail()
        );
    }

    @Test
    void deveLancarConflictExceptionQuandoEmailJaExistir() {

        AtualizarUsuarioRequestDTO dto =
                AtualizarUsuarioRequestDTO.builder()
                        .nome("Lucas")
                        .email("novo@email.com")
                        .build();

        when(usuarioAutenticadoHelper.getUsuarioLogado())
                .thenReturn(usuario);

        when(usuarioRepository.existsByEmail("novo@email.com"))
                .thenReturn(true);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> usuarioService.atualizar(dto)
        );

        assertEquals(
                "E-mail já está em uso",
                exception.getMessage()
        );

        verify(usuarioRepository, never())
                .save(any());
    }

    @Test
    void deveAlterarSenhaComSucesso() {

        AlterarSenhaRequestDTO dto =
                AlterarSenhaRequestDTO.builder()
                        .senhaAtual("123456")
                        .novaSenha("654321")
                        .confirmarNovaSenha("654321")
                        .build();

        when(usuarioAutenticadoHelper.getUsuarioLogado())
                .thenReturn(usuario);

        when(passwordEncoder.matches(
                "123456",
                usuario.getSenha()))
                .thenReturn(true);

        when(passwordEncoder.matches(
                "654321",
                usuario.getSenha()))
                .thenReturn(false);

        when(passwordEncoder.encode("654321"))
                .thenReturn("nova-senha-criptografada");

        usuarioService.alterarSenha(dto);

        assertEquals(
                "nova-senha-criptografada",
                usuario.getSenha()
        );

        verify(usuarioRepository).save(usuario);
    }

    @Test
    void deveLancarBusinessExceptionQuandoSenhaAtualForIncorreta() {

        AlterarSenhaRequestDTO dto =
                AlterarSenhaRequestDTO.builder()
                        .senhaAtual("errada")
                        .novaSenha("654321")
                        .confirmarNovaSenha("654321")
                        .build();

        when(usuarioAutenticadoHelper.getUsuarioLogado())
                .thenReturn(usuario);

        when(passwordEncoder.matches(
                dto.getSenhaAtual(),
                usuario.getSenha()))
                .thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> usuarioService.alterarSenha(dto)
        );

        assertEquals(
                "Senha atual incorreta",
                exception.getMessage()
        );

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deveLancarBusinessExceptionQuandoConfirmacaoForDiferente() {

        AlterarSenhaRequestDTO dto =
                AlterarSenhaRequestDTO.builder()
                        .senhaAtual("123456")
                        .novaSenha("654321")
                        .confirmarNovaSenha("999999")
                        .build();

        when(usuarioAutenticadoHelper.getUsuarioLogado())
                .thenReturn(usuario);

        when(passwordEncoder.matches(
                dto.getSenhaAtual(),
                usuario.getSenha()))
                .thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> usuarioService.alterarSenha(dto)
        );

        assertEquals(
                "As senhas não conferem",
                exception.getMessage()
        );

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deveLancarBusinessExceptionQuandoNovaSenhaForIgualAtual() {

        AlterarSenhaRequestDTO dto =
                AlterarSenhaRequestDTO.builder()
                        .senhaAtual("123456")
                        .novaSenha("123456")
                        .confirmarNovaSenha("123456")
                        .build();

        when(usuarioAutenticadoHelper.getUsuarioLogado())
                .thenReturn(usuario);

        when(passwordEncoder.matches(
                dto.getSenhaAtual(),
                usuario.getSenha()))
                .thenReturn(true);

        when(passwordEncoder.matches(
                dto.getNovaSenha(),
                usuario.getSenha()))
                .thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> usuarioService.alterarSenha(dto)
        );

        assertEquals(
                "A nova senha deve ser diferente da senha atual",
                exception.getMessage()
        );

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("listarUsuarios - deve retornar page de usuários com filtros aplicados")
    void listarUsuarios_deveRetornarPageComFiltros() {
        UsuarioFiltroDTO filtro = new UsuarioFiltroDTO();
        filtro.setBusca("lucas");
        filtro.setRole(Role.USUARIO);
        filtro.setAtivo(true);

        Pageable pageable = PageRequest.of(0, 20);

        Usuario usuario = criarUsuarioMock();
        PerfilInvestidor perfil = criarPerfilMock(usuario, false);

        Page<Usuario> page = new PageImpl<>(List.of(usuario));

        when(usuarioRepository.buscarComFiltros("lucas", String.valueOf(Role.USUARIO), true, pageable))
                .thenReturn(page);
        when(perfilInvestidorRepository.findByUsuarioId(usuario.getId()))
                .thenReturn(Optional.of(perfil));

        Page<UsuarioDetalheResponseDTO> resultado = usuarioService.listarUsuarios(filtro, pageable);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getEmail()).isEqualTo(usuario.getEmail());
        verify(usuarioRepository).buscarComFiltros("lucas", String.valueOf(Role.USUARIO), true, pageable);
    }

    @Test
    @DisplayName("listarUsuarios - deve retornar page vazia quando nenhum usuário encontrado")
    void listarUsuarios_deveRetornarPageVaziaQuandoNaoEncontrado() {
        UsuarioFiltroDTO filtro = new UsuarioFiltroDTO();
        Pageable pageable = PageRequest.of(0, 20);

        when(usuarioRepository.buscarComFiltros(null, null, null, pageable))
                .thenReturn(Page.empty());

        Page<UsuarioDetalheResponseDTO> resultado = usuarioService.listarUsuarios(filtro, pageable);

        assertThat(resultado.getContent()).isEmpty();
    }

    @Test
    @DisplayName("buscarUsuarioPorId - deve retornar usuário quando encontrado")
    void buscarUsuarioPorId_deveRetornarUsuarioQuandoEncontrado() {
        Usuario usuario = criarUsuarioMock();
        PerfilInvestidor perfil = criarPerfilMock(usuario, true);

        when(usuarioRepository.findById(usuario.getId()))
                .thenReturn(Optional.of(usuario));
        when(perfilInvestidorRepository.findByUsuarioId(usuario.getId()))
                .thenReturn(Optional.of(perfil));

        UsuarioDetalheResponseDTO resultado = usuarioService.buscarUsuarioPorId(usuario.getId());

        assertThat(resultado.getId()).isEqualTo(usuario.getId());
        assertThat(resultado.getEmail()).isEqualTo(usuario.getEmail());
        assertThat(resultado.isPerfilPreenchido()).isTrue();
    }

    @Test
    @DisplayName("buscarUsuarioPorId - deve lançar ResourceNotFoundException quando não encontrado")
    void buscarUsuarioPorId_deveLancarExcecaoQuandoNaoEncontrado() {
        UUID idInexistente = UUID.randomUUID();

        when(usuarioRepository.findById(idInexistente))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.buscarUsuarioPorId(idInexistente))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuário não encontrado");
    }

    @Test
    @DisplayName("alterarRole - deve promover usuário a GESTOR com sucesso")
    void alterarRole_devePromoverUsuarioComSucesso() {
        Usuario gestor = criarUsuarioMock();
        gestor.setRole(Role.GESTOR);

        Usuario alvo = criarUsuarioMock();
        alvo.setId(UUID.randomUUID());
        alvo.setRole(Role.USUARIO);

        PerfilInvestidor perfil = criarPerfilMock(alvo, false);

        when(usuarioAutenticadoHelper.getUsuarioLogado()).thenReturn(gestor);
        when(usuarioRepository.findById(alvo.getId())).thenReturn(Optional.of(alvo));
        when(usuarioRepository.save(alvo)).thenReturn(alvo);
        when(perfilInvestidorRepository.findByUsuarioId(alvo.getId()))
                .thenReturn(Optional.of(perfil));

        UsuarioDetalheResponseDTO resultado = usuarioService.alterarRole(alvo.getId(), Role.GESTOR);

        assertThat(resultado.getRole()).isEqualTo(Role.GESTOR);
        verify(usuarioRepository).save(alvo);
    }

    @Test
    @DisplayName("alterarRole - deve lançar exceção quando gestor tenta alterar a própria role")
    void alterarRole_deveLancarExcecaoQuandoGestorAlteraPropriaRole() {
        Usuario gestor = criarUsuarioMock();
        gestor.setRole(Role.GESTOR);

        when(usuarioAutenticadoHelper.getUsuarioLogado()).thenReturn(gestor);
        when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));

        assertThatThrownBy(() -> usuarioService.alterarRole(gestor.getId(), Role.USUARIO))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Você não pode alterar a própria role");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("alterarRole - deve lançar ResourceNotFoundException quando usuário alvo não existe")
    void alterarRole_deveLancarExcecaoQuandoAlvoNaoEncontrado() {
        Usuario gestor = criarUsuarioMock();
        UUID idInexistente = UUID.randomUUID();

        when(usuarioAutenticadoHelper.getUsuarioLogado()).thenReturn(gestor);
        when(usuarioRepository.findById(idInexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.alterarRole(idInexistente, Role.USUARIO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuário não encontrado");
    }

    @Test
    @DisplayName("alterarStatus - deve desativar conta de outro usuário com sucesso")
    void alterarStatus_deveDesativarContaComSucesso() {
        Usuario gestor = criarUsuarioMock();
        gestor.setRole(Role.GESTOR);

        Usuario alvo = criarUsuarioMock();
        alvo.setId(UUID.randomUUID());
        alvo.setAtivo(true);

        PerfilInvestidor perfil = criarPerfilMock(alvo, false);

        when(usuarioAutenticadoHelper.getUsuarioLogado()).thenReturn(gestor);
        when(usuarioRepository.findById(alvo.getId())).thenReturn(Optional.of(alvo));
        when(usuarioRepository.save(alvo)).thenReturn(alvo);
        when(perfilInvestidorRepository.findByUsuarioId(alvo.getId()))
                .thenReturn(Optional.of(perfil));

        UsuarioDetalheResponseDTO resultado = usuarioService.alterarStatus(alvo.getId(), false);

        assertThat(resultado.isAtivo()).isFalse();
        verify(usuarioRepository).save(alvo);
    }

    @Test
    @DisplayName("alterarStatus - deve reativar conta de outro usuário com sucesso")
    void alterarStatus_deveReativarContaComSucesso() {
        Usuario gestor = criarUsuarioMock();
        gestor.setRole(Role.GESTOR);

        Usuario alvo = criarUsuarioMock();
        alvo.setId(UUID.randomUUID());
        alvo.setAtivo(false);

        PerfilInvestidor perfil = criarPerfilMock(alvo, false);

        when(usuarioAutenticadoHelper.getUsuarioLogado()).thenReturn(gestor);
        when(usuarioRepository.findById(alvo.getId())).thenReturn(Optional.of(alvo));
        when(usuarioRepository.save(alvo)).thenReturn(alvo);
        when(perfilInvestidorRepository.findByUsuarioId(alvo.getId()))
                .thenReturn(Optional.of(perfil));

        UsuarioDetalheResponseDTO resultado = usuarioService.alterarStatus(alvo.getId(), true);

        assertThat(resultado.isAtivo()).isTrue();
        verify(usuarioRepository).save(alvo);
    }

    @Test
    @DisplayName("alterarStatus - deve lançar exceção quando gestor tenta desativar a própria conta")
    void alterarStatus_deveLancarExcecaoQuandoGestorDesativaPropraConta() {
        Usuario gestor = criarUsuarioMock();
        gestor.setRole(Role.GESTOR);

        when(usuarioAutenticadoHelper.getUsuarioLogado()).thenReturn(gestor);
        when(usuarioRepository.findById(gestor.getId())).thenReturn(Optional.of(gestor));

        assertThatThrownBy(() -> usuarioService.alterarStatus(gestor.getId(), false))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Você não pode alterar o status da própria conta");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("excluirConta - deve excluir conta com sucesso (soft delete)")
    void excluirConta_deveExcluirComSucesso() {
        Usuario usuario = criarUsuarioMock();

        ExcluirContaRequestDTO dto = new ExcluirContaRequestDTO();
        dto.setSenha("minhasenha123");

        when(usuarioAutenticadoHelper.getUsuarioLogado()).thenReturn(usuario);
        when(passwordEncoder.matches(dto.getSenha(), usuario.getSenha())).thenReturn(true);
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        usuarioService.excluirConta(dto);

        assertThat(usuario.isAtivo()).isFalse();
        assertThat(usuario.getDeletadoEm()).isNotNull();
        verify(usuarioRepository).save(usuario);
        verify(refreshTokenService).revogarTodos(usuario);
    }

    @Test
    @DisplayName("excluirConta - deve lançar exceção quando senha incorreta")
    void excluirConta_deveLancarExcecaoQuandoSenhaIncorreta() {
        Usuario usuario = criarUsuarioMock();

        ExcluirContaRequestDTO dto = new ExcluirContaRequestDTO();
        dto.setSenha("senhaerrada");

        when(usuarioAutenticadoHelper.getUsuarioLogado()).thenReturn(usuario);
        when(passwordEncoder.matches(dto.getSenha(), usuario.getSenha())).thenReturn(false);

        assertThatThrownBy(() -> usuarioService.excluirConta(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Senha incorreta");

        verify(usuarioRepository, never()).save(any());
        verify(refreshTokenService, never()).revogarTodos(any());
    }

    private Usuario criarUsuarioMock() {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setNome("Lucas Silva");
        usuario.setEmail("lucas@email.com");
        usuario.setSenha("senha-encriptada");
        usuario.setRole(Role.USUARIO);
        usuario.setAtivo(true);
        usuario.setCriadoEm(LocalDateTime.now());
        usuario.setAtualizadoEm(LocalDateTime.now());
        return usuario;
    }

    private PerfilInvestidor criarPerfilMock(Usuario usuario, boolean preenchido) {
        PerfilInvestidor perfil = new PerfilInvestidor();
        perfil.setId(UUID.randomUUID());
        perfil.setUsuario(usuario);
        perfil.setPerfilPreenchido(preenchido);
        return perfil;
    }

}