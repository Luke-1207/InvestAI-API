package com.investai.api.module.perfil.dto;

import com.investai.api.module.perfil.entity.HorizonteInvestimento;
import com.investai.api.module.perfil.entity.ObjetivoFinanceiro;
import com.investai.api.module.perfil.entity.PerfilRisco;

import java.util.Map;

public class DescricoesPerfil {

    private DescricoesPerfil() {
    }

    public static final Map<PerfilRisco, String> PERFIL_RISCO = Map.of(
            PerfilRisco.CONSERVADOR, "Você é um investidor conservador: prioriza segurança e prefere evitar oscilações bruscas no patrimônio.",
            PerfilRisco.MODERADO, "Você é um investidor moderado: aceita alguma volatilidade em busca de crescimento.",
            PerfilRisco.ARROJADO, "Você é um investidor arrojado: tolera bem as oscilações do mercado em troca de um potencial de retorno maior."
    );

    public static final Map<ObjetivoFinanceiro, String> OBJETIVO_FINANCEIRO = Map.of(
            ObjetivoFinanceiro.RENDA_PASSIVA, "Seu objetivo é gerar renda passiva, recebendo retornos periódicos sem precisar vender seus ativos.",
            ObjetivoFinanceiro.CRESCIMENTO_PATRIMONIO, "Seu objetivo é fazer seu patrimônio crescer ao longo do tempo.",
            ObjetivoFinanceiro.PRESERVAR_CAPITAL, "Seu objetivo é preservar o capital que você já tem, evitando perdas."
    );

    public static final Map<HorizonteInvestimento, String> HORIZONTE_INVESTIMENTO = Map.of(
            HorizonteInvestimento.CURTO_PRAZO, "Seu horizonte é de curto prazo: você pode precisar desse dinheiro em menos de 1 ano.",
            HorizonteInvestimento.MEDIO_PRAZO, "Seu horizonte é de médio prazo: você pretende usar esse dinheiro entre 1 e 5 anos.",
            HorizonteInvestimento.LONGO_PRAZO, "Seu horizonte é de longo prazo: você não tem pressa para usar esse dinheiro, com mais de 5 anos pela frente."
    );
}