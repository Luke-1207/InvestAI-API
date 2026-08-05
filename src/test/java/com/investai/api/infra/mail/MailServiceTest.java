package com.investai.api.infra.mail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private MailService mailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mailService, "from", "no-reply@investai.com");
    }

    @Test
    @DisplayName("enviarEmailRecuperacaoSenha - deve montar e enviar e-mail quando envio está habilitado")
    void enviarEmailRecuperacaoSenha_deveEnviarQuandoHabilitado() {
        ReflectionTestUtils.setField(mailService, "mailEnabled", true);

        mailService.enviarEmailRecuperacaoSenha(
                "lucas@email.com", "Lucas Silva", "http://localhost:4200/redefinir-senha?token=abc123"
        );

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage mensagem = captor.getValue();
        assertThat(mensagem.getFrom()).isEqualTo("no-reply@investai.com");
        assertThat(mensagem.getTo()).containsExactly("lucas@email.com");
        assertThat(mensagem.getSubject()).isEqualTo("InvestAI - Recuperação de senha");
        assertThat(mensagem.getText())
                .contains("Lucas Silva")
                .contains("http://localhost:4200/redefinir-senha?token=abc123");
    }

    @Test
    @DisplayName("enviarEmailRecuperacaoSenha - não deve chamar o mailSender quando envio está desabilitado")
    void enviarEmailRecuperacaoSenha_naoDeveEnviarQuandoDesabilitado() {
        ReflectionTestUtils.setField(mailService, "mailEnabled", false);

        mailService.enviarEmailRecuperacaoSenha(
                "lucas@email.com", "Lucas Silva", "http://localhost:4200/redefinir-senha?token=abc123"
        );

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("enviarEmailRecuperacaoSenha - não deve propagar exceção quando o envio falha")
    void enviarEmailRecuperacaoSenha_naoDevePropagarExcecaoQuandoFalha() {
        ReflectionTestUtils.setField(mailService, "mailEnabled", true);

        doThrow(new MailSendException("Falha de conexão SMTP"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        mailService.enviarEmailRecuperacaoSenha(
                "lucas@email.com", "Lucas Silva", "http://localhost:4200/redefinir-senha?token=abc123"
        );

        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}