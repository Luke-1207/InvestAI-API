package com.investai.api.infra.exception;

public class AtivoNaoEncontradoNaHgBrasilException extends RuntimeException {
    public AtivoNaoEncontradoNaHgBrasilException(String ticker) {
        super("Ticker não encontrado na HG Brasil: " + ticker);
    }
}
