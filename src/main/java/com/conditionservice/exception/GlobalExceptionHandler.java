package com.conditionservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Глобальный обработчик ошибок валидации входящих DTO.
 *
 * <p>Перехватывает {@link MethodArgumentNotValidException} (выбрасывается Spring MVC,
 * когда тело {@code @RequestBody} не проходит Jakarta Bean Validation) и собирает все
 * ошибки валидации в единый ответ с кодом {@code 400 BAD_REQUEST}.</p>
 *
 * <p>Формат ответа: {@code Map<String, String>}, где ключ — имя поля DTO,
 * значение — сообщение об ошибке на русском языке. Порядок сохраняется
 * благодаря {@link LinkedHashMap}.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Обрабатывает ошибки валидации тела запроса.
     *
     * @param ex исключение валидации
     * @return ответ с HTTP 400 и картой «поле → сообщение»
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }
}