package com.investai.api.module.perfil.service;

import com.investai.api.module.perfil.entity.HorizonteInvestimento;
import com.investai.api.module.perfil.entity.ObjetivoFinanceiro;
import com.investai.api.module.perfil.entity.PerfilInvestidor;
import com.investai.api.module.perfil.entity.PerfilRisco;
import org.springframework.stereotype.Service;

@Service
public class ResumoPerfilService {

    public String gerarResumoIA(PerfilInvestidor perfil) {
        if (perfil.getObjetivo() == null || perfil.getHorizonte() == null || perfil.getPerfilRisco() == null) {
            return "Complete o quiz para receber sua análise personalizada.";
        }

        ObjetivoFinanceiro objetivo = ObjetivoFinanceiro.valueOf(perfil.getObjetivo());
        HorizonteInvestimento horizonte = HorizonteInvestimento.valueOf(perfil.getHorizonte());
        PerfilRisco perfilRisco = PerfilRisco.valueOf(perfil.getPerfilRisco());

        String objetivoTexto = switch (objetivo) {
            case RENDA_PASSIVA -> "renda passiva";
            case CRESCIMENTO_PATRIMONIO -> "crescimento do patrimônio";
            case PRESERVAR_CAPITAL -> "preservar o capital";
        };
        String horizonteTexto = switch (horizonte) {
            case CURTO_PRAZO -> "curto prazo";
            case MEDIO_PRAZO -> "médio prazo";
            case LONGO_PRAZO -> "longo prazo";
        };
        String perfilTexto = switch (perfilRisco) {
            case CONSERVADOR -> "conservador";
            case MODERADO -> "moderado";
            case ARROJADO -> "arrojado";
        };

        return "Você busca %s no %s, com perfil %s.".formatted(objetivoTexto, horizonteTexto, perfilTexto);
    }
}