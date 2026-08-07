package com.investai.api.infra.exception;

public class HgBrasilIndisponivelException extends RuntimeException {
    public HgBrasilIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
