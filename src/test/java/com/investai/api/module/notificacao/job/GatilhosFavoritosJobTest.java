package com.investai.api.module.notificacao.job;

import com.investai.api.module.notificacao.service.GatilhosFavoritosService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GatilhosFavoritosJobTest {

    @Mock
    private GatilhosFavoritosService gatilhosFavoritosService;

    @InjectMocks
    private GatilhosFavoritosJob job;

    @Test
    @DisplayName("deve delegar a verificação para o serviço")
    void deveDelegar() {
        job.verificar();

        verify(gatilhosFavoritosService).verificar(any());
    }

    @Test
    @DisplayName("erro no serviço não deve derrubar o agendador")
    void erroNaoDevePropagar() {
        when(gatilhosFavoritosService.verificar(any())).thenThrow(new RuntimeException("falha"));

        assertThatCode(job::verificar).doesNotThrowAnyException();
    }
}
