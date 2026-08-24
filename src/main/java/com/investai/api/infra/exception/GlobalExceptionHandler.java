package com.investai.api.infra.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Erro não tratado: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor", null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> erros = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(erro ->
                erros.put(erro.getField(), erro.getDefaultMessage())
        );
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildResponse(HttpStatus.BAD_REQUEST, "Erro de validação", erros));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), null));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(BusinessException ex) {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), null));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ConflictException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(buildResponse(HttpStatus.CONFLICT, ex.getMessage(), null));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildResponse(HttpStatus.BAD_REQUEST, "Requisição malformada ou valor de campo inválido", null));
    }

    @ExceptionHandler(HgBrasilIndisponivelException.class)
    public ResponseEntity<Map<String, Object>> handleHgBrasilIndisponivel(HgBrasilIndisponivelException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(buildResponse(HttpStatus.BAD_GATEWAY, ex.getMessage(), null));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildResponse(HttpStatus.BAD_REQUEST, "Parâmetro obrigatório ausente: " + ex.getParameterName(), null));
    }

    @ExceptionHandler(TesouroDiretoIndisponivelException.class)
    public ResponseEntity<Map<String, Object>> handleTesouroDiretoIndisponivel(TesouroDiretoIndisponivelException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(buildResponse(HttpStatus.BAD_GATEWAY, ex.getMessage(), null));
    }

    @ExceptionHandler(IaIndisponivelException.class)
    public ResponseEntity<Map<String, Object>> handleIaIndisponivel(IaIndisponivelException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(buildResponse(HttpStatus.BAD_GATEWAY, ex.getMessage(), null));
    }

    @ExceptionHandler(BcbIndisponivelException.class)
    public ResponseEntity<Map<String, Object>> handleBcbIndisponivel(BcbIndisponivelException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(buildResponse(HttpStatus.BAD_GATEWAY, ex.getMessage(), null));
    }

    private Map<String, Object> buildResponse(HttpStatus status, String mensagem, Object detalhes) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("erro", mensagem);
        if (detalhes != null) {
            body.put("detalhes", detalhes);
        }
        return body;
    }
}