package com.investai.api.module.relatorio.service;

import com.investai.api.infra.exception.BusinessException;
import com.investai.api.module.ativo.service.AcaoSugestaoService;
import com.investai.api.module.auth.entity.Usuario;
import com.investai.api.module.dashboard.dto.SugestaoAtivoItemDTO;
import com.investai.api.module.perfil.dto.PerfilResponseDTO;
import com.investai.api.module.perfil.service.PerfilService;
import com.investai.api.module.relatorio.builder.FormatoRelatorio;
import com.investai.api.module.relatorio.dto.DadosRelatorioListagem;
import com.investai.api.module.relatorio.dto.ItemListagemRelatorio;
import com.investai.api.module.relatorio.dto.ModuloRelatorio;
import com.investai.api.module.relatorio.dto.RelatorioListagemRequestDTO;
import com.investai.api.module.rendafixa.dto.CategoriaRendaFixa;
import com.investai.api.module.rendafixa.dto.RendaFixaListagemResponseDTO;
import com.investai.api.module.rendafixa.entity.Indexador;
import com.investai.api.module.rendafixa.service.RendaFixaUnificadaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RelatorioListagemService {

    public static final String PERFIL_INCOMPLETO =
            "Complete seu perfil de investidor para gerar o relatório de listagem ranqueada";
    public static final String NENHUM_ATIVO = "Nenhum ativo foi encontrado na listagem ranqueada para gerar o relatório";

    private static final String MODO_INTELIGENTE = "inteligente";
    private static final String SEM_CODIGO = "-";
    private static final Map<String, String> ROTULOS_TIPO = Map.of(
            "ACAO", "Ação",
            "FII", "FII",
            "ETF", "ETF",
            "TESOURO", "Tesouro",
            "CDB", "CDB",
            "LCI", "LCI",
            "LCA", "LCA");

    private final PerfilService perfilService;
    private final AcaoSugestaoService acaoSugestaoService;
    private final RendaFixaUnificadaService rendaFixaUnificadaService;

    public DadosRelatorioListagem montarDados(RelatorioListagemRequestDTO request, Usuario usuario) {
        PerfilResponseDTO perfil = perfilService.obterPerfil(usuario.getId());
        if (!perfil.isPerfilPreenchido()) {
            throw new BusinessException(PERFIL_INCOMPLETO);
        }

        List<ItemRanqueado> ranqueados = new ArrayList<>();
        if (request.getModulo() != ModuloRelatorio.FIXA) {
            acaoSugestaoService.listarSugestoes(usuario.getId(), null).getItens()
                    .forEach(item -> ranqueados.add(deRendaVariavel(item)));
        }
        if (request.getModulo() != ModuloRelatorio.VARIAVEL) {
            rendaFixaUnificadaService.listar(MODO_INTELIGENTE, usuario.getId())
                    .forEach(item -> ranqueados.add(deRendaFixa(item)));
        }
        ranqueados.sort(Comparator.comparing(
                (ItemRanqueado item) -> item.item().score(), Comparator.nullsLast(Comparator.reverseOrder())));

        Set<String> solicitados = normalizar(request.getAtivos());
        List<ItemRanqueado> selecionados = solicitados.isEmpty()
                ? ranqueados
                : ranqueados.stream().filter(item -> item.correspondeA(solicitados)).toList();

        if (selecionados.isEmpty()) {
            throw new BusinessException(NENHUM_ATIVO);
        }

        return DadosRelatorioListagem.builder()
                .nomeUsuario(usuario.getNome())
                .geradoEm(LocalDateTime.now())
                .perfil(perfil)
                .modulo(request.getModulo())
                .filtros(request.getFiltros() == null ? Map.of() : new LinkedHashMap<>(request.getFiltros()))
                .itens(selecionados.stream()
                        .limit(RelatorioListagemRequestDTO.LIMITE_ATIVOS)
                        .map(ItemRanqueado::item)
                        .toList())
                .totalNaListagem(selecionados.size())
                .ativosNaoEncontrados(naoEncontrados(solicitados, selecionados))
                .build();
    }

    private ItemRanqueado deRendaVariavel(SugestaoAtivoItemDTO item) {
        return new ItemRanqueado(
                ItemListagemRelatorio.builder()
                        .codigo(item.getCodigo())
                        .nome(item.getNome())
                        .tipo(ROTULOS_TIPO.get(item.getTipo().name()))
                        .dadoPrincipal(item.getDy() == null ? null : "DY " + FormatoRelatorio.percentual(item.getDy()))
                        .score(item.getScore())
                        .compatibilidade(item.getCompatibilidade())
                        .justificativa(item.getJustificativa())
                        .build(),
                Set.of(item.getCodigo().toUpperCase()));
    }

    private ItemRanqueado deRendaFixa(RendaFixaListagemResponseDTO item) {
        Indexador indexador = Indexador.valueOf(item.getIndexador());
        boolean taxaSomadaAoIndexador = indexador == Indexador.IPCA
                || (item.getCategoria() == CategoriaRendaFixa.TESOURO && indexador != Indexador.PREFIXADO);

        Set<String> identificadores = new LinkedHashSet<>();
        identificadores.add(item.getId().toString().toUpperCase());
        if (item.getCodigo() != null) {
            identificadores.add(item.getCodigo().toUpperCase());
        }

        return new ItemRanqueado(
                ItemListagemRelatorio.builder()
                        .codigo(item.getCodigo() == null ? SEM_CODIGO : item.getCodigo())
                        .nome(item.getNome())
                        .tipo(ROTULOS_TIPO.get(item.getCategoria().name()))
                        .dadoPrincipal(FormatoRelatorio.taxaComIndexador(indexador, item.getTaxa(), taxaSomadaAoIndexador))
                        .score(item.getScore())
                        .compatibilidade(item.getCompatibilidade())
                        .justificativa(item.getJustificativa())
                        .build(),
                identificadores);
    }

    private Set<String> normalizar(List<String> ativos) {
        Set<String> normalizados = new LinkedHashSet<>();
        if (ativos != null) {
            ativos.forEach(ativo -> normalizados.add(ativo.trim().toUpperCase()));
        }
        return normalizados;
    }

    private List<String> naoEncontrados(Set<String> solicitados, List<ItemRanqueado> selecionados) {
        return solicitados.stream()
                .filter(solicitado -> selecionados.stream()
                        .noneMatch(item -> item.identificadores().contains(solicitado)))
                .toList();
    }

    private record ItemRanqueado(ItemListagemRelatorio item, Set<String> identificadores) {

        boolean correspondeA(Set<String> solicitados) {
            return identificadores.stream().anyMatch(solicitados::contains);
        }
    }
}
