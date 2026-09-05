package com.conditionservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldReturnBadRequestWithAllFieldErrorsInRussian() {
        // given: метод принимает DTO (метка 'body' для MethodArgumentNotValidException)
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "dto");
        bindingResult.addError(new FieldError("dto", "conditionKey",
                "Ключ условия не может быть пустым"));
        bindingResult.addError(new FieldError("dto", "payload",
                "Payload условия не может быть null"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                null, bindingResult);

        // when
        ResponseEntity<Map<String, String>> response = handler.handleValidationExceptions(ex);

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, String> body = response.getBody();
        assertTrue(body != null && body.size() == 2);
        assertEquals("Ключ условия не может быть пустым", body.get("conditionKey"));
        assertEquals("Payload условия не может быть null", body.get("payload"));
    }

    @Test
    void shouldReturnEmptyErrorsWhenNoFieldErrors() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "dto");
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<Map<String, String>> response = handler.handleValidationExceptions(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody() != null && response.getBody().isEmpty());
    }
}