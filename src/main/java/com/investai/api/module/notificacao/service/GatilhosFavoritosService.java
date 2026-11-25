package com.investai.api.module.notificacao.service;

import com.investai.api.module.ativo.dto.CotacaoResponseDTO;
import com.investai.api.module.ativo.service.CotacaoService;
import com.investai.api.module.favorito.dto.AtivoFavoritavelDTO;
import com.investai.api.module.favorito.entity.Favorito;
import com.investai.api.module.favorito.entity.TipoAtivoFavorito;
import com.investai.api.module.favorito.repository.FavoritoRepository;
import com.investai.api.module.favorito.service.AtivoFavoritavelService;
import com.investai.api.module.notificacao.entity.TipoNotificacao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GatilhosFavoritosService {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final FavoritoRepository favoritoRepository;
    private final AtivoFavoritavelService ativoFavoritavelService;
    private final CotacaoService cotacaoService;
    private final NotificacaoService notificacaoService;

    @Value("${notificacoes.gatilhos.janela-repeticao-horas:24}")
    private long janelaRepeticaoHoras = 24;

    @Value("${notificacoes.gatilhos.dias-aviso-vencimento:30}")
    private long diasAvisoVencimento = 30;

    @Value("${notificacoes.gatilhos.janela-repeticao-vencimento-dias:7}")
    private long janelaRepeticaoVencimentoDias = 7;

    public int verificar(LocalDateTime agora) {
        int geradas = verificarVariacao(agora);
        geradas += verificarVencimento(TipoAtivoFavorito.TITULO_TESOURO, agora);
        geradas += verificarVencimento(TipoAtivoFavorito.TITULO_PRIVADO, agora);
        return geradas;
    }

    private int verificarVariacao(LocalDateTime agora) {
        List<Favorito> favoritos = favoritoRepository.findByTipoAtivoAndAlertasAtivosTrue(TipoAtivoFavorito.ACAO_FII_ETF);
        if (favoritos.isEmpty()) {
            return 0;
        }
        Map<UUID, AtivoFavoritavelDTO> ativos = ativoFavoritavelService.buscarPorIds(
                TipoAtivoFavorito.ACAO_FII_ETF, favoritos.stream().map(Favorito::getAtivoId).distinct().toList());
        Map<String, Optional<CotacaoResponseDTO>> cotacoes = new HashMap<>();

        int geradas = 0;
        for (Favorito favorito : favoritos) {
            AtivoFavoritavelDTO ativo = ativos.get(favorito.getAtivoId());
            if (ativo == null || ativo.codigo() == null) {
                continue;
            }
            Optional<CotacaoResponseDTO> cotacao = cotacoes.computeIfAbsent(ativo.codigo(), cotacaoService::obterCotacaoEmCache);
            BigDecimal variacao = cotacao.map(CotacaoResponseDTO::getVariacaoPercentual).orElse(null);
            if (variacao == null || variacao.abs().compareTo(favorito.getLimiarVariacao()) < 0) {
                continue;
            }
            TipoNotificacao tipo = variacao.signum() > 0 ? TipoNotificacao.VARIACAO_ALTA : TipoNotificacao.VARIACAO_BAIXA;
            if (!favorito.alertaHabilitado(tipo)
                    || notificacaoService.jaNotificadoDesde(favorito.getUsuarioId(), tipo, ativo.id(), agora.minusHours(janelaRepeticaoHoras))) {
                continue;
            }
            String verbo = tipo == TipoNotificacao.VARIACAO_ALTA ? "subiu" : "caiu";
            String titulo = String.format(PT_BR, "%s %s %.2f%% hoje", ativo.codigo(), verbo, variacao.abs());
            String preco = cotacao.map(CotacaoResponseDTO::getPreco)
                    .map(p -> String.format(PT_BR, " Cotação atual: R$ %.2f.", p))
                    .orElse("");
            String mensagem = String.format(PT_BR,
                    "A variação do dia passou do limite de %.2f%% que você definiu para %s.%s",
                    favorito.getLimiarVariacao(), ativo.nome(), preco);
            if (registrar(favorito, tipo, titulo, mensagem)) {
                geradas++;
            }
        }
        return geradas;
    }

    private int verificarVencimento(TipoAtivoFavorito tipoAtivo, LocalDateTime agora) {
        List<Favorito> favoritos = favoritoRepository.findByTipoAtivoAndAlertasAtivosTrue(tipoAtivo);
        if (favoritos.isEmpty()) {
            return 0;
        }
        Map<UUID, AtivoFavoritavelDTO> ativos = ativoFavoritavelService.buscarPorIds(
                tipoAtivo, favoritos.stream().map(Favorito::getAtivoId).distinct().toList());
        LocalDate hoje = agora.toLocalDate();

        int geradas = 0;
        for (Favorito favorito : favoritos) {
            AtivoFavoritavelDTO ativo = ativos.get(favorito.getAtivoId());
            if (ativo == null || ativo.vencimento() == null || !favorito.alertaHabilitado(TipoNotificacao.TITULO_VENCENDO)) {
                continue;
            }
            long dias = ChronoUnit.DAYS.between(hoje, ativo.vencimento());
            if (dias < 0 || dias > diasAvisoVencimento) {
                continue;
            }
            if (notificacaoService.jaNotificadoDesde(favorito.getUsuarioId(), TipoNotificacao.TITULO_VENCENDO, ativo.id(),
                    agora.minusDays(janelaRepeticaoVencimentoDias))) {
                continue;
            }
            String prazo = dias == 0 ? "vence hoje" : dias == 1 ? "vence amanhã" : "vence em " + dias + " dias";
            String titulo = ativo.nome() + " " + prazo;
            String mensagem = "O título que você favoritou vence em " + ativo.vencimento().format(DATA_BR)
                    + ". Vale planejar o que fazer com o valor resgatado.";
            if (registrar(favorito, TipoNotificacao.TITULO_VENCENDO, titulo, mensagem)) {
                geradas++;
            }
        }
        return geradas;
    }

    private boolean registrar(Favorito favorito, TipoNotificacao tipo, String titulo, String mensagem) {
        try {
            notificacaoService.criar(favorito.getUsuarioId(), tipo, titulo, mensagem, favorito.getTipoAtivo(), favorito.getAtivoId());
            return true;
        } catch (Exception e) {
            log.warn("Falha ao gerar notificação {} para o favorito {}: {}", tipo, favorito.getId(), e.getMessage());
            return false;
        }
    }
}
