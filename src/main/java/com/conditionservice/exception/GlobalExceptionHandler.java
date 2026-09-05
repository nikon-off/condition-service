package com.conditionservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Глобальный обработчик ошибок.
 *
 * <p>Обрабатывает:
 * <ul>
 *   <li>{@link MethodArgumentNotValidException} — ошибки валидации входящих DTO
 *       (HTTP {@code 400 BAD_REQUEST});</li>
 *   <li>{@link GroupNotFoundException} — отсутствие группы условий (HTTP {@code 404}).</li>
 * </ul></p>
 *
 * <p>Формат ответа для валидации: {@code Map<String, String>}, где ключ — имя поля DTO,
 * значение — сообщение об ошибке. Порядок сохраняется благодаря {@link LinkedHashMap}.</p>
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

    /**
     * Обрабатывает отсутствие группы условий по переданному ключу.
     *
     * @param ex исключение «группа не найдена»
     * @return ответ с HTTP 404 и сообщением об ошибке
     */
    @ExceptionHandler(GroupNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleGroupNotFound(GroupNotFoundException ex) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }
}