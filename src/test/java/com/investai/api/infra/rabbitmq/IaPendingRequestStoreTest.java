package com.investai.api.infra.rabbitmq;

import com.investai.api.infra.rabbitmq.dto.ComparacaoIaResponseDTO;
import com.investai.api.infra.rabbitmq.dto.RankingResponseDTO;
import com.investai.api.infra.rabbitmq.dto.ResumoResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class IaPendingRequestStoreTest {

    private final IaPendingRequestStore store = new IaPendingRequestStore();

    @Test
    @DisplayName("registrarRanking + completarRanking - deve completar o future registrado com o correlationId certo")
    void registrarECompletarRanking_deveCompletarFutureRegistrado() throws Exception {
        CompletableFuture<RankingResponseDTO> future = store.registrarRanking("abc-123");

        RankingResponseDTO response = RankingResponseDTO.builder().correlationId("abc-123").ativos(List.of()).build();
        store.completarRanking("abc-123", response);

        assertThat(future.isDone()).isTrue();
        assertThat(future.get().getCorrelationId()).isEqualTo("abc-123");
    }

    @Test
    @DisplayName("completarRanking - não deve lançar exceção quando correlationId não está registrado")
    void completarRanking_naoDeveLancarExcecaoQuandoCorrelationIdDesconhecido() {
        RankingResponseDTO response = RankingResponseDTO.builder().correlationId("desconhecido").ativos(List.of()).build();

        assertThatCode(() -> store.completarRanking("desconhecido", response)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("removerRanking - deve tornar completarRanking um no-op depois de removido")
    void removerRanking_deveTornarCompletarRankingNoOpDepois() {
        CompletableFuture<RankingResponseDTO> future = store.registrarRanking("xyz");
        store.removerRanking("xyz");

        store.completarRanking("xyz", RankingResponseDTO.builder().correlationId("xyz").build());

        assertThat(future.isDone()).isFalse();
    }

    @Test
    @DisplayName("registrarResumo + completarResumo - deve completar o future registrado")
    void registrarECompletarResumo_deveCompletarFutureRegistrado() throws Exception {
        CompletableFuture<ResumoResponseDTO> future = store.registrarResumo("res-1");

        ResumoResponseDTO response = ResumoResponseDTO.builder().correlationId("res-1").resumo("teste").build();
        store.completarResumo("res-1", response);

        assertThat(future.get().getResumo()).isEqualTo("teste");
    }

    @Test
    @DisplayName("registrarComparacao + completarComparacao - deve completar o future registrado")
    void registrarECompletarComparacao_deveCompletarFutureRegistrado() throws Exception {
        CompletableFuture<ComparacaoIaResponseDTO> future = store.registrarComparacao("cmp-1");

        store.completarComparacao("cmp-1",
                ComparacaoIaResponseDTO.builder().correlationId("cmp-1").veredito("veredito").build());

        assertThat(future.isDone()).isTrue();
        assertThat(future.get().getVeredito()).isEqualTo("veredito");
    }

    @Test
    @DisplayName("completarComparacao - não deve lançar exceção quando correlationId não está registrado")
    void completarComparacao_naoDeveLancarExcecaoQuandoCorrelationIdDesconhecido() {
        assertThatCode(() -> store.completarComparacao("desconhecido",
                ComparacaoIaResponseDTO.builder().correlationId("desconhecido").build())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("removerComparacao - deve tornar completarComparacao um no-op depois de removido")
    void removerComparacao_deveTornarCompletarComparacaoNoOpDepois() {
        CompletableFuture<ComparacaoIaResponseDTO> future = store.registrarComparacao("cmp-2");
        store.removerComparacao("cmp-2");

        store.completarComparacao("cmp-2", ComparacaoIaResponseDTO.builder().correlationId("cmp-2").build());

        assertThat(future.isDone()).isFalse();
    }
}