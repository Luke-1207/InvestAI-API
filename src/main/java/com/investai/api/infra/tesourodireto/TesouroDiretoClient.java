package com.investai.api.infra.tesourodireto;

import com.investai.api.infra.tesourodireto.dto.TituloTesouroExternoDTO;

import java.util.List;

public interface TesouroDiretoClient {
    List<TituloTesouroExternoDTO> buscarTitulosDisponiveis();
}