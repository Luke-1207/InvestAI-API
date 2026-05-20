package com.investai.api.infra.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setup() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void deveTratarBusinessException() {

        BusinessException exception =
                new BusinessException("Erro de negócio");

        ResponseEntity<Map<String, Object>> response =
                handler.handleBusiness(exception);

        assertEquals(
                HttpStatus.UNPROCESSABLE_ENTITY,
                response.getStatusCode()
        );

        assertEquals(
                422,
                response.getBody().get("status")
        );

        assertEquals(
                "Erro de negócio",
                response.getBody().get("erro")
        );

        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    void deveTratarConflictException() {

        ConflictException exception =
                new ConflictException("E-mail já cadastrado");

        ResponseEntity<Map<String, Object>> response =
                handler.handleConflict(exception);

        assertEquals(
                HttpStatus.CONFLICT,
                response.getStatusCode()
        );

        assertEquals(
                409,
                response.getBody().get("status")
        );

        assertEquals(
                "E-mail já cadastrado",
                response.getBody().get("erro")
        );
    }

    @Test
    void deveTratarResourceNotFoundException() {

        ResourceNotFoundException exception =
                new ResourceNotFoundException("Usuário não encontrado");

        ResponseEntity<Map<String, Object>> response =
                handler.handleNotFound(exception);

        assertEquals(
                HttpStatus.NOT_FOUND,
                response.getStatusCode()
        );

        assertEquals(
                404,
                response.getBody().get("status")
        );

        assertEquals(
                "Usuário não encontrado",
                response.getBody().get("erro")
        );
    }

    @Test
    void deveTratarGenericException() {

        Exception exception =
                new RuntimeException("Erro inesperado");

        ResponseEntity<Map<String, Object>> response =
                handler.handleGeneric(exception);

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode()
        );

        assertEquals(
                500,
                response.getBody().get("status")
        );

        assertEquals(
                "Erro interno do servidor",
                response.getBody().get("erro")
        );
    }

    @Test
    void deveTratarMethodArgumentNotValidException()
            throws NoSuchMethodException {

        Object target = new Object();

        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(target, "dto");

        bindingResult.addError(
                new FieldError(
                        "dto",
                        "email",
                        "E-mail inválido"
                )
        );

        bindingResult.addError(
                new FieldError(
                        "dto",
                        "senha",
                        "Senha obrigatória"
                )
        );

        Method method = this.getClass()
                .getDeclaredMethod("metodoFake", Object.class);

        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(
                        null,
                        bindingResult
                );

        ResponseEntity<Map<String, Object>> response =
                handler.handleValidationErrors(exception);

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode()
        );

        assertEquals(
                400,
                response.getBody().get("status")
        );

        assertEquals(
                "Erro de validação",
                response.getBody().get("erro")
        );

        Map<String, String> detalhes =
                (Map<String, String>) response.getBody()
                        .get("detalhes");

        assertEquals(
                "E-mail inválido",
                detalhes.get("email")
        );

        assertEquals(
                "Senha obrigatória",
                detalhes.get("senha")
        );
    }

    private void metodoFake(Object obj) {
    }
}