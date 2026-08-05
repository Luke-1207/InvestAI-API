package com.investai.api.infra.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    @Value("${app.mail.from:no-reply@investai.com}")
    private String from;

    @Async
    public void enviarEmailRecuperacaoSenha(String destinatario, String nome, String link) {
        String assunto = "InvestAI - Recuperação de senha";
        String corpo = """
            Olá, %s!

            Recebemos uma solicitação para redefinir sua senha no InvestAI.

            Clique no link abaixo para criar uma nova senha. Este link expira em 30 minutos:
            %s

            Se você não solicitou essa alteração, pode ignorar este e-mail com segurança.
            """.formatted(nome, link);

        if (!mailEnabled) {
            log.info("[DEV] Envio de e-mail desativado. Link de recuperação: {}", link);
            return;
        }

        try {
            SimpleMailMessage mensagem = new SimpleMailMessage();
            mensagem.setFrom(from);
            mensagem.setTo(destinatario);
            mensagem.setSubject(assunto);
            mensagem.setText(corpo);
            mailSender.send(mensagem);
        } catch (MailException e) {
            log.error("Falha ao enviar e-mail de recuperação para {}: {}", destinatario, e.getMessage());
        }
    }
}
