package com.investai.api.module.integracao.service;

import com.investai.api.module.integracao.entity.Integracao;
import com.investai.api.module.integracao.entity.StatusIntegracao;
import com.investai.api.module.integracao.repository.StatusIntegracaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionOperations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitorIntegracoesServiceTest {

    @Mock
    private StatusIntegracaoRepository statusIntegracaoRepository;

    private MonitorIntegracoesService monitorIntegracoesService;

    @BeforeEach
    void setUp() {
        monitorIntegracoesService = new MonitorIntegracoesService(statusIntegracaoRepository, TransactionOperations.withoutTransaction());
    }

    private StatusIntegracao salvo() {
        ArgumentCaptor<StatusIntegracao> captor = ArgumentCaptor.forClass(StatusIntegracao.class);
        verify(statusIntegracaoRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("registrarSucesso - deve criar o registro da integração com a data do sucesso")
    void registrarSucesso_deveCriarRegistro() {
        when(statusIntegracaoRepository.findById(Integracao.TESOURO_DIRETO)).thenReturn(Optional.empty());

        monitorIntegracoesService.registrarSucesso(Integracao.TESOURO_DIRETO);

        StatusIntegracao status = salvo();
        assertThat(status.getIntegracao()).isEqualTo(Integracao.TESOURO_DIRETO);
        assertThat(status.getUltimoSucessoEm()).isNotNull();
        assertThat(status.getUltimaFalhaEm()).isNull();
    }

    @Test
    @DisplayName("registrarFalha - deve manter o último sucesso e guardar a data e o motivo da falha")
    void registrarFalha_deveManterSucessoAnterior() {
        LocalDateTime sucessoAnterior = LocalDateTime.of(2026, 10, 20, 7, 0);
        when(statusIntegracaoRepository.findById(Integracao.BANCO_CENTRAL)).thenReturn(Optional.of(
                StatusIntegracao.builder().integracao(Integracao.BANCO_CENTRAL).ultimoSucessoEm(sucessoAnterior).build()));

        monitorIntegracoesService.registrarFalha(Integracao.BANCO_CENTRAL, "  timeout  ");

        StatusIntegracao status = salvo();
        assertThat(status.getUltimoSucessoEm()).isEqualTo(sucessoAnterior);
        assertThat(status.getUltimaFalhaEm()).isNotNull();
        assertThat(status.getUltimoErro()).isEqualTo("timeout");
    }

    @Test
    @DisplayName("registrarFalha - deve resumir mensagens longas e trocar mensagem vazia por um texto padrão")
    void registrarFalha_deveTratarMensagem() {
        when(statusIntegracaoRepository.findById(any())).thenReturn(Optional.empty());

        monitorIntegracoesService.registrarFalha(Integracao.HG_BRASIL, "x".repeat(600));
        monitorIntegracoesService.registrarFalha(Integracao.LLM, null);

        ArgumentCaptor<StatusIntegracao> captor = ArgumentCaptor.forClass(StatusIntegracao.class);
        verify(statusIntegracaoRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getUltimoErro())
                .hasSize(MonitorIntegracoesService.TAMANHO_MAXIMO_ERRO).endsWith("...");
        assertThat(captor.getAllValues().get(1).getUltimoErro()).isEqualTo("Falha sem mensagem");
    }

    @Test
    @DisplayName("registrar - erro ao gravar não deve atrapalhar quem chamou")
    void registrar_erroAoGravar_naoDevePropagar() {
        when(statusIntegracaoRepository.findById(any())).thenThrow(new IllegalStateException("banco fora"));

        assertThatCode(() -> monitorIntegracoesService.registrarSucesso(Integracao.HG_BRASIL)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("listar - deve indexar os registros pela integração")
    void listar_deveIndexarPorIntegracao() {
        StatusIntegracao hg = StatusIntegracao.builder().integracao(Integracao.HG_BRASIL).build();
        when(statusIntegracaoRepository.findAll()).thenReturn(List.of(hg));

        Map<Integracao, StatusIntegracao> registros = monitorIntegracoesService.listar();

        assertThat(registros).containsOnlyKeys(Integracao.HG_BRASIL);
        assertThat(registros.get(Integracao.HG_BRASIL)).isSameAs(hg);
    }
}
