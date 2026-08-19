package com.investai.api.module.rendafixa.service;

import com.investai.api.infra.exception.TesouroDiretoIndisponivelException;
import com.investai.api.infra.tesourodireto.TesouroDiretoClient;
import com.investai.api.infra.tesourodireto.dto.TituloTesouroExternoDTO;
import com.investai.api.module.rendafixa.entity.TipoTesouro;
import com.investai.api.module.rendafixa.entity.TituloTesouro;
import com.investai.api.module.rendafixa.repository.TituloTesouroRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TesouroDiretoSincronizacaoService {

    private final TesouroDiretoClient tesouroDiretoClient;
    private final TituloTesouroRepository tituloTesouroRepository;

    @Transactional
    public void sincronizar() {
        List<TituloTesouroExternoDTO> titulosExternos;

        try {
            titulosExternos = tesouroDiretoClient.buscarTitulosDisponiveis();
        } catch (TesouroDiretoIndisponivelException e) {
            log.warn("Sincronização do Tesouro Direto pulada — fonte externa indisponível ({}). "
                    + "Mantendo os dados já persistidos.", e.getMessage());
            return;
        }

        LocalDateTime agora = LocalDateTime.now();
        List<TituloTesouro> paraSalvar = new ArrayList<>();

        for (TituloTesouroExternoDTO externo : titulosExternos) {
            TituloTesouro titulo = tituloTesouroRepository.findByCodigo(externo.getCodigo())
                    .orElseGet(TituloTesouro::new);

            titulo.setCodigo(externo.getCodigo());
            titulo.setNome(externo.getNome());
            titulo.setTipo(TipoTesouro.valueOf(externo.getTipo()));
            titulo.setTaxaAnual(externo.getTaxaAnual());
            titulo.setPrecoMinimo(externo.getPrecoMinimo());
            titulo.setVencimento(externo.getVencimento());
            titulo.setPagaJurosSemestrais(externo.isPagaJurosSemestrais());
            titulo.setDisponivel(true);
            titulo.setSincronizadoEm(agora);

            paraSalvar.add(titulo);
        }

        tituloTesouroRepository.saveAll(paraSalvar);
        log.info("Sincronização do Tesouro Direto concluída: {} títulos atualizados", paraSalvar.size());
    }
}