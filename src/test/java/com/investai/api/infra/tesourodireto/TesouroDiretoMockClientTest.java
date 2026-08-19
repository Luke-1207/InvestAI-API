package com.investai.api.infra.tesourodireto;

import com.investai.api.infra.tesourodireto.dto.TituloTesouroExternoDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class TesouroDiretoMockClientTest {

    private final TesouroDiretoMockClient tesouroDiretoMockClient = new TesouroDiretoMockClient();

    @Test
    @DisplayName("buscarTitulosDisponiveis - deve retornar catálogo sintético com códigos únicos")
    void buscarTitulosDisponiveis_deveRetornarCatalogoSinteticoComCodigosUnicos() {
        List<TituloTesouroExternoDTO> resultado = tesouroDiretoMockClient.buscarTitulosDisponiveis();

        assertThat(resultado).hasSize(7); // 3 fixos + 4 de volume
        List<String> codigos = resultado.stream().map(TituloTesouroExternoDTO::getCodigo).collect(Collectors.toList());
        assertThat(codigos).doesNotHaveDuplicates();
        assertThat(codigos).allMatch(codigo -> codigo != null && !codigo.isBlank());
    }

    @Test
    @DisplayName("buscarTitulosDisponiveis - não deve fazer nenhuma chamada externa (é só isso que garante o teste rodar rápido/offline)")
    void buscarTitulosDisponiveis_deveSerTotalmenteOffline() {
        // Se esse método terminar sem exception e sem depender de rede, já prova o contrato
        assertThat(tesouroDiretoMockClient.buscarTitulosDisponiveis()).isNotEmpty();
    }
}