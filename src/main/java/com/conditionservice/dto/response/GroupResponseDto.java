package com.conditionservice.dto.response;

import java.time.Instant;

/**
 * Ответное представление группы условий отбора.
 *
 * @param key         бизнес-ключ группы
 * @param name        имя группы
 * @param description описание группы (может быть null)
 * @param createdAt   время создания
 * @param updatedAt   время последнего обновления
 */
public record GroupResponseDto(
        String key,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt) {
}