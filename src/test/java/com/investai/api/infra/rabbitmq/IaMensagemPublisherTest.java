package com.investai.api.infra.rabbitmq;

import com.investai.api.config.RabbitConfig;
import com.investai.api.infra.exception.IaIndisponivelException;
import com.investai.api.infra.rabbitmq.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IaMensagemPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Spy
    private IaPendingRequestStore pendingRequestStore = new IaPendingRequestStore();

    private IaMensagemPublisher iaMensagemPublisher;

    @BeforeEach
    void setUp() {
        iaMensagemPublisher = new IaMensagemPublisher(rabbitTemplate, pendingRequestStore);
        ReflectionTestUtils.setField(iaMensagemPublisher, "timeoutSegundos", 1L); // curto, só pro teste
    }

    @Test
    @DisplayName("enviarResumoEAguardar - deve publicar na fila certa e retornar a resposta quando o consumer completa o future")
    void enviarResumoEAguardar_devePublicarERetornarRespostaQuandoConsumerCompleta() {
        Map<String, Object> ativo = Map.of("codigo", "TAEE3");
        PerfilIaDTO perfil = PerfilIaDTO.builder().build();

        doAnswer(invocation -> {
            ResumoRequestDTO request = invocation.getArgument(1);
            new Thread(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
                pendingRequestStore.completarResumo(request.getCorrelationId(),
                        ResumoResponseDTO.builder()
                                .correlationId(request.getCorrelationId())
                                .resumo("Resumo de teste")
                                .build());
            }).start();
            return null;
        }).when(rabbitTemplate).convertAndSend(eq(RabbitConfig.RESUMO_REQUEST), any(ResumoRequestDTO.class));

        ResumoResponseDTO resposta = iaMensagemPublisher.enviarResumoEAguardar(ModuloIa.VARIAVEL, perfil, ativo);

        assertThat(resposta.getResumo()).isEqualTo("Resumo de teste");
        assertThat(resposta.getCorrelationId()).isNotBlank();
    }

    @Test
    @DisplayName("enviarRankingEAguardar - deve publicar na fila certa e retornar a resposta quando o consumer completa o future")
    void enviarRankingEAguardar_devePublicarERetornarRespostaQuandoConsumerCompleta() {
        PerfilIaDTO perfil = PerfilIaDTO.builder().build();
        List<AtivoRankingDTO> ativos = List.of(AtivoRankingDTO.builder().codigo("TAEE3").build());

        doAnswer(invocation -> {
            RankingRequestDTO request = invocation.getArgument(1);
            new Thread(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
                pendingRequestStore.completarRanking(request.getCorrelationId(),
                        RankingResponseDTO.builder()
                                .correlationId(request.getCorrelationId())
                                .ativos(List.of(AtivoRankeadoDTO.builder().codigo("TAEE3").score(87).build()))
                                .build());
            }).start();
            return null;
        }).when(rabbitTemplate).convertAndSend(eq(RabbitConfig.RANKING_REQUEST), any(RankingRequestDTO.class));

        RankingResponseDTO resposta = iaMensagemPublisher.enviarRankingEAguardar(ModuloIa.FIXA, perfil, ativos);

        assertThat(resposta.getAtivos()).hasSize(1);
        assertThat(resposta.getAtivos().get(0).getScore()).isEqualTo(87);
    }

    @Test
    @DisplayName("enviarResumoEAguardar - deve lançar exceção e limpar o registro pendente quando dá timeout")
    void enviarResumoEAguardar_deveLancarExcecaoELimparRegistroQuandoTimeout() {
        Map<String, Object> ativo = Map.of("codigo", "TAEE3");
        PerfilIaDTO perfil = PerfilIaDTO.builder().build();

        assertThatThrownBy(() -> iaMensagemPublisher.enviarResumoEAguardar(ModuloIa.VARIAVEL, perfil, ativo))
                .isInstanceOf(IaIndisponivelException.class)
                .hasMessageContaining("não respondeu a tempo");

        verify(pendingRequestStore).removerResumo(any());
    }
}