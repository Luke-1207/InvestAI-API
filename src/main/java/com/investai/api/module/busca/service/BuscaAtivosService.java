package com.investai.api.module.busca.service;

import com.investai.api.module.ativo.entity.Acao;
import com.investai.api.module.ativo.entity.TipoAtivo;
import com.investai.api.module.ativo.repository.AcaoRepository;
import com.investai.api.module.busca.dto.BuscaAtivosRequestDTO;
import com.investai.api.module.busca.dto.GrupoBusca;
import com.investai.api.module.busca.dto.ResultadoBuscaDTO;
import com.investai.api.module.relatorio.builder.FormatoRelatorio;
import com.investai.api.module.rendafixa.entity.Indexador;
import com.investai.api.module.rendafixa.entity.TituloPrivado;
import com.investai.api.module.rendafixa.entity.TituloTesouro;
import com.investai.api.module.rendafixa.repository.TituloPrivadoRepository;
import com.investai.api.module.rendafixa.repository.TituloTesouroRepository;
import com.investai.api.module.rendafixa.service.CalculadoraRentabilidadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BuscaAtivosService {

    static final int TAMANHO_MINIMO_TERMO = 2;

    private static final int SEM_CORRESPONDENCIA = -1;
    private static final DateTimeFormatter MES_ANO = DateTimeFormatter.ofPattern("MM/yyyy");
    private static final Map<TipoAtivo, String> ROTULOS_RENDA_VARIAVEL = Map.of(
            TipoAtivo.ACAO, "Ação",
            TipoAtivo.FII, "FII",
            TipoAtivo.ETF, "ETF"
    );

    private final AcaoRepository acaoRepository;
    private final TituloTesouroRepository tituloTesouroRepository;
    private final TituloPrivadoRepository tituloPrivadoRepository;

    public List<ResultadoBuscaDTO> buscar(String termo, Integer limite) {
        String termoNormalizado = normalizar(termo);
        if (termoNormalizado.length() < TAMANHO_MINIMO_TERMO) {
            return List.of();
        }
        List<String> palavras = Arrays.asList(termoNormalizado.split(" "));
        int quantidade = limite == null ? BuscaAtivosRequestDTO.LIMITE_PADRAO : limite;

        List<Candidato> encontrados = new ArrayList<>();
        for (Candidato candidato : candidatos()) {
            int relevancia = relevancia(candidato, termoNormalizado, palavras);
            if (relevancia != SEM_CORRESPONDENCIA) {
                encontrados.add(candidato.comRelevancia(relevancia));
            }
        }

        return encontrados.stream()
                .sorted(Comparator.comparingInt(Candidato::relevancia)
                        .thenComparing(c -> c.resultado().getGrupo())
                        .thenComparing(c -> c.resultado().getNome(), String.CASE_INSENSITIVE_ORDER))
                .limit(quantidade)
                .map(Candidato::resultado)
                .toList();
    }

    private List<Candidato> candidatos() {
        List<Candidato> candidatos = new ArrayList<>();
        acaoRepository.findByAtivoTrue().forEach(acao -> candidatos.add(deRendaVariavel(acao)));
        tituloTesouroRepository.findByDisponivelTrue().forEach(titulo -> candidatos.add(deTesouro(titulo)));
        tituloPrivadoRepository.findByAtivoTrue().forEach(titulo -> candidatos.add(dePrivado(titulo)));
        return candidatos;
    }

    private Candidato deRendaVariavel(Acao acao) {
        String rotulo = ROTULOS_RENDA_VARIAVEL.get(acao.getTipo());
        return new Candidato(ResultadoBuscaDTO.builder()
                .grupo(GrupoBusca.RENDA_VARIAVEL)
                .categoria(acao.getTipo().name())
                .identificador(acao.getCodigo())
                .codigo(acao.getCodigo())
                .nome(acao.getNome())
                .detalhe(acao.getSetor())
                .build(), rotulo, 0);
    }

    private Candidato deTesouro(TituloTesouro titulo) {
        String taxa = FormatoRelatorio.taxaComIndexador(
                Indexador.valueOf(titulo.getTipo().name()),
                titulo.getTaxaAnual(),
                CalculadoraRentabilidadeService.taxaSomadaAoIndexador(titulo));
        return new Candidato(ResultadoBuscaDTO.builder()
                .grupo(GrupoBusca.RENDA_FIXA)
                .categoria("TESOURO")
                .identificador(titulo.getCodigo())
                .nome(titulo.getNome())
                .detalhe(detalheRendaFixa(taxa, titulo.getVencimento()))
                .build(), "Tesouro Direto", 0);
    }

    private Candidato dePrivado(TituloPrivado titulo) {
        String taxa = FormatoRelatorio.taxaComIndexador(
                titulo.getIndexador(),
                titulo.getTaxaPercentual(),
                CalculadoraRentabilidadeService.taxaSomadaAoIndexador(titulo));
        return new Candidato(ResultadoBuscaDTO.builder()
                .grupo(GrupoBusca.RENDA_FIXA)
                .categoria(titulo.getTipo().name())
                .identificador(titulo.getId().toString())
                .nome(titulo.getTipo().name() + " - " + titulo.getEmissor())
                .detalhe(detalheRendaFixa(taxa, titulo.getVencimento()))
                .build(), titulo.getTipo().name(), 0);
    }

    private String detalheRendaFixa(String taxa, LocalDate vencimento) {
        String vence = vencimento == null ? null : "vence em " + vencimento.format(MES_ANO);
        if (taxa == null) return vence;
        return vence == null ? taxa : taxa + " · " + vence;
    }

    private int relevancia(Candidato candidato, String termo, List<String> palavras) {
        String codigo = normalizar(candidato.resultado().getCodigo());
        String nome = normalizar(candidato.resultado().getNome());
        String texto = String.join(" ", codigo, nome, normalizar(candidato.rotuloCategoria()));

        if (!palavras.stream().allMatch(texto::contains)) return SEM_CORRESPONDENCIA;
        if (!codigo.isEmpty() && codigo.equals(termo)) return 0;
        if (!codigo.isEmpty() && codigo.startsWith(termo)) return 1;
        if (nome.startsWith(termo)) return 2;
        String primeiraPalavra = palavras.get(0);
        if (Arrays.stream(nome.split(" ")).anyMatch(parte -> parte.startsWith(primeiraPalavra))) return 3;
        return 4;
    }

    static String normalizar(String valor) {
        if (valor == null) return "";
        String semAcentos = Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcentos.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9+ ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private record Candidato(ResultadoBuscaDTO resultado, String rotuloCategoria, int relevancia) {
        Candidato comRelevancia(int novaRelevancia) {
            return new Candidato(resultado, rotuloCategoria, novaRelevancia);
        }
    }
}
