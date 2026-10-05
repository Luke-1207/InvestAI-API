package com.investai.api.module.auth.service;

import com.investai.api.infra.exception.BusinessException;
import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.auth.entity.Usuario;
import com.investai.api.module.auth.entity.UsuarioFoto;
import com.investai.api.module.auth.repository.UsuarioFotoRepository;
import com.investai.api.shared.security.UsuarioAutenticadoHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioFotoServiceTest {

    private static final byte[] PNG_VALIDO = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG_VALIDO = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
    private static final byte[] WEBP_VALIDO = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};

    @Mock
    private UsuarioFotoRepository usuarioFotoRepository;

    @Mock
    private UsuarioAutenticadoHelper usuarioAutenticadoHelper;

    @InjectMocks
    private UsuarioFotoService usuarioFotoService;

    private Usuario usuario;

    @BeforeEach
    void setup() {
        usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("salvar - deve gravar uma foto PNG válida")
    void salvar_deveGravarPngValido() {
        when(usuarioAutenticadoHelper.getUsuarioLogado()).thenReturn(usuario);
        when(usuarioFotoRepository.findById(usuario.getId())).thenReturn(Optional.empty());

        usuarioFotoService.salvar(new MockMultipartFile("arquivo", "foto.png", "image/png", PNG_VALIDO));

        ArgumentCaptor<UsuarioFoto> captor = ArgumentCaptor.forClass(UsuarioFoto.class);
        verify(usuarioFotoRepository).save(captor.capture());
        assertThat(captor.getValue().getUsuarioId()).isEqualTo(usuario.getId());
        assertThat(captor.getValue().getContentType()).isEqualTo("image/png");
        assertThat(captor.getValue().getConteudo()).isEqualTo(PNG_VALIDO);
        assertThat(captor.getValue().getAtualizadoEm()).isNotNull();
    }

    @Test
    @DisplayName("salvar - deve aceitar JPEG e WEBP válidos")
    void salvar_deveAceitarJpegEWebp() {
        when(usuarioAutenticadoHelper.getUsuarioLogado()).thenReturn(usuario);
        when(usuarioFotoRepository.findById(usuario.getId())).thenReturn(Optional.empty());

        usuarioFotoService.salvar(new MockMultipartFile("arquivo", "a.jpg", "image/jpeg", JPEG_VALIDO));
        usuarioFotoService.salvar(new MockMultipartFile("arquivo", "a.webp", "image/webp", WEBP_VALIDO));

        verify(usuarioFotoRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("salvar - deve substituir a foto existente em vez de criar outra")
    void salvar_deveSubstituirFotoExistente() {
        UsuarioFoto existente = UsuarioFoto.builder()
                .usuarioId(usuario.getId()).conteudo(JPEG_VALIDO).contentType("image/jpeg").build();
        when(usuarioAutenticadoHelper.getUsuarioLogado()).thenReturn(usuario);
        when(usuarioFotoRepository.findById(usuario.getId())).thenReturn(Optional.of(existente));

        usuarioFotoService.salvar(new MockMultipartFile("arquivo", "foto.png", "image/png", PNG_VALIDO));

        verify(usuarioFotoRepository).save(existente);
        assertThat(existente.getContentType()).isEqualTo("image/png");
        assertThat(existente.getConteudo()).isEqualTo(PNG_VALIDO);
    }

    @Test
    @DisplayName("salvar - deve rejeitar arquivo vazio")
    void salvar_deveRejeitarArquivoVazio() {
        assertThatThrownBy(() -> usuarioFotoService.salvar(
                new MockMultipartFile("arquivo", "foto.png", "image/png", new byte[0])))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Nenhum arquivo enviado");

        verify(usuarioFotoRepository, never()).save(any());
    }

    @Test
    @DisplayName("salvar - deve rejeitar arquivo acima de 2 MB")
    void salvar_deveRejeitarArquivoGrandeDemais() {
        byte[] grande = new byte[(int) UsuarioFotoService.TAMANHO_MAXIMO_BYTES + 1];
        System.arraycopy(PNG_VALIDO, 0, grande, 0, PNG_VALIDO.length);

        assertThatThrownBy(() -> usuarioFotoService.salvar(
                new MockMultipartFile("arquivo", "foto.png", "image/png", grande)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A foto deve ter no máximo 2 MB");
    }

    @Test
    @DisplayName("salvar - deve rejeitar tipo não suportado (ex: GIF)")
    void salvar_deveRejeitarTipoNaoSuportado() {
        assertThatThrownBy(() -> usuarioFotoService.salvar(
                new MockMultipartFile("arquivo", "foto.gif", "image/gif", new byte[]{'G', 'I', 'F', '8'})))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Formato não suportado. Use JPG, PNG ou WEBP");
    }

    @Test
    @DisplayName("salvar - deve rejeitar arquivo que diz ser PNG mas não tem a assinatura de PNG")
    void salvar_deveRejeitarContentTypeForjado() {
        byte[] textoQualquer = "isto nao e uma imagem".getBytes();

        assertThatThrownBy(() -> usuarioFotoService.salvar(
                new MockMultipartFile("arquivo", "foto.png", "image/png", textoQualquer)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("O conteúdo do arquivo não corresponde a uma imagem válida");

        verify(usuarioFotoRepository, never()).save(any());
    }

    @Test
    @DisplayName("salvar - deve rejeitar arquivo RIFF que não é WEBP (ex: WAV)")
    void salvar_deveRejeitarRiffQueNaoEWebp() {
        byte[] wav = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'A', 'V', 'E'};

        assertThatThrownBy(() -> usuarioFotoService.salvar(
                new MockMultipartFile("arquivo", "foto.webp", "image/webp", wav)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("obter - deve lançar exceção quando o usuário não tem foto")
    void obter_deveLancarExcecaoQuandoNaoHaFoto() {
        when(usuarioAutenticadoHelper.getUsuarioLogado()).thenReturn(usuario);
        when(usuarioFotoRepository.findById(usuario.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioFotoService.obter())
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("remover - deve apagar a foto quando existe")
    void remover_deveApagarQuandoExiste() {
        when(usuarioAutenticadoHelper.getUsuarioLogado()).thenReturn(usuario);
        when(usuarioFotoRepository.existsById(usuario.getId())).thenReturn(true);

        usuarioFotoService.remover();

        verify(usuarioFotoRepository).deleteById(usuario.getId());
    }

    @Test
    @DisplayName("remover - não deve falhar quando não existe foto (idempotente)")
    void remover_naoDeveFalharQuandoNaoExiste() {
        when(usuarioAutenticadoHelper.getUsuarioLogado()).thenReturn(usuario);
        when(usuarioFotoRepository.existsById(usuario.getId())).thenReturn(false);

        usuarioFotoService.remover();

        verify(usuarioFotoRepository, never()).deleteById(any());
    }
}