package com.investai.api.module.rendafixa.service;

import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.rendafixa.dto.TituloTesouroDetalheResponseDTO;
import com.investai.api.module.rendafixa.entity.TipoTesouro;
import com.investai.api.module.rendafixa.entity.TituloTesouro;
import com.investai.api.module.rendafixa.repository.TituloTesouroRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TituloTesouroDetalheServiceTest {

    @Mock
    private TituloTesouroRepository tituloTesouroRepository;

    @InjectMocks
    private TituloTesouroDetalheService tituloTesouroDetalheService;

    @Test
    @DisplayName("obterDetalhe - deve retornar dados com descrição legível do tipo e liquidez fixa DIARIA")
    void obterDetalhe_deveRetornarDadosComDescricaoDoTipo() {
        TituloTesouro titulo = TituloTesouro.builder()
                .id(UUID.randomUUID())
                .codigo("tesouro-selic-2029-mock")
                .nome("Tesouro Selic 2029")
                .tipo(TipoTesouro.SELIC)
                .taxaAnual(BigDecimal.valueOf(0.05))
                .precoMinimo(BigDecimal.valueOf(150.00))
                .vencimento(LocalDate.of(2029, 3, 1))
                .pagaJurosSemestrais(false)
                .disponivel(true)
                .build();

        when(tituloTesouroRepository.findByCodigo("tesouro-selic-2029-mock")).thenReturn(Optional.of(titulo));

        TituloTesouroDetalheResponseDTO resultado = tituloTesouroDetalheService.obterDetalhe("tesouro-selic-2029-mock");

        assertThat(resultado.getCodigo()).isEqualTo("tesouro-selic-2029-mock");
        assertThat(resultado.getTipo().getValor()).isEqualTo("SELIC");
        assertThat(resultado.getTipo().getDescricao()).contains("taxa básica de juros");
        assertThat(resultado.getLiquidez()).isEqualTo("DIARIA");
        assertThat(resultado.getResumoIA()).isNull();
    }

    @Test
    @DisplayName("obterDetalhe - deve lançar exceção quando código não existe")
    void obterDetalhe_deveLancarExcecaoQuandoNaoEncontrado() {
        when(tituloTesouroRepository.findByCodigo("inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tituloTesouroDetalheService.obterDetalhe("inexistente"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}