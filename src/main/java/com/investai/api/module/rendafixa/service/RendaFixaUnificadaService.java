package com.investai.api.module.rendafixa.service;

import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.infra.rabbitmq.IaMensagemPublisher;
import com.investai.api.infra.rabbitmq.dto.*;
import com.investai.api.module.ativo.entity.TipoAtivo;
import com.investai.api.module.perfil.entity.HorizonteInvestimento;
import com.investai.api.module.perfil.entity.ObjetivoFinanceiro;
import com.investai.api.module.perfil.entity.PerfilInvestidor;
import com.investai.api.module.perfil.entity.PerfilRisco;
import com.investai.api.module.perfil.repository.PerfilInvestidorRepository;
import com.investai.api.module.rendafixa.dto.CategoriaRendaFixa;
import com.investai.api.module.rendafixa.dto.RendaFixaListagemResponseDTO;
import com.investai.api.module.rendafixa.repository.TituloPrivadoRepository;
import com.investai.api.module.rendafixa.repository.TituloTesouroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RendaFixaUnificadaService {

    private final TituloTesouroRepository tituloTesouroRepository;
    private final TituloPrivadoRepository tituloPrivadoRepository;
    private final PerfilInvestidorRepository perfilInvestidorRepository;
    private final IaMensagemPublisher iaMensagemPublisher;

    public List<RendaFixaListagemResponseDTO> listar(String modo, UUID usuarioId) {
        List<RendaFixaListagemResponseDTO> unificada = montarListaUnificada();

        if (!"inteligente".equalsIgnoreCase(modo)) {
            return unificada.stream()
                    .sorted(Comparator.comparing(RendaFixaListagemResponseDTO::getNome))
                    .toList();
        }

        return aplicarRanqueamento(unificada, usuarioId);
    }

    private List<RendaFixaListagemResponseDTO> montarListaUnificada() {
        List<RendaFixaListagemResponseDTO> lista = new ArrayList<>();

        tituloTesouroRepository.findByDisponivelTrue().forEach(t -> lista.add(
                RendaFixaListagemResponseDTO.builder()
                        .id(t.getId())
                        .categoria(CategoriaRendaFixa.TESOURO)
                        .nome(t.getNome())
                        .indexador(t.getTipo().name())
                        .taxa(t.getTaxaAnual())
                        .vencimento(t.getVencimento())
                        .valorMinimo(t.getPrecoMinimo())
                        .liquidez("DIARIA")
                        .isentoIr(false)
                        .garantidoFgc(false)
                        .build()
        ));

        tituloPrivadoRepository.findByAtivoTrue().forEach(t -> lista.add(
                RendaFixaListagemResponseDTO.builder()
                        .id(t.getId())
                        .categoria(CategoriaRendaFixa.valueOf(t.getTipo().name()))
                        .nome(t.getTipo().name() + " - " + t.getEmissor())
                        .indexador(t.getIndexador().name())
                        .taxa(t.getTaxaPercentual())
                        .vencimento(t.getVencimento())
                        .valorMinimo(t.getInvestimentoMinimo())
                        .liquidez(t.getLiquidez().name())
                        .isentoIr(t.isIsentoIr())
                        .garantidoFgc(t.isGarantidoFgc())
                        .build()
        ));

        return lista;
    }

    private List<RendaFixaListagemResponseDTO> aplicarRanqueamento(List<RendaFixaListagemResponseDTO> unificada, UUID usuarioId) {
        PerfilInvestidor perfil = perfilInvestidorRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil do investidor não encontrado"));

        PerfilIaDTO perfilIa = montarPerfilIa(perfil);
        List<Map<String, Object>> ativosParaRanquear = unificada.stream()
                .map(this::toAtivoRankingMap)
                .toList();

        RankingResponseDTO resultado = iaMensagemPublisher.enviarRankingEAguardar(ModuloIa.FIXA, perfilIa, ativosParaRanquear);

        Map<String, AtivoRankeadoDTO> rankingPorId = resultado.getAtivos().stream()
                .collect(Collectors.toMap(AtivoRankeadoDTO::getCodigo, r -> r));

        return unificada.stream()
                .peek(item -> {
                    AtivoRankeadoDTO ranking = rankingPorId.get(item.getId().toString());
                    if (ranking != null) {
                        item.setScore(ranking.getScore());
                        item.setCompatibilidade(ranking.getCompatibilidade());
                        item.setJustificativa(ranking.getJustificativa());
                    }
                })
                .sorted(Comparator.comparing(
                        RendaFixaListagemResponseDTO::getScore,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private Map<String, Object> toAtivoRankingMap(RendaFixaListagemResponseDTO item) {
        Map<String, Object> ativo = new HashMap<>();
        ativo.put("codigo", item.getId().toString());
        ativo.put("categoria", item.getCategoria().name());
        ativo.put("indexador", item.getIndexador());
        ativo.put("taxa", item.getTaxa());
        ativo.put("vencimento", item.getVencimento().toString());
        ativo.put("valorMinimo", item.getValorMinimo());
        ativo.put("liquidez", item.getLiquidez());
        ativo.put("isentoIR", item.isIsentoIr());
        ativo.put("garantidoFGC", item.isGarantidoFgc());
        return ativo;
    }

    private PerfilIaDTO montarPerfilIa(PerfilInvestidor perfil) {
        return PerfilIaDTO.builder()
                .perfilRisco(perfil.getPerfilRisco() != null ? PerfilRisco.valueOf(perfil.getPerfilRisco()) : null)
                .horizonte(perfil.getHorizonte() != null ? HorizonteInvestimento.valueOf(perfil.getHorizonte()) : null)
                .objetivo(perfil.getObjetivo() != null ? ObjetivoFinanceiro.valueOf(perfil.getObjetivo()) : null)
                .valorDisponivel(perfil.getValorDisponivel())
                .tiposAceitos(perfil.getTiposAceitos() == null ? List.of() :
                        perfil.getTiposAceitos().stream().map(TipoAtivo::valueOf).toList())
                .setoresPreferidos(perfil.getSetoresPreferidos() == null ? List.of() :
                        perfil.getSetoresPreferidos().stream()
                                .map(s -> SetorPreferidoIaDTO.builder()
                                        .setor(s.getSetor())
                                        .preferencia(s.getPreferencia())
                                        .build())
                                .toList())
                .build();
    }
}