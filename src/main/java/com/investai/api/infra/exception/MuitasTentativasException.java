package com.investai.api.infra.exception;

import lombok.Getter;

@Getter
public class MuitasTentativasException extends RuntimeException {

    private final long segundosParaLiberar;

    public MuitasTentativasException(long segundosParaLiberar) {
        super(mensagem(segundosParaLiberar));
        this.segundosParaLiberar = segundosParaLiberar;
    }

    private static String mensagem(long segundos) {
        long minutos = Math.max(1, (segundos + 59) / 60);
        String espera = minutos == 1 ? "1 minuto" : minutos + " minutos";
        return "Muitas tentativas seguidas. Aguarde " + espera + " e tente de novo.";
    }
}
