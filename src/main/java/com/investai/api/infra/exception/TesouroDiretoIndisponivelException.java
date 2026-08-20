package com.investai.api.infra.exception;

public class TesouroDiretoIndisponivelException extends RuntimeException {
    public TesouroDiretoIndisponivelException(String mensagem) {
        super(mensagem);
    }
}