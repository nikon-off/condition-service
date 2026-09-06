package com.conditionservice.dto.response;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

/**
 * Ответное представление условия отбора.
 *
 * @param conditionKey бизнес-ключ условия
 * @param payload      JSONB-структура условия
 * @param createdAt    время создания
 * @param updatedAt    время последнего обновления
 */
public record ConditionResponseDto(
        String conditionKey,
        JsonNode payload,
        Instant createdAt,
        Instant updatedAt) {
}