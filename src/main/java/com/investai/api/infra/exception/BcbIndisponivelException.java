package com.investai.api.infra.exception;

public class BcbIndisponivelException extends RuntimeException {
    public BcbIndisponivelException(String mensagem) {
        super(mensagem);
    }
}