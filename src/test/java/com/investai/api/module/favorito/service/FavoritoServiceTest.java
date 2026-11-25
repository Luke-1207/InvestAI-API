package com.investai.api.module.favorito.service;

import com.investai.api.infra.exception.BusinessException;
import com.investai.api.infra.exception.ConflictException;
import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.favorito.dto.AdicionarFavoritoRequestDTO;
import com.investai.api.module.favorito.dto.AtivoFavoritavelDTO;
import com.investai.api.module.favorito.dto.ConfigurarAlertasRequestDTO;
import com.investai.api.module.favorito.dto.FavoritoResponseDTO;
import com.investai.api.module.favorito.dto.FavoritoStatusResponseDTO;
import com.investai.api.module.favorito.entity.Favorito;
import com.investai.api.module.favorito.entity.TipoAtivoFavorito;
import com.investai.api.module.favorito.repository.FavoritoRepository;
import com.investai.api.module.notificacao.entity.TipoNotificacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoritoServiceTest {

    @Mock
    private FavoritoRepository favoritoRepository;

    @Mock
    private AtivoFavoritavelService ativoFavoritavelService;

    @InjectMocks
    private FavoritoService favoritoService;

    private final UUID usuarioId = UUID.randomUUID();
    private final UUID acaoId = UUID.randomUUID();
    private AtivoFavoritavelDTO petr4;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(favoritoService, "limitePorUsuario", 50);
        petr4 = new AtivoFavoritavelDTO(acaoId, TipoAtivoFavorito.ACAO_FII_ETF, "PETR4", "Petrobras PN", null);
    }

    private AdicionarFavoritoRequestDTO requestPetr4() {
        return AdicionarFavoritoRequestDTO.builder().tipoAtivo(TipoAtivoFavorito.ACAO_FII_ETF).codigo("petr4").build();
    }

    private Favorito favoritoDoUsuario(UUID id, UUID dono, TipoAtivoFavorito tipo, UUID ativoId) {
        return Favorito.builder().id(id).usuarioId(dono).tipoAtivo(tipo).ativoId(ativoId).criadoEm(LocalDateTime.now()).build();
    }

    @Test
    @DisplayName("adicionar - deve salvar o favorito com alertas ativos e limiar padrão")
    void adicionar_deveSalvarFavorito() {
        when(ativoFavoritavelService.resolver(TipoAtivoFavorito.ACAO_FII_ETF, "petr4", null)).thenReturn(petr4);
        when(favoritoRepository.countByUsuarioId(usuarioId)).thenReturn(3L);
        when(favoritoRepository.saveAndFlush(any(Favorito.class))).thenAnswer(inv -> {
            Favorito f = inv.getArgument(0);
            f.setId(UUID.randomUUID());
            return f;
        });

        FavoritoResponseDTO resposta = favoritoService.adicionar(usuarioId, requestPetr4());

        ArgumentCaptor<Favorito> captor = ArgumentCaptor.forClass(Favorito.class);
        verify(favoritoRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getUsuarioId()).isEqualTo(usuarioId);
        assertThat(captor.getValue().getAtivoId()).isEqualTo(acaoId);
        assertThat(captor.getValue().isAlertasAtivos()).isTrue();
        assertThat(captor.getValue().getLimiarVariacao()).isEqualByComparingTo(Favorito.LIMIAR_VARIACAO_PADRAO);
        assertThat(resposta.getCodigo()).isEqualTo("PETR4");
        assertThat(resposta.getTiposAlertaDisponiveis()).containsExactly(TipoNotificacao.VARIACAO_ALTA, TipoNotificacao.VARIACAO_BAIXA);
    }

    @Test
    @DisplayName("adicionar - deve lançar conflito quando o ativo já é favorito")
    void adicionar_deveLancarConflitoQuandoDuplicado() {
        when(ativoFavoritavelService.resolver(any(), any(), any())).thenReturn(petr4);
        when(favoritoRepository.existsByUsuarioIdAndAtivoIdAndTipoAtivo(usuarioId, acaoId, TipoAtivoFavorito.ACAO_FII_ETF)).thenReturn(true);

        assertThatThrownBy(() -> favoritoService.adicionar(usuarioId, requestPetr4()))
                .isInstanceOf(ConflictException.class);
        verify(favoritoRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("adicionar - deve bloquear quando o usuário atingiu o limite de 50 favoritos")
    void adicionar_deveBloquearNoLimite() {
        when(ativoFavoritavelService.resolver(any(), any(), any())).thenReturn(petr4);
        when(favoritoRepository.countByUsuarioId(usuarioId)).thenReturn(50L);

        assertThatThrownBy(() -> favoritoService.adicionar(usuarioId, requestPetr4()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("limite de 50");
        verify(favoritoRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("adicionar - deve converter violação do índice único em conflito")
    void adicionar_deveConverterViolacaoDeUnicidade() {
        when(ativoFavoritavelService.resolver(any(), any(), any())).thenReturn(petr4);
        when(favoritoRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uk"));

        assertThatThrownBy(() -> favoritoService.adicionar(usuarioId, requestPetr4()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("listar - deve montar a lista com nome e código e ignorar ativos que não existem mais")
    void listar_deveResolverAtivos() {
        UUID tesouroId = UUID.randomUUID();
        Favorito fAcao = favoritoDoUsuario(UUID.randomUUID(), usuarioId, TipoAtivoFavorito.ACAO_FII_ETF, acaoId);
        Favorito fTesouro = favoritoDoUsuario(UUID.randomUUID(), usuarioId, TipoAtivoFavorito.TITULO_TESOURO, tesouroId);
        Favorito fOrfao = favoritoDoUsuario(UUID.randomUUID(), usuarioId, TipoAtivoFavorito.ACAO_FII_ETF, UUID.randomUUID());
        when(favoritoRepository.findByUsuarioIdOrderByCriadoEmDesc(usuarioId)).thenReturn(List.of(fAcao, fTesouro, fOrfao));
        when(ativoFavoritavelService.buscarPorIds(eq(TipoAtivoFavorito.ACAO_FII_ETF), anyCollection())).thenReturn(Map.of(acaoId, petr4));
        when(ativoFavoritavelService.buscarPorIds(eq(TipoAtivoFavorito.TITULO_TESOURO), anyCollection())).thenReturn(Map.of(tesouroId,
                new AtivoFavoritavelDTO(tesouroId, TipoAtivoFavorito.TITULO_TESOURO, "LTN2029", "Tesouro Prefixado 2029", LocalDate.of(2029, 1, 1))));

        List<FavoritoResponseDTO> lista = favoritoService.listar(usuarioId);

        assertThat(lista).extracting(FavoritoResponseDTO::getCodigo).containsExactly("PETR4", "LTN2029");
        assertThat(lista.get(1).getTiposAlertaDisponiveis()).containsExactly(TipoNotificacao.TITULO_VENCENDO);
    }

    @Test
    @DisplayName("status - deve indicar se o ativo está favoritado")
    void status_deveIndicarFavoritado() {
        UUID favoritoId = UUID.randomUUID();
        when(ativoFavoritavelService.resolver(TipoAtivoFavorito.ACAO_FII_ETF, "PETR4", null)).thenReturn(petr4);
        when(favoritoRepository.findByUsuarioIdAndAtivoIdAndTipoAtivo(usuarioId, acaoId, TipoAtivoFavorito.ACAO_FII_ETF))
                .thenReturn(Optional.of(favoritoDoUsuario(favoritoId, usuarioId, TipoAtivoFavorito.ACAO_FII_ETF, acaoId)));

        FavoritoStatusResponseDTO status = favoritoService.status(usuarioId, TipoAtivoFavorito.ACAO_FII_ETF, "PETR4", null);

        assertThat(status.isFavoritado()).isTrue();
        assertThat(status.getFavoritoId()).isEqualTo(favoritoId);
    }

    @Test
    @DisplayName("status - deve indicar não favoritado")
    void status_deveIndicarNaoFavoritado() {
        when(ativoFavoritavelService.resolver(any(), any(), any())).thenReturn(petr4);
        when(favoritoRepository.findByUsuarioIdAndAtivoIdAndTipoAtivo(any(), any(), any())).thenReturn(Optional.empty());

        assertThat(favoritoService.status(usuarioId, TipoAtivoFavorito.ACAO_FII_ETF, "PETR4", null).isFavoritado()).isFalse();
    }

    @Test
    @DisplayName("remover - deve excluir o favorito do próprio usuário")
    void remover_deveExcluir() {
        UUID favoritoId = UUID.randomUUID();
        Favorito favorito = favoritoDoUsuario(favoritoId, usuarioId, TipoAtivoFavorito.ACAO_FII_ETF, acaoId);
        when(favoritoRepository.findById(favoritoId)).thenReturn(Optional.of(favorito));

        favoritoService.remover(usuarioId, favoritoId);

        verify(favoritoRepository).delete(favorito);
    }

    @Test
    @DisplayName("remover - deve retornar não encontrado para favorito de outro usuário")
    void remover_deveBloquearFavoritoDeOutroUsuario() {
        UUID favoritoId = UUID.randomUUID();
        when(favoritoRepository.findById(favoritoId))
                .thenReturn(Optional.of(favoritoDoUsuario(favoritoId, UUID.randomUUID(), TipoAtivoFavorito.ACAO_FII_ETF, acaoId)));

        assertThatThrownBy(() -> favoritoService.remover(usuarioId, favoritoId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(favoritoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("configurarAlertas - deve atualizar tipos e limiar")
    void configurarAlertas_deveAtualizar() {
        UUID favoritoId = UUID.randomUUID();
        Favorito favorito = favoritoDoUsuario(favoritoId, usuarioId, TipoAtivoFavorito.ACAO_FII_ETF, acaoId);
        when(favoritoRepository.findById(favoritoId)).thenReturn(Optional.of(favorito));
        when(favoritoRepository.save(favorito)).thenReturn(favorito);
        when(ativoFavoritavelService.buscarPorIds(eq(TipoAtivoFavorito.ACAO_FII_ETF), anyCollection())).thenReturn(Map.of(acaoId, petr4));

        ConfigurarAlertasRequestDTO dto = ConfigurarAlertasRequestDTO.builder()
                .alertasAtivos(true)
                .tiposAlertaAtivos(List.of(TipoNotificacao.VARIACAO_BAIXA, TipoNotificacao.VARIACAO_BAIXA))
                .limiarVariacao(new BigDecimal("3.50"))
                .build();

        FavoritoResponseDTO resposta = favoritoService.configurarAlertas(usuarioId, favoritoId, dto);

        assertThat(favorito.getTiposAlertaAtivos()).containsExactly(TipoNotificacao.VARIACAO_BAIXA);
        assertThat(favorito.getLimiarVariacao()).isEqualByComparingTo("3.50");
        assertThat(resposta.getTiposAlertaAtivos()).containsExactly(TipoNotificacao.VARIACAO_BAIXA);
    }

    @Test
    @DisplayName("configurarAlertas - deve rejeitar tipo de alerta que não se aplica ao ativo")
    void configurarAlertas_deveRejeitarTipoInvalido() {
        UUID favoritoId = UUID.randomUUID();
        Favorito favorito = favoritoDoUsuario(favoritoId, usuarioId, TipoAtivoFavorito.TITULO_TESOURO, UUID.randomUUID());
        when(favoritoRepository.findById(favoritoId)).thenReturn(Optional.of(favorito));

        ConfigurarAlertasRequestDTO dto = ConfigurarAlertasRequestDTO.builder()
                .alertasAtivos(true)
                .tiposAlertaAtivos(List.of(TipoNotificacao.VARIACAO_ALTA))
                .build();

        assertThatThrownBy(() -> favoritoService.configurarAlertas(usuarioId, favoritoId, dto))
                .isInstanceOf(BusinessException.class);
        verify(favoritoRepository, never()).save(any());
    }

    @Test
    @DisplayName("configurarAlertas - deve permitir desligar os alertas mantendo o limiar")
    void configurarAlertas_deveDesligar() {
        UUID favoritoId = UUID.randomUUID();
        Favorito favorito = favoritoDoUsuario(favoritoId, usuarioId, TipoAtivoFavorito.ACAO_FII_ETF, acaoId);
        favorito.setLimiarVariacao(new BigDecimal("7.00"));
        when(favoritoRepository.findById(favoritoId)).thenReturn(Optional.of(favorito));
        when(favoritoRepository.save(favorito)).thenReturn(favorito);
        when(ativoFavoritavelService.buscarPorIds(any(), anyCollection())).thenReturn(Map.of(acaoId, petr4));

        favoritoService.configurarAlertas(usuarioId, favoritoId, ConfigurarAlertasRequestDTO.builder().alertasAtivos(false).build());

        assertThat(favorito.isAlertasAtivos()).isFalse();
        assertThat(favorito.getTiposAlertaAtivos()).isNull();
        assertThat(favorito.getLimiarVariacao()).isEqualByComparingTo("7.00");
    }
}
