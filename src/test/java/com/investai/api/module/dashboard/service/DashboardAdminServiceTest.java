package com.investai.api.module.dashboard.service;

import com.investai.api.infra.ia.IaHealthClient;
import com.investai.api.infra.ia.dto.IaHealthStatusDTO;
import com.investai.api.module.ativo.entity.TipoAtivo;
import com.investai.api.module.ativo.repository.AcaoRepository;
import com.investai.api.module.auth.repository.UsuarioRepository;
import com.investai.api.module.dashboard.dto.DashboardAdminResponseDTO;
import com.investai.api.module.perfil.repository.PerfilInvestidorRepository;
import com.investai.api.module.rendafixa.entity.TipoTituloPrivado;
import com.investai.api.module.rendafixa.entity.TituloTesouro;
import com.investai.api.module.rendafixa.repository.TituloPrivadoRepository;
import com.investai.api.module.rendafixa.repository.TituloTesouroRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardAdminServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PerfilInvestidorRepository perfilInvestidorRepository;

    @Mock
    private AcaoRepository acaoRepository;

    @Mock
    private TituloTesouroRepository tituloTesouroRepository;

    @Mock
    private TituloPrivadoRepository tituloPrivadoRepository;

    @Mock
    private IaHealthClient iaHealthClient;

    @InjectMocks
    private DashboardAdminService dashboardAdminService;

    private void configurarCenarioPadrao() {
        when(usuarioRepository.countByDeletadoEmIsNull()).thenReturn(100L);
        when(perfilInvestidorRepository.countByPerfilPreenchidoTrue()).thenReturn(80L);
        when(perfilInvestidorRepository.countByPerfilPreenchidoTrueAndPerfilRisco(any())).thenReturn(10L);
        when(acaoRepository.countByAtivoTrueAndTipo(any(TipoAtivo.class))).thenReturn(5L);
        when(tituloPrivadoRepository.countByAtivoTrueAndTipo(any(TipoTituloPrivado.class))).thenReturn(3L);
        when(tituloTesouroRepository.countByDisponivelTrue()).thenReturn(7L);
        when(tituloTesouroRepository.findTopByOrderBySincronizadoEmDesc()).thenReturn(Optional.empty());
        when(tituloPrivadoRepository.countByAtivoTrueAndVencimentoBetween(any(), any())).thenReturn(2L);
        when(iaHealthClient.verificarStatus()).thenReturn(IaHealthStatusDTO.builder().disponivel(true).rabbitmqConectado(true).build());
    }

    @Test
    @DisplayName("obterMetricasAdmin - deve agregar todas as métricas corretamente")
    void obterMetricasAdmin_deveAgregarTodasAsMetricas() {
        configurarCenarioPadrao();

        DashboardAdminResponseDTO resultado = dashboardAdminService.obterMetricasAdmin();

        assertThat(resultado.getTotalUsuarios()).isEqualTo(100L);
        assertThat(resultado.getUsuariosComPerfilPreenchido()).isEqualTo(80L);
        assertThat(resultado.getDistribuicaoRisco()).hasSize(3); // CONSERVADOR, MODERADO, ARROJADO
        assertThat(resultado.getDistribuicaoAtivosPorCategoria()).containsKeys("ACAO", "FII", "ETF", "TESOURO", "CDB", "LCI", "LCA");
        assertThat(resultado.getTitulosVencendoEm30Dias()).isEqualTo(2L);
        assertThat(resultado.isIaDisponivel()).isTrue();
        assertThat(resultado.getGeradoEm()).isNotNull();
    }

    @Test
    @DisplayName("obterMetricasAdmin - deve retornar ultimaSincronizacaoTesouro nula quando nenhum título foi sincronizado ainda")
    void obterMetricasAdmin_deveRetornarUltimaSincronizacaoNulaQuandoNenhumTituloSincronizado() {
        configurarCenarioPadrao();

        DashboardAdminResponseDTO resultado = dashboardAdminService.obterMetricasAdmin();

        assertThat(resultado.getUltimaSincronizacaoTesouro()).isNull();
    }

    @Test
    @DisplayName("obterMetricasAdmin - deve retornar ultimaSincronizacaoTesouro quando existe título sincronizado")
    void obterMetricasAdmin_deveRetornarUltimaSincronizacaoQuandoExisteTitulo() {
        configurarCenarioPadrao();
        LocalDateTime sincronizadoEm = LocalDateTime.now().minusMinutes(10);
        TituloTesouro tituloRecente = TituloTesouro.builder().id(UUID.randomUUID()).sincronizadoEm(sincronizadoEm).build();
        when(tituloTesouroRepository.findTopByOrderBySincronizadoEmDesc()).thenReturn(Optional.of(tituloRecente));

        DashboardAdminResponseDTO resultado = dashboardAdminService.obterMetricasAdmin();

        assertThat(resultado.getUltimaSincronizacaoTesouro()).isEqualTo(sincronizadoEm);
    }

    @Test
    @DisplayName("obterMetricasAdmin - não deve quebrar quando IA está indisponível")
    void obterMetricasAdmin_naoDeveQuebrarQuandoIaIndisponivel() {
        configurarCenarioPadrao();
        when(iaHealthClient.verificarStatus()).thenReturn(IaHealthStatusDTO.builder().disponivel(false).rabbitmqConectado(null).build());

        DashboardAdminResponseDTO resultado = dashboardAdminService.obterMetricasAdmin();

        assertThat(resultado.isIaDisponivel()).isFalse();
        assertThat(resultado.getIaRabbitmqConectado()).isNull();
    }

    @Test
    @DisplayName("obterMetricasAdmin - segunda chamada deve usar o cache, sem consultar os repositories de novo")
    void obterMetricasAdmin_segundaChamada_deveUsarCache() {
        configurarCenarioPadrao();

        dashboardAdminService.obterMetricasAdmin();
        dashboardAdminService.obterMetricasAdmin();
        dashboardAdminService.obterMetricasAdmin();

        verify(usuarioRepository, times(1)).countByDeletadoEmIsNull();
        verify(iaHealthClient, times(1)).verificarStatus();
    }
}