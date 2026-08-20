package com.investai.api.infra.rabbitmq;

import com.investai.api.infra.rabbitmq.dto.RankingResponseDTO;
import com.investai.api.infra.rabbitmq.dto.ResumoResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IaMensagemConsumerTest {

    @Mock
    private IaPendingRequestStore pendingRequestStore;

    @InjectMocks
    private IaMensagemConsumer iaMensagemConsumer;

    @Test
    @DisplayName("receberRanking - deve delegar para completarRanking com o correlationId da mensagem")
    void receberRanking_deveDelegarParaCompletarRanking() {
        RankingResponseDTO response = RankingResponseDTO.builder().correlationId("abc").ativos(List.of()).build();

        iaMensagemConsumer.receberRanking(response);

        verify(pendingRequestStore).completarRanking("abc", response);
    }

    @Test
    @DisplayName("receberResumo - deve delegar para completarResumo com o correlationId da mensagem")
    void receberResumo_deveDelegarParaCompletarResumo() {
        ResumoResponseDTO response = ResumoResponseDTO.builder().correlationId("xyz").resumo("teste").build();

        iaMensagemConsumer.receberResumo(response);

        verify(pendingRequestStore).completarResumo("xyz", response);
    }
}