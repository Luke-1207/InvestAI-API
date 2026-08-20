package com.investai.api.module.rendafixa.service;

import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.rendafixa.dto.RentabilidadeEstimadaDTO;
import com.investai.api.module.rendafixa.dto.TituloPrivadoDetalheResponseDTO;
import com.investai.api.module.rendafixa.entity.*;
import com.investai.api.module.rendafixa.repository.TituloPrivadoRepository;
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
class TituloPrivadoDetalheServiceTest {

    @Mock
    private TituloPrivadoRepository tituloPrivadoRepository;

    @Mock
    private CalculadoraRentabilidadeService calculadoraRentabilidadeService;

    @InjectMocks
    private TituloPrivadoDetalheService tituloPrivadoDetalheService;

    @Test
    @DisplayName("obterDetalhe - deve retornar dados com rentabilidade estimada calculada")
    void obterDetalhe_deveRetornarDadosComRentabilidadeEstimada() {
        UUID id = UUID.randomUUID();
        TituloPrivado titulo = TituloPrivado.builder()
                .id(id)
                .tipo(TipoTituloPrivado.CDB)
                .emissor("Banco Inter")
                .indexador(Indexador.CDI)
                .taxaPercentual(BigDecimal.valueOf(112.0))
                .vencimento(LocalDate.now().plusYears(2))
                .investimentoMinimo(BigDecimal.valueOf(500))
                .liquidez(TipoLiquidez.DIARIA)
                .garantidoFgc(true)
                .isentoIr(false)
                .ativo(true)
                .build();

        RentabilidadeEstimadaDTO rentabilidade = RentabilidadeEstimadaDTO.builder()
                .taxaBrutaAnual(BigDecimal.valueOf(112.0))
                .aliquotaIR(BigDecimal.valueOf(15.0))
                .taxaLiquidaAnual(BigDecimal.valueOf(95.20))
                .build();

        when(tituloPrivadoRepository.findById(id)).thenReturn(Optional.of(titulo));
        when(calculadoraRentabilidadeService.calcular(titulo)).thenReturn(rentabilidade);

        TituloPrivadoDetalheResponseDTO resultado = tituloPrivadoDetalheService.obterDetalhe(id);

        assertThat(resultado.getEmissor()).isEqualTo("Banco Inter");
        assertThat(resultado.getRentabilidadeEstimada().getTaxaLiquidaAnual()).isEqualByComparingTo("95.20");
        assertThat(resultado.getResumoIA()).isNull();
    }

    @Test
    @DisplayName("obterDetalhe - deve lançar exceção quando título não existe")
    void obterDetalhe_deveLancarExcecaoQuandoNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(tituloPrivadoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tituloPrivadoDetalheService.obterDetalhe(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}