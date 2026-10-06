package com.investai.api.module.ativo.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.investai.api.infra.exception.IaIndisponivelException;
import com.investai.api.infra.rabbitmq.AtivoIaMapper;
import com.investai.api.infra.rabbitmq.IaMensagemPublisher;
import com.investai.api.infra.rabbitmq.PerfilIaMapper;
import com.investai.api.infra.rabbitmq.dto.ModuloIa;
import com.investai.api.infra.rabbitmq.dto.ResumoResponseDTO;
import com.investai.api.module.ativo.dto.AcaoDetalheResponseDTO;
import com.investai.api.module.perfil.entity.PerfilInvestidor;
import com.investai.api.module.perfil.repository.PerfilInvestidorRepository;
import com.investai.api.shared.event.PerfilAlteradoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumoAtivoService {

    private final PerfilInvestidorRepository perfilInvestidorRepository;
    private final IaMensagemPublisher iaMensagemPublisher;

    private final Cache<String, String> cache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    public Optional<String> obterResumo(UUID usuarioId, AcaoDetalheResponseDTO ativo) {
        String chave = chave(usuarioId, ativo.getCodigo());
        String emCache = cache.getIfPresent(chave);
        if (emCache != null) {
            return Optional.of(emCache);
        }

        Optional<PerfilInvestidor> perfil = perfilInvestidorRepository.findByUsuarioId(usuarioId)
                .filter(this::perfilProntoParaIa);
        if (perfil.isEmpty() || !ativoProntoParaIa(ativo)) {
            return Optional.empty();
        }

        try {
            ResumoResponseDTO resposta = iaMensagemPublisher.enviarResumoEAguardar(
                    ModuloIa.VARIAVEL, PerfilIaMapper.dePerfil(perfil.get()), AtivoIaMapper.deAcao(ativo));

            if (resposta == null || resposta.getErro() != null
                    || resposta.getResumo() == null || resposta.getResumo().isBlank()) {
                log.warn("Microsserviço IA não gerou resumo para {}: {}", ativo.getCodigo(),
                        resposta == null ? "sem resposta" : resposta.getErro());
                return Optional.empty();
            }

            cache.put(chave, resposta.getResumo());
            return Optional.of(resposta.getResumo());
        } catch (IaIndisponivelException e) {
            return Optional.empty();
        }
    }

    @EventListener
    public void aoAlterarPerfil(PerfilAlteradoEvent event) {
        String prefixo = event.getUsuarioId() + ":";
        cache.asMap().keySet().removeIf(chave -> chave.startsWith(prefixo));
    }

    private String chave(UUID usuarioId, String codigo) {
        return usuarioId + ":" + codigo.toUpperCase();
    }

    private boolean perfilProntoParaIa(PerfilInvestidor perfil) {
        return perfil.isPerfilPreenchido()
                && perfil.getValorDisponivel() != null
                && perfil.getValorDisponivel().signum() > 0;
    }

    private boolean ativoProntoParaIa(AcaoDetalheResponseDTO ativo) {
        return ativo.isCotacaoDisponivel()
                && ativo.getPreco() != null
                && ativo.getPreco().signum() > 0;
    }
}
