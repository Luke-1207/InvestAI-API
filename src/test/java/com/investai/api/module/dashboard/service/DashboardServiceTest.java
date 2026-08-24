package com.investai.api.module.dashboard.service;

import com.investai.api.module.dashboard.dto.*;
import com.investai.api.module.perfil.dto.PerfilResponseDTO;
import com.investai.api.module.perfil.service.PerfilService;
import com.investai.api.shared.event.PerfilAlteradoEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private IndicadoresMercadoSincronizacaoService indicadoresMercadoSincronizacaoService;

    @Mock
    private DashboardSugestaoService dashboardSugestaoService;

    @Mock
    private PerfilService perfilService;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(
                indicadoresMercadoSincronizacaoService, dashboardSugestaoService, perfilService, Runnable::run);
    }

    private void configurarCenarioPadrao(UUID usuarioId) {
        when(indicadoresMercadoSincronizacaoService.obterSnapshotAtual())
                .thenReturn(IndicadoresMercadoResponseDTO.builder().build());
        when(dashboardSugestaoService.sugerirRendaVariavel(usuarioId))
                .thenReturn(SugestoesRendaVariavelResponseDTO.builder().itens(List.of()).build());
        when(dashboardSugestaoService.sugerirRendaFixa(usuarioId))
                .thenReturn(SugestoesRendaFixaResponseDTO.builder().itens(List.of()).build());
        when(perfilService.obterPerfil(usuarioId))
                .thenReturn(PerfilResponseDTO.builder().build());
    }

    @Test
    @DisplayName("obterDashboard - deve agregar indicadores, sugestões e perfil num único DTO")
    void obterDashboard_deveAgregarTudoNumUnicoDTO() {
        UUID usuarioId = UUID.randomUUID();

        IndicadoresMercadoResponseDTO indicadores = IndicadoresMercadoResponseDTO.builder()
                .ibovespaPontos(BigDecimal.valueOf(134820.5)).build();
        SugestoesRendaVariavelResponseDTO rendaVariavel = SugestoesRendaVariavelResponseDTO.builder().itens(List.of()).build();
        SugestoesRendaFixaResponseDTO rendaFixa = SugestoesRendaFixaResponseDTO.builder().itens(List.of()).build();
        PerfilResponseDTO perfil = PerfilResponseDTO.builder().perfilPreenchido(true).build();

        when(indicadoresMercadoSincronizacaoService.obterSnapshotAtual()).thenReturn(indicadores);
        when(dashboardSugestaoService.sugerirRendaVariavel(usuarioId)).thenReturn(rendaVariavel);
        when(dashboardSugestaoService.sugerirRendaFixa(usuarioId)).thenReturn(rendaFixa);
        when(perfilService.obterPerfil(usuarioId)).thenReturn(perfil);

        DashboardResponseDTO resultado = dashboardService.obterDashboard(usuarioId);

        assertThat(resultado.getIndicadoresMercado()).isEqualTo(indicadores);
        assertThat(resultado.getSugestoesRendaVariavel()).isEqualTo(rendaVariavel);
        assertThat(resultado.getSugestoesRendaFixa()).isEqualTo(rendaFixa);
        assertThat(resultado.getPerfil()).isEqualTo(perfil);
        assertThat(resultado.getGeradoEm()).isNotNull();
    }

    @Test
    @DisplayName("obterDashboard - segunda chamada pro mesmo usuário deve usar o cache")
    void obterDashboard_segundaChamada_deveUsarCache() {
        UUID usuarioId = UUID.randomUUID();
        configurarCenarioPadrao(usuarioId);

        dashboardService.obterDashboard(usuarioId);
        dashboardService.obterDashboard(usuarioId);
        dashboardService.obterDashboard(usuarioId);

        verify(perfilService, times(1)).obterPerfil(usuarioId);
    }

    @Test
    @DisplayName("obterDashboard - usuários diferentes devem ter entradas de cache separadas")
    void obterDashboard_usuariosDiferentes_devemTerCachesSeparados() {
        UUID usuario1 = UUID.randomUUID();
        UUID usuario2 = UUID.randomUUID();
        configurarCenarioPadrao(usuario1);
        configurarCenarioPadrao(usuario2);

        dashboardService.obterDashboard(usuario1);
        dashboardService.obterDashboard(usuario2);

        verify(perfilService).obterPerfil(usuario1);
        verify(perfilService).obterPerfil(usuario2);
    }

    @Test
    @DisplayName("aoAlterarPerfil - deve invalidar o cache do usuário, forçando nova agregação")
    void aoAlterarPerfil_deveInvalidarCacheDoUsuario() {
        UUID usuarioId = UUID.randomUUID();
        configurarCenarioPadrao(usuarioId);

        dashboardService.obterDashboard(usuarioId);
        dashboardService.aoAlterarPerfil(new PerfilAlteradoEvent(this, usuarioId));
        dashboardService.obterDashboard(usuarioId);

        verify(perfilService, times(2)).obterPerfil(usuarioId);
    }
}