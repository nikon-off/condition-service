package com.conditionservice.dto.response;

/**
 * Результат проверки одной группы в рамках пакетного запроса.
 *
 * @param groupKey бизнес-ключ проверенной группы
 * @param result   результат проверки {@link CheckResultDto}
 */
public record BatchCheckResultDto(
        String groupKey,
        CheckResultDto result) {
}