package com.investai.api.infra.exception;

public class ConflictException extends RuntimeException {
    public ConflictException(String mensagem) { super(mensagem); }
}
