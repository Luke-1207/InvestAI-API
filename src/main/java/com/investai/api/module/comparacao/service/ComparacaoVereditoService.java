package com.investai.api.module.comparacao.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.investai.api.infra.exception.BusinessException;
import com.investai.api.infra.exception.IaIndisponivelException;
import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.infra.rabbitmq.AtivoIaMapper;
import com.investai.api.infra.rabbitmq.IaMensagemPublisher;
import com.investai.api.infra.rabbitmq.PerfilIaMapper;
import com.investai.api.infra.rabbitmq.dto.ComparacaoIaResponseDTO;
import com.investai.api.module.ativo.dto.AcaoDetalheResponseDTO;
import com.investai.api.module.ativo.service.AcaoDetalheService;
import com.investai.api.module.comparacao.dto.TipoAtivoComparacao;
import com.investai.api.module.comparacao.dto.VereditoComparacaoResponseDTO;
import com.investai.api.module.perfil.entity.PerfilInvestidor;
import com.investai.api.module.perfil.repository.PerfilInvestidorRepository;
import com.investai.api.module.rendafixa.entity.TituloPrivado;
import com.investai.api.module.rendafixa.entity.TituloTesouro;
import com.investai.api.module.rendafixa.repository.TituloPrivadoRepository;
import com.investai.api.module.rendafixa.repository.TituloTesouroRepository;
import com.investai.api.shared.event.PerfilAlteradoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComparacaoVereditoService {

    private final PerfilInvestidorRepository perfilInvestidorRepository;
    private final AcaoDetalheService acaoDetalheService;
    private final TituloTesouroRepository tituloTesouroRepository;
    private final TituloPrivadoRepository tituloPrivadoRepository;
    private final IaMensagemPublisher iaMensagemPublisher;

    private final Cache<String, String> cache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    public VereditoComparacaoResponseDTO obterVeredito(
            UUID usuarioId,
            TipoAtivoComparacao tipoA, String identificadorA,
            TipoAtivoComparacao tipoB, String identificadorB
    ) {
        String idA = normalizar(identificadorA);
        String idB = normalizar(identificadorB);

        if (tipoA.isRendaVariavel() == tipoB.isRendaVariavel() && idA.equalsIgnoreCase(idB)) {
            throw new BusinessException("Escolha dois ativos diferentes para comparar");
        }

        String chave = usuarioId + ":" + tipoA + ":" + idA.toUpperCase() + "|" + tipoB + ":" + idB.toUpperCase();
        String emCache = cache.getIfPresent(chave);
        if (emCache != null) {
            return new VereditoComparacaoResponseDTO(emCache, false);
        }

        PerfilInvestidor perfil = perfilInvestidorRepository.findByUsuarioId(usuarioId)
                .filter(this::perfilProntoParaIa)
                .orElseThrow(() -> new BusinessException(
                        "Preencha seu perfil de investidor para receber o veredito da comparação"));

        Map<String, Object> ativoA = montarAtivo(tipoA, idA);
        Map<String, Object> ativoB = montarAtivo(tipoB, idB);

        ComparacaoIaResponseDTO resposta = iaMensagemPublisher.enviarComparacaoEAguardar(
                PerfilIaMapper.dePerfil(perfil), ativoA, ativoB);

        if (resposta == null || resposta.getVeredito() == null || resposta.getVeredito().isBlank()) {
            log.warn("Microsserviço IA não gerou veredito para {} x {}: {}", idA, idB,
                    resposta == null ? "sem resposta" : resposta.getErro());
            throw new IaIndisponivelException("Não foi possível gerar o veredito da comparação no momento");
        }

        boolean simplificado = resposta.getErro() != null && !resposta.getErro().isBlank();
        if (!simplificado) {
            cache.put(chave, resposta.getVeredito());
        }
        return new VereditoComparacaoResponseDTO(resposta.getVeredito(), simplificado);
    }

    @EventListener
    public void aoAlterarPerfil(PerfilAlteradoEvent event) {
        String prefixo = event.getUsuarioId() + ":";
        cache.asMap().keySet().removeIf(chave -> chave.startsWith(prefixo));
    }

    private Map<String, Object> montarAtivo(TipoAtivoComparacao tipo, String identificador) {
        if (tipo.isRendaVariavel()) {
            return montarAtivoVariavel(identificador);
        }
        if (tipo == TipoAtivoComparacao.TESOURO) {
            return montarTesouro(identificador);
        }
        return montarTituloPrivado(identificador);
    }

    private Map<String, Object> montarAtivoVariavel(String codigo) {
        AcaoDetalheResponseDTO ativo = acaoDetalheService.obterDetalhe(codigo, null);
        if (!ativo.isCotacaoDisponivel() || !positivo(ativo.getPreco())) {
            throw new BusinessException("Cotação de " + ativo.getCodigo()
                    + " indisponível no momento, não foi possível gerar o veredito");
        }
        return AtivoIaMapper.deAcao(ativo);
    }

    private Map<String, Object> montarTesouro(String identificador) {
        TituloTesouro titulo = tituloTesouroRepository.findByCodigo(identificador)
                .or(() -> Optional.ofNullable(comoUuid(identificador)).flatMap(tituloTesouroRepository::findById))
                .filter(TituloTesouro::isDisponivel)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Título do Tesouro não encontrado: " + identificador));
        exigirTaxaPositiva(titulo.getTaxaAnual(), titulo.getNome());
        return AtivoIaMapper.deTesouro(titulo);
    }

    private Map<String, Object> montarTituloPrivado(String identificador) {
        TituloPrivado titulo = Optional.ofNullable(comoUuid(identificador))
                .flatMap(tituloPrivadoRepository::findById)
                .filter(TituloPrivado::isAtivo)
                .orElseThrow(() -> new ResourceNotFoundException("Título privado não encontrado"));
        exigirTaxaPositiva(titulo.getTaxaPercentual(), titulo.getTipo().name() + " - " + titulo.getEmissor());
        return AtivoIaMapper.deTituloPrivado(titulo);
    }

    private void exigirTaxaPositiva(BigDecimal taxa, String nome) {
        if (!positivo(taxa)) {
            throw new BusinessException("Taxa de " + nome + " indisponível, não foi possível gerar o veredito");
        }
    }

    private boolean perfilProntoParaIa(PerfilInvestidor perfil) {
        return perfil.isPerfilPreenchido()
                && perfil.getPerfilRisco() != null
                && perfil.getHorizonte() != null
                && perfil.getObjetivo() != null
                && positivo(perfil.getValorDisponivel());
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }

    private String normalizar(String identificador) {
        return identificador == null ? "" : identificador.trim();
    }

    private UUID comoUuid(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
