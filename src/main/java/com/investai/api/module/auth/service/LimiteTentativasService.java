package com.investai.api.module.auth.service;

import com.investai.api.infra.exception.MuitasTentativasException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LimiteTentativasService {

    public enum Operacao { LOGIN, RECUPERACAO_SENHA }

    private record Contador(int falhas, Instant inicioJanela, Instant bloqueadoAte) {

        boolean bloqueado(Instant agora) {
            return bloqueadoAte != null && bloqueadoAte.isAfter(agora);
        }
    }

    private final Map<String, Contador> contadores = new ConcurrentHashMap<>();
    private final int maxPorEmail;
    private final int maxPorIp;
    private final Duration janela;
    private final Duration bloqueio;
    private final Clock clock;

    @Autowired
    public LimiteTentativasService(
            @Value("${security.limite-tentativas.max-por-email:5}") int maxPorEmail,
            @Value("${security.limite-tentativas.max-por-ip:20}") int maxPorIp,
            @Value("${security.limite-tentativas.janela-minutos:15}") long janelaMinutos,
            @Value("${security.limite-tentativas.bloqueio-minutos:15}") long bloqueioMinutos
    ) {
        this(maxPorEmail, maxPorIp, Duration.ofMinutes(janelaMinutos), Duration.ofMinutes(bloqueioMinutos), Clock.systemUTC());
    }

    LimiteTentativasService(int maxPorEmail, int maxPorIp, Duration janela, Duration bloqueio, Clock clock) {
        this.maxPorEmail = maxPorEmail;
        this.maxPorIp = maxPorIp;
        this.janela = janela;
        this.bloqueio = bloqueio;
        this.clock = clock;
    }

    public void verificar(Operacao operacao, String email, String ip) {
        Instant agora = clock.instant();
        long segundos = Math.max(segundosBloqueado(chaveEmail(operacao, email), agora), segundosBloqueado(chaveIp(operacao, ip), agora));
        if (segundos > 0) throw new MuitasTentativasException(segundos);
    }

    public void registrarTentativa(Operacao operacao, String email, String ip) {
        Instant agora = clock.instant();
        contar(chaveEmail(operacao, email), maxPorEmail, agora);
        contar(chaveIp(operacao, ip), maxPorIp, agora);
    }

    public void limpar(Operacao operacao, String email) {
        contadores.remove(chaveEmail(operacao, email));
    }

    @Scheduled(fixedRateString = "${security.limite-tentativas.limpeza-ms:600000}")
    public void removerExpirados() {
        Instant agora = clock.instant();
        contadores.values().removeIf(c -> !c.bloqueado(agora) && !c.inicioJanela().plus(janela).isAfter(agora));
    }

    private long segundosBloqueado(String chave, Instant agora) {
        Contador contador = contadores.get(chave);
        if (contador == null || !contador.bloqueado(agora)) return 0;
        return Math.max(1, Duration.between(agora, contador.bloqueadoAte()).toSeconds());
    }

    private void contar(String chave, int maximo, Instant agora) {
        contadores.compute(chave, (k, atual) -> {
            if (atual != null && atual.bloqueado(agora)) return atual;
            boolean janelaValida = atual != null && atual.inicioJanela().plus(janela).isAfter(agora);
            int falhas = janelaValida ? atual.falhas() + 1 : 1;
            Instant inicio = janelaValida ? atual.inicioJanela() : agora;
            if (falhas >= maximo) return new Contador(0, agora, agora.plus(bloqueio));
            return new Contador(falhas, inicio, null);
        });
    }

    private static String chaveEmail(Operacao operacao, String email) {
        return operacao + ":email:" + email;
    }

    private static String chaveIp(Operacao operacao, String ip) {
        return operacao + ":ip:" + ip;
    }
}
