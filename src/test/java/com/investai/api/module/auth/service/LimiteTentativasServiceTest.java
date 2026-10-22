package com.investai.api.module.auth.service;

import com.investai.api.infra.exception.MuitasTentativasException;
import com.investai.api.module.auth.service.LimiteTentativasService.Operacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LimiteTentativasServiceTest {

    private static final String EMAIL = "lucas@email.com";
    private static final String IP = "200.10.20.30";

    private static final class RelogioAjustavel extends Clock {
        private Instant agora = Instant.parse("2026-10-21T20:00:00Z");

        void avancar(Duration tempo) {
            agora = agora.plus(tempo);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }

    private final RelogioAjustavel relogio = new RelogioAjustavel();
    private final LimiteTentativasService limite =
            new LimiteTentativasService(5, 20, Duration.ofMinutes(15), Duration.ofMinutes(15), relogio);

    private void falhar(int vezes, String email, String ip) {
        for (int i = 0; i < vezes; i++) limite.registrarTentativa(Operacao.LOGIN, email, ip);
    }

    @Test
    @DisplayName("deve liberar enquanto não chega no limite de falhas do e-mail")
    void abaixoDoLimite_deveLiberar() {
        falhar(4, EMAIL, IP);

        assertThatCode(() -> limite.verificar(Operacao.LOGIN, EMAIL, IP)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("ao chegar no limite do e-mail deve bloquear por 15 minutos")
    void noLimiteDoEmail_deveBloquear() {
        falhar(5, EMAIL, IP);

        assertThatThrownBy(() -> limite.verificar(Operacao.LOGIN, EMAIL, "10.0.0.1"))
                .isInstanceOf(MuitasTentativasException.class)
                .hasMessage("Muitas tentativas seguidas. Aguarde 15 minutos e tente de novo.")
                .extracting("segundosParaLiberar").isEqualTo(900L);
    }

    @Test
    @DisplayName("o bloqueio deve acabar depois do tempo de espera")
    void bloqueio_deveExpirar() {
        falhar(5, EMAIL, IP);

        relogio.avancar(Duration.ofMinutes(14));
        assertThatThrownBy(() -> limite.verificar(Operacao.LOGIN, EMAIL, IP))
                .hasMessage("Muitas tentativas seguidas. Aguarde 1 minuto e tente de novo.");

        relogio.avancar(Duration.ofMinutes(1));
        assertThatCode(() -> limite.verificar(Operacao.LOGIN, EMAIL, IP)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("falhas espalhadas fora da janela não devem se acumular")
    void falhasForaDaJanela_naoDevemAcumular() {
        falhar(4, EMAIL, IP);
        relogio.avancar(Duration.ofMinutes(16));
        falhar(4, EMAIL, IP);

        assertThatCode(() -> limite.verificar(Operacao.LOGIN, EMAIL, IP)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("um login certo deve zerar as falhas do e-mail, mas não as do IP")
    void limpar_deveZerarSoOEmail() {
        falhar(4, EMAIL, IP);
        limite.limpar(Operacao.LOGIN, EMAIL);
        falhar(4, EMAIL, IP);
        assertThatCode(() -> limite.verificar(Operacao.LOGIN, EMAIL, IP)).doesNotThrowAnyException();

        falhar(12, "outro@email.com", IP);
        assertThatThrownBy(() -> limite.verificar(Operacao.LOGIN, "terceiro@email.com", IP))
                .isInstanceOf(MuitasTentativasException.class);
    }

    @Test
    @DisplayName("muitas falhas do mesmo IP com e-mails diferentes devem bloquear o IP")
    void limiteDoIp_deveBloquearQualquerEmail() {
        for (int i = 0; i < 20; i++) limite.registrarTentativa(Operacao.LOGIN, "usuario" + i + "@email.com", IP);

        assertThatThrownBy(() -> limite.verificar(Operacao.LOGIN, "novo@email.com", IP))
                .isInstanceOf(MuitasTentativasException.class);
        assertThatCode(() -> limite.verificar(Operacao.LOGIN, "novo@email.com", "10.0.0.1")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("o bloqueio do e-mail não deve afetar outro e-mail vindo de outro IP")
    void bloqueioDoEmail_naoAfetaOutroEmail() {
        falhar(5, EMAIL, IP);

        assertThatCode(() -> limite.verificar(Operacao.LOGIN, "outro@email.com", "10.0.0.1")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("login e recuperação de senha devem ter contagens separadas")
    void operacoes_devemSerIndependentes() {
        falhar(5, EMAIL, IP);

        assertThatCode(() -> limite.verificar(Operacao.RECUPERACAO_SENHA, EMAIL, IP)).doesNotThrowAnyException();

        for (int i = 0; i < 5; i++) limite.registrarTentativa(Operacao.RECUPERACAO_SENHA, "outro@email.com", "10.0.0.1");
        assertThatThrownBy(() -> limite.verificar(Operacao.RECUPERACAO_SENHA, "outro@email.com", "10.0.0.1"))
                .isInstanceOf(MuitasTentativasException.class);
    }

    @Test
    @DisplayName("tentativas durante o bloqueio não devem estender o tempo de espera")
    void tentativasDuranteOBloqueio_naoEstendemOPrazo() {
        falhar(5, EMAIL, IP);
        relogio.avancar(Duration.ofMinutes(10));
        falhar(3, EMAIL, IP);

        assertThatThrownBy(() -> limite.verificar(Operacao.LOGIN, EMAIL, IP))
                .extracting("segundosParaLiberar").isEqualTo(300L);
    }

    @Test
    @DisplayName("a limpeza periódica deve remover só os registros que já expiraram")
    @SuppressWarnings("unchecked")
    void removerExpirados_deveManterOsAtivos() {
        falhar(2, "antigo@email.com", "10.0.0.1");
        relogio.avancar(Duration.ofMinutes(16));
        falhar(5, EMAIL, IP);

        limite.removerExpirados();

        Map<String, ?> contadores = (Map<String, ?>) ReflectionTestUtils.getField(limite, "contadores");
        assertThat(contadores).containsOnlyKeys("LOGIN:email:" + EMAIL, "LOGIN:ip:" + IP);
        assertThatThrownBy(() -> limite.verificar(Operacao.LOGIN, EMAIL, IP)).isInstanceOf(MuitasTentativasException.class);
    }
}
