package com.investai.api.module.integracao.job;

import com.investai.api.module.integracao.service.SaudeIntegracoesService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class IntegracoesHealthJobTest {

    @Test
    @DisplayName("verificar - deve conferir as integrações mesmo sem ninguém com o painel aberto")
    void verificar_deveChamarOServico() {
        SaudeIntegracoesService servico = mock(SaudeIntegracoesService.class);

        new IntegracoesHealthJob(servico).verificar();

        verify(servico).verificar();
    }
}
