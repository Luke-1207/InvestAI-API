package com.investai.api.infra.exception;

public class BusinessException extends RuntimeException {
    public BusinessException(String mensagem) { super(mensagem); }
}
