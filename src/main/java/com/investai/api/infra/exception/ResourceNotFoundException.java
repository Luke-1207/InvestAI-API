package com.investai.api.infra.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String mensagem) { super(mensagem); }
}
