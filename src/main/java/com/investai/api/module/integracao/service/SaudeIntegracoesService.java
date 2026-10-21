package com.investai.api.module.integracao.service;

import com.investai.api.infra.hgbrasil.HgBrasilClient;
import com.investai.api.infra.hgbrasil.HgBrasilMockClient;
import com.investai.api.infra.ia.IaHealthClient;
import com.investai.api.infra.ia.dto.IaHealthStatusDTO;
import com.investai.api.infra.ia.dto.IaLlmHealthDTO;
import com.investai.api.infra.tesourodireto.TesouroDiretoClient;
import com.investai.api.infra.tesourodireto.TesouroDiretoMockClient;
import com.investai.api.module.dashboard.service.IndicadoresMercadoSincronizacaoService;
import com.investai.api.module.integracao.dto.SaudeIntegracoesResponseDTO;
import com.investai.api.module.integracao.dto.SituacaoIntegracao;
import com.investai.api.module.integracao.dto.StatusIntegracaoDTO;
import com.investai.api.module.integracao.entity.Integracao;
import com.investai.api.module.integracao.entity.StatusIntegracao;
import com.investai.api.module.rendafixa.service.TesouroDiretoSincronizacaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
@RequiredArgsConstructor
public class SaudeIntegracoesService {

    static final String DETALHE_SIMULADO = "Rodando em modo simulado: os dados não vêm da fonte real.";
    static final String DETALHE_SEM_IA = "Sem como verificar enquanto o microsserviço de IA está fora do ar.";

    private final IaHealthClient iaHealthClient;
    private final MonitorIntegracoesService monitorIntegracoesService;
    private final HgBrasilClient hgBrasilClient;
    private final TesouroDiretoClient tesouroDiretoClient;
    private final IndicadoresMercadoSincronizacaoService indicadoresMercadoSincronizacaoService;
    private final TesouroDiretoSincronizacaoService tesouroDiretoSincronizacaoService;
    @Qualifier("dashboardExecutor")
    private final Executor dashboardExecutor;

    public SaudeIntegracoesResponseDTO verificar() {
        return montarResposta(verificarIa(false));
    }

    public SaudeIntegracoesResponseDTO verificarAgora() {
        CompletableFuture<IaHealthStatusDTO> ia = CompletableFuture.supplyAsync(() -> verificarIa(true), dashboardExecutor);
        CompletableFuture<Void> tesouro = CompletableFuture.runAsync(() -> sincronizar(Integracao.TESOURO_DIRETO), dashboardExecutor);
        CompletableFuture<Void> indicadores = CompletableFuture.runAsync(() -> {
            sincronizar(Integracao.HG_BRASIL);
            sincronizar(Integracao.BANCO_CENTRAL);
        }, dashboardExecutor);

        CompletableFuture.allOf(ia, tesouro, indicadores).join();
        return montarResposta(ia.join());
    }

    public StatusIntegracaoDTO verificarAgora(Integracao integracao) {
        IaHealthStatusDTO ia = null;
        if (integracao.verificadaAoVivo()) {
            ia = verificarIa(true);
        } else {
            sincronizar(integracao);
        }
        return montarItem(integracao, monitorIntegracoesService.listar().get(integracao), ia, LocalDateTime.now());
    }

    private IaHealthStatusDTO verificarIa(boolean forcar) {
        IaHealthStatusDTO ia = forcar ? iaHealthClient.verificarStatus(true) : iaHealthClient.verificarStatus();
        registrarVerificacaoAoVivo(ia);
        return ia;
    }

    private void sincronizar(Integracao integracao) {
        Runnable sincronizacao = switch (integracao) {
            case HG_BRASIL -> indicadoresMercadoSincronizacaoService::sincronizarMercado;
            case BANCO_CENTRAL -> indicadoresMercadoSincronizacaoService::sincronizarSelicIpca;
            case TESOURO_DIRETO -> tesouroDiretoSincronizacaoService::sincronizar;
            default -> throw new IllegalArgumentException("Integração sem sincronização: " + integracao);
        };
        try {
            sincronizacao.run();
        } catch (RuntimeException e) {
            log.warn("Falha inesperada ao verificar {}: {}", integracao, e.getMessage());
            monitorIntegracoesService.registrarFalha(integracao, e.getMessage());
        }
    }

    private SaudeIntegracoesResponseDTO montarResposta(IaHealthStatusDTO ia) {
        Map<Integracao, StatusIntegracao> registros = monitorIntegracoesService.listar();
        LocalDateTime agora = LocalDateTime.now();

        List<StatusIntegracaoDTO> integracoes = new ArrayList<>();
        for (Integracao integracao : Integracao.values()) {
            integracoes.add(montarItem(integracao, registros.get(integracao), ia, agora));
        }

        return SaudeIntegracoesResponseDTO.builder().integracoes(integracoes).verificadoEm(agora).build();
    }

    private StatusIntegracaoDTO montarItem(Integracao integracao, StatusIntegracao registro, IaHealthStatusDTO ia, LocalDateTime agora) {
        boolean simulado = simulado(integracao);
        StatusIntegracaoDTO.StatusIntegracaoDTOBuilder item = StatusIntegracaoDTO.builder()
                .chave(integracao)
                .nome(integracao.getNome())
                .simulado(simulado)
                .ultimoSucessoEm(registro != null ? registro.getUltimoSucessoEm() : null)
                .ultimaFalhaEm(registro != null ? registro.getUltimaFalhaEm() : null)
                .ultimoErro(registro != null ? registro.getUltimoErro() : null);

        if (integracao.verificadaAoVivo()) {
            preencherAoVivo(item, integracao, ia);
        } else {
            SituacaoIntegracao situacao = situacaoPorSincronizacao(integracao, registro, agora);
            item.situacao(situacao).detalhe(simulado ? DETALHE_SIMULADO : detalheSincronizacao(integracao, situacao));
        }
        return item.build();
    }

    static SituacaoIntegracao situacaoPorSincronizacao(Integracao integracao, StatusIntegracao registro, LocalDateTime agora) {
        LocalDateTime sucesso = registro != null ? registro.getUltimoSucessoEm() : null;
        LocalDateTime falha = registro != null ? registro.getUltimaFalhaEm() : null;

        if (falha != null && (sucesso == null || falha.isAfter(sucesso))) return SituacaoIntegracao.FORA_DO_AR;
        if (sucesso == null) return SituacaoIntegracao.SEM_DADOS;
        if (sucesso.plus(integracao.getLimiteSemSucesso()).isBefore(agora)) return SituacaoIntegracao.DESATUALIZADA;
        return SituacaoIntegracao.OPERACIONAL;
    }

    private void registrarVerificacaoAoVivo(IaHealthStatusDTO ia) {
        if (!ia.isDisponivel()) {
            monitorIntegracoesService.registrarFalha(Integracao.MICROSSERVICO_IA, "O microsserviço de IA não respondeu ao /health");
            return;
        }
        monitorIntegracoesService.registrarSucesso(Integracao.MICROSSERVICO_IA);

        if (Boolean.TRUE.equals(ia.getRabbitmqConectado())) {
            monitorIntegracoesService.registrarSucesso(Integracao.RABBITMQ);
        } else if (Boolean.FALSE.equals(ia.getRabbitmqConectado())) {
            monitorIntegracoesService.registrarFalha(Integracao.RABBITMQ, "O microsserviço de IA não conseguiu se conectar ao RabbitMQ");
        }

        IaLlmHealthDTO llm = ia.getLlm();
        if (llm != null && llm.isDisponivel()) {
            monitorIntegracoesService.registrarSucesso(Integracao.LLM);
        } else if (llm != null) {
            monitorIntegracoesService.registrarFalha(Integracao.LLM, llm.getDetalhe());
        }
    }

    private void preencherAoVivo(StatusIntegracaoDTO.StatusIntegracaoDTOBuilder item, Integracao integracao, IaHealthStatusDTO ia) {
        switch (integracao) {
            case MICROSSERVICO_IA -> item
                    .situacao(ia.isDisponivel() ? SituacaoIntegracao.OPERACIONAL : SituacaoIntegracao.FORA_DO_AR)
                    .detalhe(ia.isDisponivel() ? "Respondendo normalmente." : "Não respondeu à verificação de saúde.");
            case RABBITMQ -> {
                Boolean conectado = ia.isDisponivel() ? ia.getRabbitmqConectado() : null;
                if (conectado == null) {
                    item.situacao(SituacaoIntegracao.DESCONHECIDA).detalhe(DETALHE_SEM_IA);
                } else {
                    item.situacao(conectado ? SituacaoIntegracao.OPERACIONAL : SituacaoIntegracao.FORA_DO_AR)
                            .detalhe(conectado ? "Conexão confirmada pelo microsserviço de IA." : "O microsserviço de IA não conseguiu se conectar.");
                }
            }
            case LLM -> {
                IaLlmHealthDTO llm = ia.isDisponivel() ? ia.getLlm() : null;
                if (llm == null) {
                    item.situacao(SituacaoIntegracao.DESCONHECIDA)
                            .detalhe(ia.isDisponivel() ? "O microsserviço de IA ainda não informa o estado do LLM." : DETALHE_SEM_IA);
                } else if (llm.isDisponivel()) {
                    item.situacao(SituacaoIntegracao.OPERACIONAL).detalhe("Acessível, usando o modelo " + llm.getModelo() + ".");
                } else {
                    item.situacao(SituacaoIntegracao.FORA_DO_AR).detalhe(llm.getDetalhe());
                }
            }
            default -> throw new IllegalArgumentException("Integração não verificada ao vivo: " + integracao);
        }
    }

    private boolean simulado(Integracao integracao) {
        return switch (integracao) {
            case HG_BRASIL -> hgBrasilClient instanceof HgBrasilMockClient;
            case TESOURO_DIRETO -> tesouroDiretoClient instanceof TesouroDiretoMockClient;
            default -> false;
        };
    }

    private static String detalheSincronizacao(Integracao integracao, SituacaoIntegracao situacao) {
        return switch (situacao) {
            case FORA_DO_AR -> "A última tentativa de sincronização falhou.";
            case SEM_DADOS -> "Ainda não houve uma sincronização registrada.";
            case DESATUALIZADA -> "Sem sincronização bem-sucedida há mais de " + integracao.getLimiteSemSucesso().toHours() + " horas.";
            default -> "Sincronizando normalmente.";
        };
    }
}
