package com.investai.api.module.relatorio.service;

import com.investai.api.module.relatorio.dto.HistoricoRelatorioResponseDTO;
import com.investai.api.module.relatorio.entity.HistoricoRelatorio;
import com.investai.api.module.relatorio.entity.TipoRelatorio;
import com.investai.api.module.relatorio.repository.HistoricoRelatorioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class HistoricoRelatorioService {

    public static final int TAMANHO_PADRAO = 20;
    public static final int TAMANHO_MAXIMO = 100;

    private final HistoricoRelatorioRepository historicoRelatorioRepository;

    public void registrar(UUID usuarioId, TipoRelatorio tipo, String referencia) {
        try {
            historicoRelatorioRepository.save(HistoricoRelatorio.builder()
                    .usuarioId(usuarioId)
                    .tipo(tipo)
                    .referencia(limitar(referencia))
                    .geradoEm(LocalDateTime.now())
                    .build());
        } catch (RuntimeException e) {
            log.warn("Não foi possível registrar o histórico do relatório {} ({}) do usuário {}: {}",
                    tipo, referencia, usuarioId, e.getMessage());
        }
    }

    public Page<HistoricoRelatorioResponseDTO> listar(UUID usuarioId, int pagina, int tamanho) {
        int paginaValida = Math.max(pagina, 0);
        int tamanhoValido = tamanho < 1 ? TAMANHO_PADRAO : Math.min(tamanho, TAMANHO_MAXIMO);

        return historicoRelatorioRepository
                .findByUsuarioIdOrderByGeradoEmDesc(usuarioId, PageRequest.of(paginaValida, tamanhoValido))
                .map(this::toResponseDTO);
    }

    private String limitar(String referencia) {
        return referencia.length() <= HistoricoRelatorio.TAMANHO_MAXIMO_REFERENCIA
                ? referencia
                : referencia.substring(0, HistoricoRelatorio.TAMANHO_MAXIMO_REFERENCIA);
    }

    private HistoricoRelatorioResponseDTO toResponseDTO(HistoricoRelatorio historico) {
        return HistoricoRelatorioResponseDTO.builder()
                .id(historico.getId())
                .tipo(historico.getTipo())
                .referencia(historico.getReferencia())
                .geradoEm(historico.getGeradoEm())
                .build();
    }
}
