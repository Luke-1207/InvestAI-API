package com.investai.api.module.notificacao.service;

import com.investai.api.module.ativo.dto.CotacaoResponseDTO;
import com.investai.api.module.ativo.service.CotacaoService;
import com.investai.api.module.favorito.dto.AtivoFavoritavelDTO;
import com.investai.api.module.favorito.entity.Favorito;
import com.investai.api.module.favorito.entity.TipoAtivoFavorito;
import com.investai.api.module.favorito.repository.FavoritoRepository;
import com.investai.api.module.favorito.service.AtivoFavoritavelService;
import com.investai.api.module.notificacao.entity.TipoNotificacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GatilhosFavoritosServiceTest {

    @Mock
    private FavoritoRepository favoritoRepository;

    @Mock
    private AtivoFavoritavelService ativoFavoritavelService;

    @Mock
    private CotacaoService cotacaoService;

    @Mock
    private NotificacaoService notificacaoService;

    @InjectMocks
    private GatilhosFavoritosService service;

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 25, 15, 0);
    private final UUID usuarioId = UUID.randomUUID();
    private final UUID acaoId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(favoritoRepository.findByTipoAtivoAndAlertasAtivosTrue(any())).thenReturn(List.of());
        when(ativoFavoritavelService.buscarPorIds(eq(TipoAtivoFavorito.ACAO_FII_ETF), anyCollection()))
                .thenReturn(Map.of(acaoId, new AtivoFavoritavelDTO(acaoId, TipoAtivoFavorito.ACAO_FII_ETF, "PETR4", "Petrobras PN", null)));
    }

    private Favorito favoritoAcao(String limiar, List<TipoNotificacao> tipos) {
        return Favorito.builder().id(UUID.randomUUID()).usuarioId(usuarioId).ativoId(acaoId)
                .tipoAtivo(TipoAtivoFavorito.ACAO_FII_ETF).alertasAtivos(true)
                .tiposAlertaAtivos(tipos).limiarVariacao(new BigDecimal(limiar)).build();
    }

    private void cotacao(String variacao) {
        when(cotacaoService.obterCotacaoEmCache("PETR4")).thenReturn(Optional.of(CotacaoResponseDTO.builder()
                .codigo("PETR4").preco(new BigDecimal("38.10")).variacaoPercentual(new BigDecimal(variacao)).build()));
    }

    @Test
    @DisplayName("deve notificar alta quando a variação passa do limiar")
    void deveNotificarAlta() {
        when(favoritoRepository.findByTipoAtivoAndAlertasAtivosTrue(TipoAtivoFavorito.ACAO_FII_ETF))
                .thenReturn(List.of(favoritoAcao("5.00", null)));
        cotacao("6.20");

        int geradas = service.verificar(AGORA);

        assertThat(geradas).isEqualTo(1);
        verify(notificacaoService).criar(eq(usuarioId), eq(TipoNotificacao.VARIACAO_ALTA), contains("PETR4 subiu"),
                contains("R$"), eq(TipoAtivoFavorito.ACAO_FII_ETF), eq(acaoId));
    }

    @Test
    @DisplayName("deve notificar queda quando a variação negativa passa do limiar")
    void deveNotificarQueda() {
        when(favoritoRepository.findByTipoAtivoAndAlertasAtivosTrue(TipoAtivoFavorito.ACAO_FII_ETF))
                .thenReturn(List.of(favoritoAcao("3.00", null)));
        cotacao("-3.00");

        service.verificar(AGORA);

        verify(notificacaoService).criar(eq(usuarioId), eq(TipoNotificacao.VARIACAO_BAIXA), contains("caiu"), any(), any(), any());
    }

    @Test
    @DisplayName("não deve notificar variação abaixo do limiar")
    void naoDeveNotificarAbaixoDoLimiar() {
        when(favoritoRepository.findByTipoAtivoAndAlertasAtivosTrue(TipoAtivoFavorito.ACAO_FII_ETF))
                .thenReturn(List.of(favoritoAcao("5.00", null)));
        cotacao("4.99");

        assertThat(service.verificar(AGORA)).isZero();
        verify(notificacaoService, never()).criar(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("não deve notificar quando o tipo de alerta está desligado")
    void naoDeveNotificarTipoDesligado() {
        when(favoritoRepository.findByTipoAtivoAndAlertasAtivosTrue(TipoAtivoFavorito.ACAO_FII_ETF))
                .thenReturn(List.of(favoritoAcao("5.00", List.of(TipoNotificacao.VARIACAO_BAIXA))));
        cotacao("8.00");

        assertThat(service.verificar(AGORA)).isZero();
    }

    @Test
    @DisplayName("não deve repetir o aviso dentro da janela de 24 horas")
    void naoDeveRepetirNaJanela() {
        when(favoritoRepository.findByTipoAtivoAndAlertasAtivosTrue(TipoAtivoFavorito.ACAO_FII_ETF))
                .thenReturn(List.of(favoritoAcao("5.00", null)));
        cotacao("9.00");
        when(notificacaoService.jaNotificadoDesde(usuarioId, TipoNotificacao.VARIACAO_ALTA, acaoId, AGORA.minusHours(24)))
                .thenReturn(true);

        assertThat(service.verificar(AGORA)).isZero();
    }

    @Test
    @DisplayName("não deve consultar o provedor quando não há cotação em cache")
    void naoDeveNotificarSemCotacaoEmCache() {
        when(favoritoRepository.findByTipoAtivoAndAlertasAtivosTrue(TipoAtivoFavorito.ACAO_FII_ETF))
                .thenReturn(List.of(favoritoAcao("5.00", null)));
        when(cotacaoService.obterCotacaoEmCache("PETR4")).thenReturn(Optional.empty());

        assertThat(service.verificar(AGORA)).isZero();
        verify(cotacaoService, never()).obterCotacao(any());
    }

    @Test
    @DisplayName("deve avisar título favoritado que vence dentro de 30 dias")
    void deveAvisarVencimento() {
        UUID tesouroId = UUID.randomUUID();
        Favorito favorito = Favorito.builder().id(UUID.randomUUID()).usuarioId(usuarioId).ativoId(tesouroId)
                .tipoAtivo(TipoAtivoFavorito.TITULO_TESOURO).alertasAtivos(true).build();
        when(favoritoRepository.findByTipoAtivoAndAlertasAtivosTrue(TipoAtivoFavorito.TITULO_TESOURO)).thenReturn(List.of(favorito));
        when(ativoFavoritavelService.buscarPorIds(eq(TipoAtivoFavorito.TITULO_TESOURO), anyCollection())).thenReturn(Map.of(tesouroId,
                new AtivoFavoritavelDTO(tesouroId, TipoAtivoFavorito.TITULO_TESOURO, "LTN2026", "Tesouro Prefixado 2026", LocalDate.of(2026, 12, 7))));

        assertThat(service.verificar(AGORA)).isEqualTo(1);
        verify(notificacaoService).criar(eq(usuarioId), eq(TipoNotificacao.TITULO_VENCENDO), eq("Tesouro Prefixado 2026 vence em 12 dias"),
                contains("07/12/2026"), eq(TipoAtivoFavorito.TITULO_TESOURO), eq(tesouroId));
    }

    @Test
    @DisplayName("não deve avisar título que vence depois da janela ou já venceu")
    void naoDeveAvisarForaDaJanela() {
        UUID longe = UUID.randomUUID();
        UUID vencido = UUID.randomUUID();
        when(favoritoRepository.findByTipoAtivoAndAlertasAtivosTrue(TipoAtivoFavorito.TITULO_PRIVADO)).thenReturn(List.of(
                Favorito.builder().usuarioId(usuarioId).ativoId(longe).tipoAtivo(TipoAtivoFavorito.TITULO_PRIVADO).alertasAtivos(true).build(),
                Favorito.builder().usuarioId(usuarioId).ativoId(vencido).tipoAtivo(TipoAtivoFavorito.TITULO_PRIVADO).alertasAtivos(true).build()));
        when(ativoFavoritavelService.buscarPorIds(eq(TipoAtivoFavorito.TITULO_PRIVADO), anyCollection())).thenReturn(Map.of(
                longe, new AtivoFavoritavelDTO(longe, TipoAtivoFavorito.TITULO_PRIVADO, null, "CDB Banco A", LocalDate.of(2027, 6, 1)),
                vencido, new AtivoFavoritavelDTO(vencido, TipoAtivoFavorito.TITULO_PRIVADO, null, "CDB Banco B", LocalDate.of(2026, 11, 1))));

        assertThat(service.verificar(AGORA)).isZero();
        verify(notificacaoService, never()).criar(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("uma falha ao gravar não deve interromper os demais favoritos")
    void falhaNaoDeveInterromper() {
        when(favoritoRepository.findByTipoAtivoAndAlertasAtivosTrue(TipoAtivoFavorito.ACAO_FII_ETF))
                .thenReturn(List.of(favoritoAcao("5.00", null), favoritoAcao("5.00", null)));
        cotacao("7.00");
        when(notificacaoService.criar(any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("falha"))
                .thenReturn(null);

        assertThat(service.verificar(AGORA)).isEqualTo(1);
    }

    @Test
    @DisplayName("não deve consultar ativos quando ninguém tem favoritos com alerta")
    void semFavoritosNaoFazNada() {
        assertThat(service.verificar(AGORA)).isZero();
        verify(ativoFavoritavelService, never()).buscarPorIds(any(), anyCollection());
        verify(notificacaoService, never()).criar(any(), any(), any(), any(), isNull(), any());
    }
}
