package com.conditionservice.exception;

/**
 * Исключение «условие отбора не найдено».
 *
 * <p>Выбрасывается сервисным слоем, когда по переданному бизнес-ключу
 * ({@code conditionKey}) условие в таблице {@code conditions} отсутствует.
 * Обрабатывается в {@link GlobalExceptionHandler} как HTTP {@code 404}.</p>
 */
public class ConditionNotFoundException extends RuntimeException {

    public ConditionNotFoundException(String conditionKey) {
        super("Условие отбора с ключом '" + conditionKey + "' не найдено");
    }
}