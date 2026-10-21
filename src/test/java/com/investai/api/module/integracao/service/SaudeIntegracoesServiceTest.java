package com.investai.api.module.integracao.service;

import com.investai.api.infra.hgbrasil.HgBrasilClient;
import com.investai.api.infra.hgbrasil.HgBrasilMockClient;
import com.investai.api.infra.ia.IaHealthClient;
import com.investai.api.infra.ia.dto.IaHealthStatusDTO;
import com.investai.api.infra.ia.dto.IaLlmHealthDTO;
import com.investai.api.infra.tesourodireto.TesouroDiretoClient;
import com.investai.api.module.dashboard.service.IndicadoresMercadoSincronizacaoService;
import com.investai.api.module.integracao.dto.SaudeIntegracoesResponseDTO;
import com.investai.api.module.integracao.dto.SituacaoIntegracao;
import com.investai.api.module.integracao.dto.StatusIntegracaoDTO;
import com.investai.api.module.integracao.entity.Integracao;
import com.investai.api.module.integracao.entity.StatusIntegracao;
import com.investai.api.module.rendafixa.service.TesouroDiretoSincronizacaoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SaudeIntegracoesServiceTest {

    private final IaHealthClient iaHealthClient = mock(IaHealthClient.class);
    private final MonitorIntegracoesService monitor = mock(MonitorIntegracoesService.class);
    private final IndicadoresMercadoSincronizacaoService indicadores = mock(IndicadoresMercadoSincronizacaoService.class);
    private final TesouroDiretoSincronizacaoService tesouro = mock(TesouroDiretoSincronizacaoService.class);
    private final LocalDateTime agora = LocalDateTime.now();

    private SaudeIntegracoesService servico(HgBrasilClient hgBrasil) {
        return new SaudeIntegracoesService(iaHealthClient, monitor, hgBrasil, mock(TesouroDiretoClient.class),
                indicadores, tesouro, Runnable::run);
    }

    private IaHealthStatusDTO iaNoAr() {
        return IaHealthStatusDTO.builder().disponivel(true).rabbitmqConectado(true).llm(llm(true, null)).build();
    }

    private IaLlmHealthDTO llm(boolean disponivel, String detalhe) {
        return IaLlmHealthDTO.builder().disponivel(disponivel).provider("groq").modelo("openai/gpt-oss-120b").detalhe(detalhe).build();
    }

    private StatusIntegracaoDTO item(SaudeIntegracoesResponseDTO resposta, Integracao integracao) {
        return resposta.getIntegracoes().stream().filter(i -> i.getChave() == integracao).findFirst().orElseThrow();
    }

    private void comRegistros(Map<Integracao, StatusIntegracao> registros) {
        when(monitor.listar()).thenReturn(registros);
    }

    @Test
    @DisplayName("verificar - deve trazer as seis integrações na ordem, com nome")
    void verificar_deveTrazerTodasAsIntegracoes() {
        when(iaHealthClient.verificarStatus()).thenReturn(IaHealthStatusDTO.builder().disponivel(true).rabbitmqConectado(true).llm(llm(true, null)).build());
        comRegistros(new EnumMap<>(Integracao.class));

        SaudeIntegracoesResponseDTO resposta = servico(mock(HgBrasilClient.class)).verificar();

        assertThat(resposta.getIntegracoes()).extracting(StatusIntegracaoDTO::getNome).containsExactly(
                "Microsserviço de IA", "RabbitMQ", "LLM (Groq)", "HG Brasil", "Tesouro Direto", "Banco Central");
        assertThat(resposta.getVerificadoEm()).isNotNull();
    }

    @Test
    @DisplayName("verificar - IA, RabbitMQ e LLM no ar devem aparecer operacionais e ter o sucesso registrado")
    void verificar_tudoNoAr_deveRegistrarSucessos() {
        when(iaHealthClient.verificarStatus()).thenReturn(IaHealthStatusDTO.builder().disponivel(true).rabbitmqConectado(true).llm(llm(true, null)).build());
        comRegistros(new EnumMap<>(Integracao.class));

        SaudeIntegracoesResponseDTO resposta = servico(mock(HgBrasilClient.class)).verificar();

        assertThat(item(resposta, Integracao.MICROSSERVICO_IA).getSituacao()).isEqualTo(SituacaoIntegracao.OPERACIONAL);
        assertThat(item(resposta, Integracao.RABBITMQ).getSituacao()).isEqualTo(SituacaoIntegracao.OPERACIONAL);
        assertThat(item(resposta, Integracao.LLM).getSituacao()).isEqualTo(SituacaoIntegracao.OPERACIONAL);
        assertThat(item(resposta, Integracao.LLM).getDetalhe()).contains("openai/gpt-oss-120b");
        verify(monitor).registrarSucesso(Integracao.MICROSSERVICO_IA);
        verify(monitor).registrarSucesso(Integracao.RABBITMQ);
        verify(monitor).registrarSucesso(Integracao.LLM);
    }

    @Test
    @DisplayName("verificar - LLM e RabbitMQ com problema devem aparecer fora do ar com o motivo")
    void verificar_llmERabbitComProblema_deveRegistrarFalhas() {
        when(iaHealthClient.verificarStatus()).thenReturn(IaHealthStatusDTO.builder().disponivel(true).rabbitmqConectado(false)
                .llm(llm(false, "O provedor recusou a chave de API")).build());
        comRegistros(new EnumMap<>(Integracao.class));

        SaudeIntegracoesResponseDTO resposta = servico(mock(HgBrasilClient.class)).verificar();

        assertThat(item(resposta, Integracao.RABBITMQ).getSituacao()).isEqualTo(SituacaoIntegracao.FORA_DO_AR);
        assertThat(item(resposta, Integracao.LLM).getSituacao()).isEqualTo(SituacaoIntegracao.FORA_DO_AR);
        assertThat(item(resposta, Integracao.LLM).getDetalhe()).isEqualTo("O provedor recusou a chave de API");
        verify(monitor).registrarFalha(Integracao.LLM, "O provedor recusou a chave de API");
        verify(monitor).registrarFalha(any(), org.mockito.ArgumentMatchers.contains("RabbitMQ"));
    }

    @Test
    @DisplayName("verificar - IA fora do ar deixa RabbitMQ e LLM como desconhecidos, sem registrar nada para eles")
    void verificar_iaForaDoAr_deveDeixarDependentesDesconhecidos() {
        when(iaHealthClient.verificarStatus()).thenReturn(IaHealthStatusDTO.builder().disponivel(false).build());
        comRegistros(new EnumMap<>(Integracao.class));

        SaudeIntegracoesResponseDTO resposta = servico(mock(HgBrasilClient.class)).verificar();

        assertThat(item(resposta, Integracao.MICROSSERVICO_IA).getSituacao()).isEqualTo(SituacaoIntegracao.FORA_DO_AR);
        assertThat(item(resposta, Integracao.RABBITMQ).getSituacao()).isEqualTo(SituacaoIntegracao.DESCONHECIDA);
        assertThat(item(resposta, Integracao.LLM).getSituacao()).isEqualTo(SituacaoIntegracao.DESCONHECIDA);
        assertThat(item(resposta, Integracao.LLM).getDetalhe()).isEqualTo(SaudeIntegracoesService.DETALHE_SEM_IA);
        verify(monitor).registrarFalha(any(), any());
        verify(monitor, never()).registrarSucesso(any());
    }

    @Test
    @DisplayName("verificar - IA antiga que não informa o LLM deve deixá-lo como desconhecido")
    void verificar_iaSemInformacaoDoLlm_deveDeixarDesconhecido() {
        when(iaHealthClient.verificarStatus()).thenReturn(IaHealthStatusDTO.builder().disponivel(true).rabbitmqConectado(true).build());
        comRegistros(new EnumMap<>(Integracao.class));

        StatusIntegracaoDTO llm = item(servico(mock(HgBrasilClient.class)).verificar(), Integracao.LLM);

        assertThat(llm.getSituacao()).isEqualTo(SituacaoIntegracao.DESCONHECIDA);
        assertThat(llm.getDetalhe()).contains("ainda não informa");
    }

    @Test
    @DisplayName("verificar - deve trazer as datas registradas e marcar a HG Brasil como simulada quando usa o mock")
    void verificar_deveTrazerDatasEModoSimulado() {
        when(iaHealthClient.verificarStatus()).thenReturn(IaHealthStatusDTO.builder().disponivel(true).rabbitmqConectado(true).llm(llm(true, null)).build());
        Map<Integracao, StatusIntegracao> registros = new EnumMap<>(Integracao.class);
        LocalDateTime sucesso = agora.minusMinutes(10);
        registros.put(Integracao.HG_BRASIL, StatusIntegracao.builder().integracao(Integracao.HG_BRASIL).ultimoSucessoEm(sucesso).build());
        comRegistros(registros);

        SaudeIntegracoesResponseDTO resposta = servico(mock(HgBrasilMockClient.class)).verificar();

        StatusIntegracaoDTO hg = item(resposta, Integracao.HG_BRASIL);
        assertThat(hg.isSimulado()).isTrue();
        assertThat(hg.getSituacao()).isEqualTo(SituacaoIntegracao.OPERACIONAL);
        assertThat(hg.getUltimoSucessoEm()).isEqualTo(sucesso);
        assertThat(hg.getDetalhe()).isEqualTo(SaudeIntegracoesService.DETALHE_SIMULADO);
        assertThat(item(resposta, Integracao.TESOURO_DIRETO).isSimulado()).isFalse();
        assertThat(item(resposta, Integracao.TESOURO_DIRETO).getSituacao()).isEqualTo(SituacaoIntegracao.SEM_DADOS);
    }

    @Test
    @DisplayName("situacaoPorSincronizacao - deve considerar a última falha, a falta de dados e o tempo sem sucesso")
    void situacaoPorSincronizacao_deveSeguirAsRegras() {
        StatusIntegracao emDia = StatusIntegracao.builder().ultimoSucessoEm(agora.minusHours(2)).build();
        StatusIntegracao falhouDepois = StatusIntegracao.builder().ultimoSucessoEm(agora.minusHours(3)).ultimaFalhaEm(agora.minusHours(1)).build();
        StatusIntegracao voltou = StatusIntegracao.builder().ultimoSucessoEm(agora.minusMinutes(5)).ultimaFalhaEm(agora.minusHours(1)).build();
        StatusIntegracao antigo = StatusIntegracao.builder().ultimoSucessoEm(agora.minusHours(27)).build();
        StatusIntegracao soFalhas = StatusIntegracao.builder().ultimaFalhaEm(agora.minusHours(1)).build();

        assertThat(SaudeIntegracoesService.situacaoPorSincronizacao(Integracao.BANCO_CENTRAL, emDia, agora)).isEqualTo(SituacaoIntegracao.OPERACIONAL);
        assertThat(SaudeIntegracoesService.situacaoPorSincronizacao(Integracao.BANCO_CENTRAL, falhouDepois, agora)).isEqualTo(SituacaoIntegracao.FORA_DO_AR);
        assertThat(SaudeIntegracoesService.situacaoPorSincronizacao(Integracao.BANCO_CENTRAL, voltou, agora)).isEqualTo(SituacaoIntegracao.OPERACIONAL);
        assertThat(SaudeIntegracoesService.situacaoPorSincronizacao(Integracao.BANCO_CENTRAL, antigo, agora)).isEqualTo(SituacaoIntegracao.DESATUALIZADA);
        assertThat(SaudeIntegracoesService.situacaoPorSincronizacao(Integracao.TESOURO_DIRETO, antigo, agora)).isEqualTo(SituacaoIntegracao.OPERACIONAL);
        assertThat(SaudeIntegracoesService.situacaoPorSincronizacao(Integracao.BANCO_CENTRAL, soFalhas, agora)).isEqualTo(SituacaoIntegracao.FORA_DO_AR);
        assertThat(SaudeIntegracoesService.situacaoPorSincronizacao(Integracao.BANCO_CENTRAL, null, agora)).isEqualTo(SituacaoIntegracao.SEM_DADOS);
    }

    @Test
    @DisplayName("verificar - integração desatualizada deve explicar quanto tempo é aceitável")
    void verificar_desatualizada_deveExplicarLimite() {
        when(iaHealthClient.verificarStatus()).thenReturn(IaHealthStatusDTO.builder().disponivel(true).rabbitmqConectado(true).llm(llm(true, null)).build());
        Map<Integracao, StatusIntegracao> registros = new EnumMap<>(Integracao.class);
        registros.put(Integracao.BANCO_CENTRAL, StatusIntegracao.builder().ultimoSucessoEm(agora.minusDays(3)).build());
        comRegistros(registros);

        StatusIntegracaoDTO bcb = item(servico(mock(HgBrasilClient.class)).verificar(), Integracao.BANCO_CENTRAL);

        assertThat(bcb.getSituacao()).isEqualTo(SituacaoIntegracao.DESATUALIZADA);
        assertThat(bcb.getDetalhe()).contains("26 horas");
    }

    @Test
    @DisplayName("verificar - a verificação periódica deve aceitar o resultado guardado do LLM")
    void verificar_naoDeveForcarAVerificacao() {
        when(iaHealthClient.verificarStatus()).thenReturn(iaNoAr());
        comRegistros(new EnumMap<>(Integracao.class));

        servico(mock(HgBrasilClient.class)).verificar();

        verify(iaHealthClient, never()).verificarStatus(true);
        verifyNoInteractions(indicadores, tesouro);
    }

    @Test
    @DisplayName("verificarAgora - deve forçar a checagem da IA e sincronizar HG Brasil, Tesouro e Banco Central")
    void verificarAgora_deveVerificarTodas() {
        when(iaHealthClient.verificarStatus(true)).thenReturn(iaNoAr());
        comRegistros(new EnumMap<>(Integracao.class));

        SaudeIntegracoesResponseDTO resposta = servico(mock(HgBrasilClient.class)).verificarAgora();

        assertThat(resposta.getIntegracoes()).hasSize(Integracao.values().length);
        verify(iaHealthClient).verificarStatus(true);
        verify(indicadores).sincronizarMercado();
        verify(indicadores).sincronizarSelicIpca();
        verify(tesouro).sincronizar();
        verify(monitor).registrarSucesso(Integracao.LLM);
    }

    @Test
    @DisplayName("verificarAgora - uma integração externa deve sincronizar só ela e devolver o estado atualizado")
    void verificarAgora_integracaoExterna_deveSincronizarSoEla() {
        Map<Integracao, StatusIntegracao> registros = new EnumMap<>(Integracao.class);
        registros.put(Integracao.TESOURO_DIRETO, StatusIntegracao.builder().integracao(Integracao.TESOURO_DIRETO).ultimoSucessoEm(agora).build());
        comRegistros(registros);

        StatusIntegracaoDTO item = servico(mock(HgBrasilClient.class)).verificarAgora(Integracao.TESOURO_DIRETO);

        assertThat(item.getChave()).isEqualTo(Integracao.TESOURO_DIRETO);
        assertThat(item.getSituacao()).isEqualTo(SituacaoIntegracao.OPERACIONAL);
        assertThat(item.getUltimoSucessoEm()).isEqualTo(agora);
        verify(tesouro).sincronizar();
        verifyNoInteractions(indicadores, iaHealthClient);
    }

    @Test
    @DisplayName("verificarAgora - o Banco Central deve sincronizar Selic e IPCA")
    void verificarAgora_bancoCentral_deveSincronizarSelicIpca() {
        comRegistros(new EnumMap<>(Integracao.class));

        StatusIntegracaoDTO item = servico(mock(HgBrasilClient.class)).verificarAgora(Integracao.BANCO_CENTRAL);

        assertThat(item.getChave()).isEqualTo(Integracao.BANCO_CENTRAL);
        verify(indicadores).sincronizarSelicIpca();
        verify(indicadores, never()).sincronizarMercado();
    }

    @Test
    @DisplayName("verificarAgora - o LLM deve ser checado ao vivo, sem cache e sem sincronizar nada")
    void verificarAgora_llm_deveForcarAChecagem() {
        when(iaHealthClient.verificarStatus(true)).thenReturn(IaHealthStatusDTO.builder().disponivel(true).rabbitmqConectado(true)
                .llm(llm(false, "O provedor do LLM não respondeu")).build());
        comRegistros(new EnumMap<>(Integracao.class));

        StatusIntegracaoDTO item = servico(mock(HgBrasilClient.class)).verificarAgora(Integracao.LLM);

        assertThat(item.getChave()).isEqualTo(Integracao.LLM);
        assertThat(item.getSituacao()).isEqualTo(SituacaoIntegracao.FORA_DO_AR);
        assertThat(item.getDetalhe()).isEqualTo("O provedor do LLM não respondeu");
        verify(monitor).registrarFalha(Integracao.LLM, "O provedor do LLM não respondeu");
        verifyNoInteractions(indicadores, tesouro);
    }

    @Test
    @DisplayName("verificarAgora - um erro inesperado na sincronização deve virar falha registrada")
    void verificarAgora_erroInesperado_deveRegistrarFalha() {
        doThrow(new IllegalStateException("Banco indisponível")).when(indicadores).sincronizarMercado();
        comRegistros(new EnumMap<>(Integracao.class));

        StatusIntegracaoDTO item = servico(mock(HgBrasilClient.class)).verificarAgora(Integracao.HG_BRASIL);

        assertThat(item.getChave()).isEqualTo(Integracao.HG_BRASIL);
        verify(monitor).registrarFalha(Integracao.HG_BRASIL, "Banco indisponível");
    }
}
