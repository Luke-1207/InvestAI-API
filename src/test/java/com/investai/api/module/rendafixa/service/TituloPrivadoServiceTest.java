package com.investai.api.module.rendafixa.service;

import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.rendafixa.dto.AtualizarTituloPrivadoRequestDTO;
import com.investai.api.module.rendafixa.dto.CadastroTituloPrivadoRequestDTO;
import com.investai.api.module.rendafixa.dto.TituloPrivadoResponseDTO;
import com.investai.api.module.rendafixa.entity.Indexador;
import com.investai.api.module.rendafixa.entity.TipoLiquidez;
import com.investai.api.module.rendafixa.entity.TipoTituloPrivado;
import com.investai.api.module.rendafixa.entity.TituloPrivado;
import com.investai.api.module.rendafixa.repository.TituloPrivadoRepository;
import com.investai.api.shared.event.CatalogoAtivosAlteradoEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TituloPrivadoServiceTest {

    @Mock
    private TituloPrivadoRepository tituloPrivadoRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private TituloPrivadoService tituloPrivadoService;

    private CadastroTituloPrivadoRequestDTO criarCadastroDTO() {
        CadastroTituloPrivadoRequestDTO dto = new CadastroTituloPrivadoRequestDTO();
        dto.setTipo(TipoTituloPrivado.CDB);
        dto.setEmissor("Banco Inter");
        dto.setIndexador(Indexador.CDI);
        dto.setTaxaPercentual(BigDecimal.valueOf(112.0));
        dto.setVencimento(LocalDate.now().plusYears(2));
        dto.setInvestimentoMinimo(BigDecimal.valueOf(500.00));
        dto.setLiquidez(TipoLiquidez.DIARIA);
        dto.setGarantidoFgc(true);
        dto.setIsentoIr(false);
        return dto;
    }

    private TituloPrivado criarTituloExistente(UUID id) {
        return TituloPrivado.builder()
                .id(id)
                .tipo(TipoTituloPrivado.CDB)
                .emissor("Banco Antigo")
                .indexador(Indexador.CDI)
                .taxaPercentual(BigDecimal.valueOf(100.0))
                .vencimento(LocalDate.now().plusYears(1))
                .investimentoMinimo(BigDecimal.valueOf(1000.00))
                .liquidez(TipoLiquidez.NO_VENCIMENTO)
                .garantidoFgc(true)
                .isentoIr(false)
                .ativo(true)
                .build();
    }

    @Test
    @DisplayName("cadastrar - deve criar título privado ativo")
    void cadastrar_deveCriarTituloPrivadoAtivo() {
        CadastroTituloPrivadoRequestDTO dto = criarCadastroDTO();

        TituloPrivadoResponseDTO response = tituloPrivadoService.cadastrar(dto);

        ArgumentCaptor<TituloPrivado> captor = ArgumentCaptor.forClass(TituloPrivado.class);
        verify(tituloPrivadoRepository).save(captor.capture());

        TituloPrivado salvo = captor.getValue();
        assertThat(salvo.getTipo()).isEqualTo(TipoTituloPrivado.CDB);
        assertThat(salvo.getEmissor()).isEqualTo("Banco Inter");
        assertThat(salvo.isAtivo()).isTrue();

        assertThat(response.getEmissor()).isEqualTo("Banco Inter");
        assertThat(response.isAtivo()).isTrue();
    }

    @Test
    @DisplayName("atualizar - deve sobrescrever os campos substantivos sem alterar o status ativo")
    void atualizar_deveSobrescreverCamposSemAlterarStatusAtivo() {
        UUID id = UUID.randomUUID();
        TituloPrivado existente = criarTituloExistente(id);
        existente.setAtivo(false); // já estava desativado

        when(tituloPrivadoRepository.findById(id)).thenReturn(Optional.of(existente));

        AtualizarTituloPrivadoRequestDTO dto = new AtualizarTituloPrivadoRequestDTO();
        dto.setTipo(TipoTituloPrivado.LCI);
        dto.setEmissor("Banco Novo");
        dto.setIndexador(Indexador.IPCA);
        dto.setTaxaPercentual(BigDecimal.valueOf(6.5));
        dto.setVencimento(LocalDate.now().plusYears(3));
        dto.setInvestimentoMinimo(BigDecimal.valueOf(2000.00));
        dto.setLiquidez(TipoLiquidez.DIARIA);
        dto.setGarantidoFgc(true);
        dto.setIsentoIr(true);

        TituloPrivadoResponseDTO response = tituloPrivadoService.atualizar(id, dto);

        assertThat(response.getEmissor()).isEqualTo("Banco Novo");
        assertThat(response.getTipo()).isEqualTo(TipoTituloPrivado.LCI);
        assertThat(response.isIsentoIr()).isTrue();
        assertThat(response.isAtivo()).isFalse(); // PUT não mexe no ativo — continua desativado
    }

    @Test
    @DisplayName("atualizar - deve lançar exceção quando título não existe")
    void atualizar_deveLancarExcecaoQuandoNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(tituloPrivadoRepository.findById(id)).thenReturn(Optional.empty());

        AtualizarTituloPrivadoRequestDTO dto = new AtualizarTituloPrivadoRequestDTO();

        assertThatThrownBy(() -> tituloPrivadoService.atualizar(id, dto))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(tituloPrivadoRepository, never()).save(any());
    }

    @Test
    @DisplayName("alterarStatus - deve reativar título desativado")
    void alterarStatus_deveReativarTituloDesativado() {
        UUID id = UUID.randomUUID();
        TituloPrivado existente = criarTituloExistente(id);
        existente.setAtivo(false);

        when(tituloPrivadoRepository.findById(id)).thenReturn(Optional.of(existente));

        TituloPrivadoResponseDTO response = tituloPrivadoService.alterarStatus(id, true);

        assertThat(response.isAtivo()).isTrue();
        verify(tituloPrivadoRepository).save(existente);
    }

    @Test
    @DisplayName("alterarStatus - deve desativar título ativo")
    void alterarStatus_deveDesativarTituloAtivo() {
        UUID id = UUID.randomUUID();
        TituloPrivado existente = criarTituloExistente(id);

        when(tituloPrivadoRepository.findById(id)).thenReturn(Optional.of(existente));

        TituloPrivadoResponseDTO response = tituloPrivadoService.alterarStatus(id, false);

        assertThat(response.isAtivo()).isFalse();
    }

    @Test
    @DisplayName("remover - deve apagar o título de vez e avisar que o catálogo mudou")
    void remover_deveApagarTitulo() {
        UUID id = UUID.randomUUID();
        TituloPrivado existente = criarTituloExistente(id);

        when(tituloPrivadoRepository.findById(id)).thenReturn(Optional.of(existente));

        tituloPrivadoService.remover(id);

        verify(tituloPrivadoRepository).delete(existente);
        verify(tituloPrivadoRepository, never()).save(any());
        verify(applicationEventPublisher).publishEvent(any(CatalogoAtivosAlteradoEvent.class));
    }

    @Test
    @DisplayName("remover - deve lançar exceção quando título não existe")
    void remover_deveLancarExcecaoQuandoNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(tituloPrivadoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tituloPrivadoService.remover(id))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(tituloPrivadoRepository, never()).delete(any(TituloPrivado.class));
        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    @DisplayName("cadastrar, atualizar e alterarStatus - devem avisar que o catálogo mudou")
    void alteracoes_devemPublicarEventoDeCatalogo() {
        UUID id = UUID.randomUUID();
        when(tituloPrivadoRepository.findById(id)).thenReturn(Optional.of(criarTituloExistente(id)));

        tituloPrivadoService.cadastrar(criarCadastroDTO());
        AtualizarTituloPrivadoRequestDTO atualizacao = new AtualizarTituloPrivadoRequestDTO();
        atualizacao.setTipo(TipoTituloPrivado.LCI);
        atualizacao.setEmissor("Banco Novo");
        atualizacao.setIndexador(Indexador.CDI);
        atualizacao.setTaxaPercentual(BigDecimal.valueOf(95));
        atualizacao.setVencimento(LocalDate.now().plusYears(1));
        atualizacao.setInvestimentoMinimo(BigDecimal.valueOf(1000));
        atualizacao.setLiquidez(TipoLiquidez.NO_VENCIMENTO);
        atualizacao.setGarantidoFgc(true);
        atualizacao.setIsentoIr(true);
        tituloPrivadoService.atualizar(id, atualizacao);
        tituloPrivadoService.alterarStatus(id, false);

        verify(applicationEventPublisher, times(3)).publishEvent(any(CatalogoAtivosAlteradoEvent.class));
    }

    @Test
    @DisplayName("listarParaGestao - deve trazer ativos e inativos com o status de cada um")
    void listarParaGestao_deveTrazerAtivosEInativos() {
        TituloPrivado ativo = criarTituloExistente(UUID.randomUUID());
        TituloPrivado inativo = criarTituloExistente(UUID.randomUUID());
        inativo.setAtivo(false);
        when(tituloPrivadoRepository.findAllByOrderByTipoAscEmissorAscVencimentoAsc()).thenReturn(List.of(ativo, inativo));

        List<TituloPrivadoResponseDTO> resultado = tituloPrivadoService.listarParaGestao();

        assertThat(resultado).extracting(TituloPrivadoResponseDTO::isAtivo).containsExactly(true, false);
    }
}