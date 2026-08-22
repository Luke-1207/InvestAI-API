package com.investai.api.module.rendafixa.service;

import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.infra.rabbitmq.IaMensagemPublisher;
import com.investai.api.infra.rabbitmq.dto.*;
import com.investai.api.module.perfil.entity.PerfilInvestidor;
import com.investai.api.module.perfil.repository.PerfilInvestidorRepository;
import com.investai.api.module.rendafixa.dto.CategoriaRendaFixa;
import com.investai.api.module.rendafixa.dto.RendaFixaListagemResponseDTO;
import com.investai.api.module.rendafixa.entity.*;
import com.investai.api.module.rendafixa.repository.TituloPrivadoRepository;
import com.investai.api.module.rendafixa.repository.TituloTesouroRepository;
import com.investai.api.shared.event.PerfilAlteradoEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RendaFixaUnificadaServiceTest {

    @Mock
    private TituloTesouroRepository tituloTesouroRepository;

    @Mock
    private TituloPrivadoRepository tituloPrivadoRepository;

    @Mock
    private PerfilInvestidorRepository perfilInvestidorRepository;

    @Mock
    private IaMensagemPublisher iaMensagemPublisher;

    @InjectMocks
    private RendaFixaUnificadaService rendaFixaUnificadaService;

    private TituloTesouro criarTesouro(String nome) {
        return TituloTesouro.builder()
                .id(UUID.randomUUID())
                .codigo(nome.toLowerCase().replace(" ", "-"))
                .nome(nome)
                .tipo(TipoTesouro.SELIC)
                .taxaAnual(BigDecimal.valueOf(0.08))
                .precoMinimo(BigDecimal.valueOf(150))
                .vencimento(LocalDate.now().plusYears(3))
                .pagaJurosSemestrais(false)
                .disponivel(true)
                .build();
    }

    private TituloPrivado criarPrivado(TipoTituloPrivado tipo, String emissor) {
        return TituloPrivado.builder()
                .id(UUID.randomUUID())
                .tipo(tipo)
                .emissor(emissor)
                .indexador(Indexador.CDI)
                .taxaPercentual(BigDecimal.valueOf(105))
                .vencimento(LocalDate.now().plusYears(2))
                .investimentoMinimo(BigDecimal.valueOf(500))
                .liquidez(TipoLiquidez.DIARIA)
                .garantidoFgc(true)
                .isentoIr(tipo != TipoTituloPrivado.CDB)
                .ativo(true)
                .build();
    }

    @Test
    @DisplayName("listar - modo livre deve unificar Tesouro e privados sem chamar a IA")
    void listar_modoLivre_deveUnificarSemChamarIA() {
        TituloTesouro tesouro = criarTesouro("Tesouro Selic 2029");
        TituloPrivado cdb = criarPrivado(TipoTituloPrivado.CDB, "Banco Inter");
        TituloPrivado lci = criarPrivado(TipoTituloPrivado.LCI, "Banco Alfa");

        when(tituloTesouroRepository.findByDisponivelTrue()).thenReturn(List.of(tesouro));
        when(tituloPrivadoRepository.findByAtivoTrue()).thenReturn(List.of(cdb, lci));

        List<RendaFixaListagemResponseDTO> resultado = rendaFixaUnificadaService.listar("livre", UUID.randomUUID());

        assertThat(resultado).hasSize(3);
        assertThat(resultado).allMatch(item -> item.getScore() == null);
        assertThat(resultado).extracting(RendaFixaListagemResponseDTO::getCategoria)
                .containsExactlyInAnyOrder(CategoriaRendaFixa.TESOURO, CategoriaRendaFixa.CDB, CategoriaRendaFixa.LCI);

        verifyNoInteractions(perfilInvestidorRepository, iaMensagemPublisher);
    }

    @Test
    @DisplayName("listar - modo livre deve marcar garantidoFgc=false e isentoIr=false para Tesouro")
    void listar_modoLivre_deveMarcarFlagsCorretasParaTesouro() {
        TituloTesouro tesouro = criarTesouro("Tesouro IPCA+ 2035");
        when(tituloTesouroRepository.findByDisponivelTrue()).thenReturn(List.of(tesouro));
        when(tituloPrivadoRepository.findByAtivoTrue()).thenReturn(List.of());

        List<RendaFixaListagemResponseDTO> resultado = rendaFixaUnificadaService.listar("livre", UUID.randomUUID());

        assertThat(resultado.get(0).isGarantidoFgc()).isFalse();
        assertThat(resultado.get(0).isIsentoIr()).isFalse();
        assertThat(resultado.get(0).getLiquidez()).isEqualTo("DIARIA");
    }

    @Test
    @DisplayName("listar - modo inteligente deve aplicar score retornado pela IA e ordenar decrescente")
    void listar_modoInteligente_deveAplicarScoreEOrdenar() {
        UUID usuarioId = UUID.randomUUID();
        TituloTesouro tesouro = criarTesouro("Tesouro Selic 2029");
        TituloPrivado cdb = criarPrivado(TipoTituloPrivado.CDB, "Banco Inter");

        when(tituloTesouroRepository.findByDisponivelTrue()).thenReturn(List.of(tesouro));
        when(tituloPrivadoRepository.findByAtivoTrue()).thenReturn(List.of(cdb));

        PerfilInvestidor perfil = PerfilInvestidor.builder()
                .id(UUID.randomUUID())
                .perfilRisco("MODERADO")
                .horizonte("LONGO_PRAZO")
                .objetivo("RENDA_PASSIVA")
                .valorDisponivel(BigDecimal.valueOf(5000))
                .tiposAceitos(List.of())
                .setoresPreferidos(List.of())
                .build();

        when(perfilInvestidorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(perfil));

        RankingResponseDTO resposta = RankingResponseDTO.builder()
                .correlationId("qualquer")
                .ativos(List.of(
                        AtivoRankeadoDTO.builder().codigo(tesouro.getId().toString()).score(60).compatibilidade(Compatibilidade.MEDIA).justificativa("ok").build(),
                        AtivoRankeadoDTO.builder().codigo(cdb.getId().toString()).score(92).compatibilidade(Compatibilidade.ALTA).justificativa("ótimo").build()
                ))
                .build();

        when(iaMensagemPublisher.enviarRankingEAguardar(eq(ModuloIa.FIXA), any(), any())).thenReturn(resposta);

        List<RendaFixaListagemResponseDTO> resultado = rendaFixaUnificadaService.listar("inteligente", usuarioId);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getId()).isEqualTo(cdb.getId());
        assertThat(resultado.get(0).getScore()).isEqualTo(92);
        assertThat(resultado.get(0).getCompatibilidade()).isEqualTo(Compatibilidade.ALTA);
        assertThat(resultado.get(1).getScore()).isEqualTo(60);
    }

    @Test
    @DisplayName("listar - modo inteligente deve montar o mapa de ativos com os nomes de campo certos pro schema Python")
    void listar_modoInteligente_deveMontarMapaComNomesDeCampoCorretos() {
        UUID usuarioId = UUID.randomUUID();
        TituloTesouro tesouro = criarTesouro("Tesouro Selic 2029");

        when(tituloTesouroRepository.findByDisponivelTrue()).thenReturn(List.of(tesouro));
        when(tituloPrivadoRepository.findByAtivoTrue()).thenReturn(List.of());

        PerfilInvestidor perfil = PerfilInvestidor.builder()
                .id(UUID.randomUUID())
                .perfilRisco("CONSERVADOR")
                .horizonte("CURTO_PRAZO")
                .objetivo("PRESERVAR_CAPITAL")
                .tiposAceitos(List.of())
                .setoresPreferidos(List.of())
                .build();

        when(perfilInvestidorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(perfil));
        when(iaMensagemPublisher.enviarRankingEAguardar(any(), any(), any()))
                .thenReturn(RankingResponseDTO.builder().ativos(List.of()).build());

        rendaFixaUnificadaService.listar("inteligente", usuarioId);

        ArgumentCaptor<List<Map<String, Object>>> captor = ArgumentCaptor.forClass(List.class);
        verify(iaMensagemPublisher).enviarRankingEAguardar(eq(ModuloIa.FIXA), any(), captor.capture());

        Map<String, Object> ativoEnviado = captor.getValue().get(0);
        assertThat(ativoEnviado.get("codigo")).isEqualTo(tesouro.getId().toString());
        assertThat(ativoEnviado.get("nome")).isEqualTo(tesouro.getNome());
        assertThat(ativoEnviado.get("tipo")).isEqualTo("TESOURO");
        assertThat(ativoEnviado.get("taxaPercentual")).isEqualTo(tesouro.getTaxaAnual());
        assertThat(ativoEnviado.get("investimentoMinimo")).isEqualTo(tesouro.getPrecoMinimo());
        assertThat(ativoEnviado.get("indexador")).isEqualTo(tesouro.getTipo().name());
        assertThat(ativoEnviado.get("liquidez")).isEqualTo("DIARIA");
        assertThat(ativoEnviado.get("isentoIR")).isEqualTo(false);
        assertThat(ativoEnviado.get("garantidoFGC")).isEqualTo(false);
        assertThat(ativoEnviado.containsKey("categoria")).isFalse();
        assertThat(ativoEnviado.containsKey("taxa")).isFalse();
        assertThat(ativoEnviado.containsKey("valorMinimo")).isFalse();
    }

    @Test
    @DisplayName("listar - modo inteligente deve lançar exceção quando perfil não encontrado")
    void listar_modoInteligente_deveLancarExcecaoQuandoPerfilNaoEncontrado() {
        UUID usuarioId = UUID.randomUUID();
        when(tituloTesouroRepository.findByDisponivelTrue()).thenReturn(List.of());
        when(tituloPrivadoRepository.findByAtivoTrue()).thenReturn(List.of());
        when(perfilInvestidorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rendaFixaUnificadaService.listar("inteligente", usuarioId))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(iaMensagemPublisher);
    }

    @Test
    @DisplayName("listar - modo inteligente deve chamar a IA na primeira vez")
    void listar_modoInteligente_primeiraChamada_deveChamarIA() {
        UUID usuarioId = UUID.randomUUID();
        configurarCenarioPadraoInteligente(usuarioId);

        rendaFixaUnificadaService.listar("inteligente", usuarioId);

        verify(iaMensagemPublisher, times(1)).enviarRankingEAguardar(any(), any(), any());
    }

    @Test
    @DisplayName("listar - modo inteligente na segunda chamada pro mesmo usuário deve usar o cache, sem chamar a IA de novo")
    void listar_modoInteligente_segundaChamada_naoDeveChamarIANovamente() {
        UUID usuarioId = UUID.randomUUID();
        configurarCenarioPadraoInteligente(usuarioId);

        rendaFixaUnificadaService.listar("inteligente", usuarioId);
        rendaFixaUnificadaService.listar("inteligente", usuarioId);
        rendaFixaUnificadaService.listar("inteligente", usuarioId);

        verify(iaMensagemPublisher, times(1)).enviarRankingEAguardar(any(), any(), any());
    }

    @Test
    @DisplayName("listar - modo inteligente deve ter cache separado por usuário")
    void listar_modoInteligente_usuariosDiferentes_devemTerCachesSeparados() {
        UUID usuario1 = UUID.randomUUID();
        UUID usuario2 = UUID.randomUUID();
        configurarCenarioPadraoInteligente(usuario1);
        configurarCenarioPadraoInteligente(usuario2);

        rendaFixaUnificadaService.listar("inteligente", usuario1);
        rendaFixaUnificadaService.listar("inteligente", usuario2);

        verify(iaMensagemPublisher, times(2)).enviarRankingEAguardar(any(), any(), any());
    }

    @Test
    @DisplayName("aoAlterarPerfil - deve invalidar o cache do usuário, forçando nova chamada à IA")
    void aoAlterarPerfil_deveInvalidarCacheDoUsuario() {
        UUID usuarioId = UUID.randomUUID();
        configurarCenarioPadraoInteligente(usuarioId);

        rendaFixaUnificadaService.listar("inteligente", usuarioId);
        rendaFixaUnificadaService.aoAlterarPerfil(new PerfilAlteradoEvent(this, usuarioId));
        rendaFixaUnificadaService.listar("inteligente", usuarioId);

        verify(iaMensagemPublisher, times(2)).enviarRankingEAguardar(any(), any(), any());
    }

    private void configurarCenarioPadraoInteligente(UUID usuarioId) {
        when(tituloTesouroRepository.findByDisponivelTrue()).thenReturn(List.of(criarTesouro("Tesouro Selic 2029")));
        when(tituloPrivadoRepository.findByAtivoTrue()).thenReturn(List.of());

        PerfilInvestidor perfil = PerfilInvestidor.builder()
                .id(UUID.randomUUID())
                .perfilRisco("CONSERVADOR").horizonte("CURTO_PRAZO").objetivo("PRESERVAR_CAPITAL")
                .tiposAceitos(List.of()).setoresPreferidos(List.of())
                .build();

        when(perfilInvestidorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(perfil));
        when(iaMensagemPublisher.enviarRankingEAguardar(any(), any(), any()))
                .thenReturn(RankingResponseDTO.builder().ativos(List.of()).build());
    }
}