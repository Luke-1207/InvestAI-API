package com.investai.api.module.rendafixa.service;

import com.investai.api.module.perfil.entity.PerfilInvestidor;
import com.investai.api.module.rendafixa.dto.CategoriaRendaFixa;
import com.investai.api.module.rendafixa.dto.RendaFixaListagemResponseDTO;
import com.investai.api.shared.scoring.PontuacaoUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RendaFixaPontuacaoServiceTest {

    private final RendaFixaPontuacaoService service = new RendaFixaPontuacaoService();

    private PerfilInvestidor perfil(String risco, String horizonte, String objetivo, double valorDisponivel) {
        return PerfilInvestidor.builder()
                .id(UUID.randomUUID()).perfilRisco(risco).horizonte(horizonte).objetivo(objetivo)
                .valorDisponivel(BigDecimal.valueOf(valorDisponivel)).perfilPreenchido(true).build();
    }

    private RendaFixaListagemResponseDTO titulo(CategoriaRendaFixa categoria, String indexador, int diasParaVencer,
                                                String liquidez, boolean isentoIr, boolean garantidoFgc, double valorMinimo) {
        return RendaFixaListagemResponseDTO.builder()
                .id(UUID.randomUUID()).categoria(categoria).nome("Título").indexador(indexador)
                .taxa(BigDecimal.TEN).vencimento(LocalDate.now().plusDays(diasParaVencer))
                .valorMinimo(BigDecimal.valueOf(valorMinimo)).liquidez(liquidez)
                .isentoIr(isentoIr).garantidoFgc(garantidoFgc).build();
    }

    private int score(Map<String, Integer> criterios) {
        return PontuacaoUtil.normalizarScore(criterios);
    }

    @Test
    @DisplayName("pesos - a soma dos melhores critérios de cada dimensão deve fechar exatamente 100")
    void pesos_somaDosMaximosDeveSer100() {
        int maximo = RendaFixaPontuacaoService.INDEXADOR_IDEAL + RendaFixaPontuacaoService.VENCIMENTO_NO_HORIZONTE
                + RendaFixaPontuacaoService.LIQUIDEZ_ADEQUADA + RendaFixaPontuacaoService.OBJETIVO_ALINHADO
                + RendaFixaPontuacaoService.INVESTIMENTO_ACESSIVEL + RendaFixaPontuacaoService.GARANTIA;

        assertThat(maximo).isEqualTo(100);
    }

    @Test
    @DisplayName("arrojado de longo prazo com Tesouro Prefixado longo deve chegar a 100")
    void arrojadoComPrefixadoLongo_deveChegarA100() {
        Map<String, Integer> criterios = service.avaliarCriterios(
                titulo(CategoriaRendaFixa.TESOURO, "PREFIXADO", 3000, "DIARIA", false, false, 30),
                perfil("ARROJADO", "LONGO_PRAZO", "CRESCIMENTO_PATRIMONIO", 9999));

        assertThat(score(criterios)).isEqualTo(100);
    }

    @Test
    @DisplayName("conservador de curto prazo com Tesouro Selic deve ser Alta (90)")
    void conservadorComTesouroSelic_deveSerAlta() {
        Map<String, Integer> criterios = service.avaliarCriterios(
                titulo(CategoriaRendaFixa.TESOURO, "SELIC", 100, "DIARIA", false, false, 150),
                perfil("CONSERVADOR", "CURTO_PRAZO", "RENDA_PASSIVA", 9999));

        assertThat(score(criterios)).isEqualTo(90);
        assertThat(PontuacaoUtil.classificarCompatibilidade(score(criterios)).name()).isEqualTo("ALTA");
        assertThat(criterios).containsKeys("indexador_ideal", "garantia_tesouro");
    }

    @Test
    @DisplayName("moderado agora tem indexador favorito e pode chegar a Alta (90, antes o máximo era 55)")
    void moderadoComIpca_deveSerAlta() {
        Map<String, Integer> criterios = service.avaliarCriterios(
                titulo(CategoriaRendaFixa.CDB, "IPCA", 2500, "NO_VENCIMENTO", false, true, 1000),
                perfil("MODERADO", "LONGO_PRAZO", "PRESERVAR_CAPITAL", 500));

        assertThat(score(criterios)).isEqualTo(90);
        assertThat(criterios).containsEntry("indexador_protege_inflacao", 15).doesNotContainKey("investimento_acessivel");
    }

    @Test
    @DisplayName("combinação parcial deve ficar em Média (76)")
    void combinacaoParcial_deveSerMedia() {
        Map<String, Integer> criterios = service.avaliarCriterios(
                titulo(CategoriaRendaFixa.LCI, "CDI", 1000, "NO_VENCIMENTO", true, true, 1000),
                perfil("MODERADO", "MEDIO_PRAZO", "CRESCIMENTO_PATRIMONIO", 5000));

        assertThat(score(criterios)).isEqualTo(76);
        assertThat(PontuacaoUtil.classificarCompatibilidade(76).name()).isEqualTo("MEDIA");
    }

    @Test
    @DisplayName("título que não combina com o perfil deve ser Baixa (10)")
    void tituloQueNaoCombina_deveSerBaixa() {
        Map<String, Integer> criterios = service.avaliarCriterios(
                titulo(CategoriaRendaFixa.CDB, "PREFIXADO", 3000, "NO_VENCIMENTO", false, false, 5000),
                perfil("CONSERVADOR", "CURTO_PRAZO", "CRESCIMENTO_PATRIMONIO", 1000));

        assertThat(score(criterios)).isEqualTo(10);
        assertThat(criterios).containsEntry("vencimento_muito_alem_horizonte", -10);
    }

    @Test
    @DisplayName("todo critério possível deve ter uma descrição em linguagem simples, e a soma deles deve explicar o score")
    void todoCriterioDeveTerDescricao() {
        String[] riscos = {"CONSERVADOR", "MODERADO", "ARROJADO"};
        String[] horizontes = {"CURTO_PRAZO", "MEDIO_PRAZO", "LONGO_PRAZO"};
        String[] objetivos = {"RENDA_PASSIVA", "CRESCIMENTO_PATRIMONIO", "PRESERVAR_CAPITAL"};
        String[] indexadores = {"SELIC", "CDI", "IPCA", "PREFIXADO"};
        int[] prazos = {100, 1000, 3000};
        for (String risco : riscos) for (String horizonte : horizontes) for (String objetivo : objetivos)
            for (String indexador : indexadores) for (int prazo : prazos) for (String liquidez : new String[]{"DIARIA", "NO_VENCIMENTO"}) {
                CategoriaRendaFixa categoria = "SELIC".equals(indexador) ? CategoriaRendaFixa.TESOURO : CategoriaRendaFixa.CDB;
                Map<String, Integer> criterios = service.avaliarCriterios(
                        titulo(categoria, indexador, prazo, liquidez, prazo == 1000, prazo != 3000, prazo == 100 ? 10 : 99999),
                        perfil(risco, horizonte, objetivo, 5000));

                assertThat(RendaFixaPontuacaoService.DESCRICOES_CRITERIOS).containsKeys(criterios.keySet().toArray(String[]::new));
                int soma = PontuacaoUtil.listarCriterios(criterios, RendaFixaPontuacaoService.DESCRICOES_CRITERIOS)
                        .stream().mapToInt(c -> c.getPontos()).sum();
                assertThat(Math.max(0, Math.min(100, soma))).isEqualTo(score(criterios));
            }
    }

    @Test
    @DisplayName("renda passiva deve preferir títulos isentos de IR")
    void rendaPassiva_devePreferirIsento() {
        PerfilInvestidor perfil = perfil("MODERADO", "MEDIO_PRAZO", "RENDA_PASSIVA", 9999);

        int isento = score(service.avaliarCriterios(titulo(CategoriaRendaFixa.LCA, "CDI", 1000, "NO_VENCIMENTO", true, true, 1000), perfil));
        int tributado = score(service.avaliarCriterios(titulo(CategoriaRendaFixa.CDB, "CDI", 1000, "NO_VENCIMENTO", false, true, 1000), perfil));

        assertThat(isento - tributado).isEqualTo(10);
    }
}
