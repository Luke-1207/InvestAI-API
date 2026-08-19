package com.investai.api.module.rendafixa.service;

import com.investai.api.infra.exception.TesouroDiretoIndisponivelException;
import com.investai.api.infra.tesourodireto.TesouroDiretoClient;
import com.investai.api.infra.tesourodireto.dto.TituloTesouroExternoDTO;
import com.investai.api.module.rendafixa.entity.TipoTesouro;
import com.investai.api.module.rendafixa.entity.TituloTesouro;
import com.investai.api.module.rendafixa.repository.TituloTesouroRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TesouroDiretoSincronizacaoServiceTest {

    @Mock
    private TesouroDiretoClient tesouroDiretoClient;

    @Mock
    private TituloTesouroRepository tituloTesouroRepository;

    @InjectMocks
    private TesouroDiretoSincronizacaoService tesouroDiretoSincronizacaoService;

    @Test
    @DisplayName("sincronizar - deve atualizar título existente e criar título novo")
    void sincronizar_deveAtualizarTituloExistenteECriarTituloNovo() {
        TituloTesouroExternoDTO existente = TituloTesouroExternoDTO.builder()
                .codigo("tesouro-selic-01032031")
                .nome("Tesouro Selic 2031")
                .tipo("SELIC")
                .taxaAnual(BigDecimal.valueOf(0.08))
                .precoMinimo(BigDecimal.valueOf(189.45))
                .vencimento(LocalDate.of(2031, 3, 1))
                .pagaJurosSemestrais(false)
                .build();

        TituloTesouroExternoDTO novo = TituloTesouroExternoDTO.builder()
                .codigo("tesouro-ipca-2035-mock")
                .nome("Tesouro IPCA+ 2035")
                .tipo("IPCA")
                .taxaAnual(BigDecimal.valueOf(6.24))
                .precoMinimo(BigDecimal.valueOf(45.00))
                .vencimento(LocalDate.of(2035, 5, 15))
                .pagaJurosSemestrais(false)
                .build();

        when(tesouroDiretoClient.buscarTitulosDisponiveis()).thenReturn(List.of(existente, novo));

        TituloTesouro tituloJaSalvo = TituloTesouro.builder()
                .id(UUID.randomUUID())
                .codigo("tesouro-selic-01032031")
                .nome("Tesouro Selic 2031 (desatualizado)")
                .tipo(TipoTesouro.SELIC)
                .taxaAnual(BigDecimal.valueOf(0.05))
                .precoMinimo(BigDecimal.valueOf(150.00))
                .vencimento(LocalDate.of(2031, 3, 1))
                .disponivel(true)
                .build();

        when(tituloTesouroRepository.findByCodigo("tesouro-selic-01032031")).thenReturn(Optional.of(tituloJaSalvo));
        when(tituloTesouroRepository.findByCodigo("tesouro-ipca-2035-mock")).thenReturn(Optional.empty());

        tesouroDiretoSincronizacaoService.sincronizar();

        ArgumentCaptor<List<TituloTesouro>> captor = ArgumentCaptor.forClass(List.class);
        verify(tituloTesouroRepository).saveAll(captor.capture());

        List<TituloTesouro> salvos = captor.getValue();
        assertThat(salvos).hasSize(2);

        TituloTesouro atualizado = salvos.stream().filter(t -> t.getCodigo().equals("tesouro-selic-01032031")).findFirst().orElseThrow();
        assertThat(atualizado.getId()).isEqualTo(tituloJaSalvo.getId()); // é o MESMO registro, não um novo
        assertThat(atualizado.getNome()).isEqualTo("Tesouro Selic 2031"); // sobrescrito com o valor novo
        assertThat(atualizado.getTaxaAnual()).isEqualByComparingTo("0.08");
        assertThat(atualizado.getSincronizadoEm()).isNotNull();

        TituloTesouro criado = salvos.stream().filter(t -> t.getCodigo().equals("tesouro-ipca-2035-mock")).findFirst().orElseThrow();
        assertThat(criado.getId()).isNull(); // ainda não persistido, é @GeneratedValue
        assertThat(criado.getTipo()).isEqualTo(TipoTesouro.IPCA);
        assertThat(criado.isDisponivel()).isTrue();
    }

    @Test
    @DisplayName("sincronizar - não deve gravar nada quando o client externo está indisponível (fallback pro cache local)")
    void sincronizar_naoDeveGravarNadaQuandoClientIndisponivel() {
        when(tesouroDiretoClient.buscarTitulosDisponiveis())
                .thenThrow(new TesouroDiretoIndisponivelException("brapi.dev fora do ar"));

        tesouroDiretoSincronizacaoService.sincronizar();

        verify(tituloTesouroRepository, never()).saveAll(any());
        verify(tituloTesouroRepository, never()).save(any());
    }
}