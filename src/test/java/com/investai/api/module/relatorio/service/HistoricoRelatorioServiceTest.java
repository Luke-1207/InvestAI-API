package com.investai.api.module.relatorio.service;

import com.investai.api.module.relatorio.dto.HistoricoRelatorioResponseDTO;
import com.investai.api.module.relatorio.entity.HistoricoRelatorio;
import com.investai.api.module.relatorio.entity.TipoRelatorio;
import com.investai.api.module.relatorio.repository.HistoricoRelatorioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoricoRelatorioServiceTest {

    @Mock
    private HistoricoRelatorioRepository historicoRelatorioRepository;

    @InjectMocks
    private HistoricoRelatorioService historicoRelatorioService;

    @Captor
    private ArgumentCaptor<HistoricoRelatorio> historicoCaptor;

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    private final UUID usuarioId = UUID.randomUUID();

    @Test
    @DisplayName("registrar - deve salvar usuário, tipo, referência e o momento da geração")
    void registrar_deveSalvarRegistroCompleto() {
        LocalDateTime antes = LocalDateTime.now();

        historicoRelatorioService.registrar(usuarioId, TipoRelatorio.ATIVO_INDIVIDUAL, "TAEE11");

        verify(historicoRelatorioRepository).save(historicoCaptor.capture());
        HistoricoRelatorio salvo = historicoCaptor.getValue();
        assertThat(salvo.getUsuarioId()).isEqualTo(usuarioId);
        assertThat(salvo.getTipo()).isEqualTo(TipoRelatorio.ATIVO_INDIVIDUAL);
        assertThat(salvo.getReferencia()).isEqualTo("TAEE11");
        assertThat(salvo.getGeradoEm()).isBetween(antes, LocalDateTime.now());
    }

    @Test
    @DisplayName("registrar - referência maior que a coluna deve ser cortada no limite")
    void registrar_referenciaLonga_deveCortarNoLimite() {
        historicoRelatorioService.registrar(usuarioId, TipoRelatorio.ATIVO_INDIVIDUAL, "x".repeat(400));

        verify(historicoRelatorioRepository).save(historicoCaptor.capture());
        assertThat(historicoCaptor.getValue().getReferencia())
                .hasSize(HistoricoRelatorio.TAMANHO_MAXIMO_REFERENCIA);
    }

    @Test
    @DisplayName("registrar - falha ao salvar não deve propagar, para não impedir a entrega do PDF")
    void registrar_falhaAoSalvar_naoDevePropagar() {
        when(historicoRelatorioRepository.save(any()))
                .thenThrow(new DataAccessResourceFailureException("banco fora do ar"));

        assertThatCode(() -> historicoRelatorioService.registrar(usuarioId, TipoRelatorio.PERFIL, "PERFIL"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("listar - deve buscar a página pedida do usuário e converter para DTO")
    void listar_deveBuscarPaginaEConverter() {
        HistoricoRelatorio registro = HistoricoRelatorio.builder()
                .id(UUID.randomUUID()).usuarioId(usuarioId).tipo(TipoRelatorio.LISTAGEM)
                .referencia("LISTAGEM_VARIAVEL").geradoEm(LocalDateTime.of(2026, 10, 6, 15, 0)).build();
        when(historicoRelatorioRepository.findByUsuarioIdOrderByGeradoEmDesc(eq(usuarioId), any()))
                .thenReturn(new PageImpl<>(List.of(registro), PageRequest.of(1, 10), 11));

        Page<HistoricoRelatorioResponseDTO> pagina = historicoRelatorioService.listar(usuarioId, 1, 10);

        verify(historicoRelatorioRepository).findByUsuarioIdOrderByGeradoEmDesc(eq(usuarioId), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
        assertThat(pagina.getTotalElements()).isEqualTo(11);
        assertThat(pagina.getContent()).singleElement().satisfies(dto -> {
            assertThat(dto.getId()).isEqualTo(registro.getId());
            assertThat(dto.getTipo()).isEqualTo(TipoRelatorio.LISTAGEM);
            assertThat(dto.getReferencia()).isEqualTo("LISTAGEM_VARIAVEL");
            assertThat(dto.getGeradoEm()).isEqualTo(LocalDateTime.of(2026, 10, 6, 15, 0));
        });
    }

    @Test
    @DisplayName("listar - página negativa e tamanho inválido devem cair nos valores padrão")
    void listar_parametrosInvalidos_devemUsarPadrao() {
        when(historicoRelatorioRepository.findByUsuarioIdOrderByGeradoEmDesc(eq(usuarioId), any()))
                .thenReturn(Page.empty());

        historicoRelatorioService.listar(usuarioId, -3, 0);

        verify(historicoRelatorioRepository).findByUsuarioIdOrderByGeradoEmDesc(eq(usuarioId), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(HistoricoRelatorioService.TAMANHO_PADRAO);
    }

    @Test
    @DisplayName("listar - tamanho acima do máximo deve ser limitado")
    void listar_tamanhoAcimaDoMaximo_deveSerLimitado() {
        when(historicoRelatorioRepository.findByUsuarioIdOrderByGeradoEmDesc(eq(usuarioId), any()))
                .thenReturn(Page.empty());

        historicoRelatorioService.listar(usuarioId, 0, 5000);

        verify(historicoRelatorioRepository).findByUsuarioIdOrderByGeradoEmDesc(eq(usuarioId), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(HistoricoRelatorioService.TAMANHO_MAXIMO);
    }
}
