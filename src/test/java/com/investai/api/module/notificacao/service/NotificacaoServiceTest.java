package com.investai.api.module.notificacao.service;

import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.auth.repository.UsuarioRepository;
import com.investai.api.module.favorito.dto.AtivoFavoritavelDTO;
import com.investai.api.module.favorito.entity.TipoAtivoFavorito;
import com.investai.api.module.favorito.service.AtivoFavoritavelService;
import com.investai.api.module.notificacao.dto.ComunicadoSistemaRequestDTO;
import com.investai.api.module.notificacao.dto.NotificacaoResponseDTO;
import com.investai.api.module.notificacao.entity.Notificacao;
import com.investai.api.module.notificacao.entity.TipoNotificacao;
import com.investai.api.module.notificacao.repository.NotificacaoRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacaoServiceTest {

    @Mock
    private NotificacaoRepository notificacaoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AtivoFavoritavelService ativoFavoritavelService;

    @InjectMocks
    private NotificacaoService notificacaoService;

    private final UUID usuarioId = UUID.randomUUID();

    @Test
    @DisplayName("listar - deve paginar, limitar o tamanho e trazer o código do ativo")
    void listar_devePaginarEResolverCodigo() {
        UUID acaoId = UUID.randomUUID();
        Notificacao comAtivo = Notificacao.builder().id(UUID.randomUUID()).usuarioId(usuarioId)
                .tipo(TipoNotificacao.VARIACAO_ALTA).titulo("PETR4 subiu").mensagem("m")
                .tipoAtivo(TipoAtivoFavorito.ACAO_FII_ETF).ativoId(acaoId).criadoEm(LocalDateTime.now()).build();
        Notificacao semAtivo = Notificacao.builder().id(UUID.randomUUID()).usuarioId(usuarioId)
                .tipo(TipoNotificacao.SISTEMA).titulo("Aviso").mensagem("m").criadoEm(LocalDateTime.now()).build();
        when(notificacaoRepository.findByUsuarioIdOrderByCriadoEmDesc(eq(usuarioId), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(comAtivo, semAtivo), inv.getArgument(1), 2));
        when(ativoFavoritavelService.buscarPorIds(eq(TipoAtivoFavorito.ACAO_FII_ETF), anyCollection()))
                .thenReturn(Map.of(acaoId, new AtivoFavoritavelDTO(acaoId, TipoAtivoFavorito.ACAO_FII_ETF, "PETR4", "Petrobras", null)));

        Page<NotificacaoResponseDTO> pagina = notificacaoService.listar(usuarioId, null, 0, 500);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(notificacaoRepository).findByUsuarioIdOrderByCriadoEmDesc(eq(usuarioId), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(NotificacaoService.TAMANHO_MAXIMO_PAGINA);
        assertThat(pagina.getContent()).extracting(NotificacaoResponseDTO::getCodigoAtivo).containsExactly("PETR4", null);
    }

    @Test
    @DisplayName("listar - deve filtrar por lida quando informado")
    void listar_deveFiltrarPorLida() {
        when(notificacaoRepository.findByUsuarioIdAndLidaOrderByCriadoEmDesc(eq(usuarioId), eq(false), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        assertThat(notificacaoService.listar(usuarioId, false, 0, 10).getTotalElements()).isZero();
    }

    @Test
    @DisplayName("contarNaoLidas - deve retornar o total de não lidas")
    void contarNaoLidas_deveRetornarTotal() {
        when(notificacaoRepository.countByUsuarioIdAndLidaFalse(usuarioId)).thenReturn(4L);

        assertThat(notificacaoService.contarNaoLidas(usuarioId).total()).isEqualTo(4L);
    }

    @Test
    @DisplayName("marcarComoLida - deve marcar e registrar o horário")
    void marcarComoLida_deveMarcar() {
        UUID id = UUID.randomUUID();
        Notificacao notificacao = Notificacao.builder().id(id).usuarioId(usuarioId).tipo(TipoNotificacao.SISTEMA)
                .titulo("t").mensagem("m").build();
        when(notificacaoRepository.findByIdAndUsuarioId(id, usuarioId)).thenReturn(Optional.of(notificacao));
        when(notificacaoRepository.save(notificacao)).thenReturn(notificacao);

        NotificacaoResponseDTO resposta = notificacaoService.marcarComoLida(usuarioId, id);

        assertThat(resposta.isLida()).isTrue();
        assertThat(resposta.getLidaEm()).isNotNull();
    }

    @Test
    @DisplayName("marcarComoLida - deve retornar não encontrada para notificação de outro usuário")
    void marcarComoLida_deveBloquearOutroUsuario() {
        UUID id = UUID.randomUUID();
        when(notificacaoRepository.findByIdAndUsuarioId(id, usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificacaoService.marcarComoLida(usuarioId, id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("marcarTodasComoLidas - deve retornar quantas foram atualizadas")
    void marcarTodas_deveRetornarQuantidade() {
        when(notificacaoRepository.marcarTodasComoLidas(eq(usuarioId), any())).thenReturn(3);

        assertThat(notificacaoService.marcarTodasComoLidas(usuarioId).atualizadas()).isEqualTo(3);
    }

    @Test
    @DisplayName("enviarComunicado - deve criar uma notificação SISTEMA para cada usuário ativo")
    @SuppressWarnings("unchecked")
    void enviarComunicado_deveCriarParaTodos() {
        when(usuarioRepository.findIdsAtivos()).thenReturn(List.of(UUID.randomUUID(), UUID.randomUUID()));

        int destinatarios = notificacaoService.enviarComunicado(
                new ComunicadoSistemaRequestDTO("  Manutenção  ", " Sistema fora do ar às 22h ")).destinatarios();

        ArgumentCaptor<List<Notificacao>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificacaoRepository).saveAll(captor.capture());
        assertThat(destinatarios).isEqualTo(2);
        assertThat(captor.getValue()).allSatisfy(n -> {
            assertThat(n.getTipo()).isEqualTo(TipoNotificacao.SISTEMA);
            assertThat(n.getTitulo()).isEqualTo("Manutenção");
            assertThat(n.getMensagem()).isEqualTo("Sistema fora do ar às 22h");
        });
    }

    @Test
    @DisplayName("criar - deve cortar títulos maiores que o limite da coluna")
    void criar_deveCortarTitulo() {
        when(notificacaoRepository.save(any(Notificacao.class))).thenAnswer(inv -> inv.getArgument(0));

        Notificacao criada = notificacaoService.criar(usuarioId, TipoNotificacao.SISTEMA, "x".repeat(200), "m", null, null);

        assertThat(criada.getTitulo()).hasSize(Notificacao.TAMANHO_MAXIMO_TITULO);
    }
}
