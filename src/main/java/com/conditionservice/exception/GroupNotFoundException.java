package com.conditionservice.exception;

/**
 * Исключение «группа условий не найдена».
 *
 * <p>Выбрасывается сервисным слоем, когда по переданному бизнес-ключу
 * ({@code groupKey}) группа в таблице {@code condition_groups} отсутствует.
 * Обрабатывается в {@link GlobalExceptionHandler} как HTTP {@code 404}.</p>
 */
public class GroupNotFoundException extends RuntimeException {

    public GroupNotFoundException(String groupKey) {
        super("Группа условий с ключом '" + groupKey + "' не найдена");
    }
}