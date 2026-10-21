package com.investai.api.module.integracao.service;

import com.investai.api.module.integracao.entity.Integracao;
import com.investai.api.module.integracao.entity.StatusIntegracao;
import com.investai.api.module.integracao.repository.StatusIntegracaoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Service
public class MonitorIntegracoesService {

    static final int TAMANHO_MAXIMO_ERRO = 500;

    private final StatusIntegracaoRepository statusIntegracaoRepository;
    private final TransactionOperations transacaoIndependente;

    @Autowired
    public MonitorIntegracoesService(StatusIntegracaoRepository statusIntegracaoRepository,
                                     PlatformTransactionManager transactionManager) {
        this(statusIntegracaoRepository, transacaoIndependente(transactionManager));
    }

    MonitorIntegracoesService(StatusIntegracaoRepository statusIntegracaoRepository,
                              TransactionOperations transacaoIndependente) {
        this.statusIntegracaoRepository = statusIntegracaoRepository;
        this.transacaoIndependente = transacaoIndependente;
    }

    public void registrarSucesso(Integracao integracao) {
        registrar(integracao, status -> status.setUltimoSucessoEm(LocalDateTime.now()));
    }

    public void registrarFalha(Integracao integracao, String erro) {
        registrar(integracao, status -> {
            status.setUltimaFalhaEm(LocalDateTime.now());
            status.setUltimoErro(resumir(erro));
        });
    }

    public Map<Integracao, StatusIntegracao> listar() {
        Map<Integracao, StatusIntegracao> registros = new EnumMap<>(Integracao.class);
        statusIntegracaoRepository.findAll().forEach(status -> registros.put(status.getIntegracao(), status));
        return registros;
    }

    private void registrar(Integracao integracao, Consumer<StatusIntegracao> alteracao) {
        try {
            transacaoIndependente.executeWithoutResult(transacao -> {
                StatusIntegracao status = statusIntegracaoRepository.findById(integracao)
                        .orElseGet(() -> StatusIntegracao.builder().integracao(integracao).build());
                alteracao.accept(status);
                statusIntegracaoRepository.save(status);
            });
        } catch (RuntimeException e) {
            log.warn("Não foi possível registrar o estado da integração {}: {}", integracao, e.getMessage());
        }
    }

    private static String resumir(String erro) {
        if (erro == null || erro.isBlank()) return "Falha sem mensagem";
        String texto = erro.strip();
        return texto.length() <= TAMANHO_MAXIMO_ERRO ? texto : texto.substring(0, TAMANHO_MAXIMO_ERRO - 3) + "...";
    }

    private static TransactionOperations transacaoIndependente(PlatformTransactionManager transactionManager) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
    }
}
