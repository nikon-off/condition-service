package com.example.condition.service.converter.dto;

/**
 * Результат сохранения канонического условия в БД (Upsert).
 *
 * <p>
 * Содержит три компонента, необходимых вызывающей стороне:
 * </p>
 * <ul>
 * <li>{@code conditionKey} — детерминированный бизнес-ключ (SHA-256 hex),
 * используется для идемпотентного Upsert по
 * {@code uk_conditions_condition_key};</li>
 * <li>{@code payload} — исходное каноническое условие (для ответа API);</li>
 * <li>{@code id} — персистентный идентификатор строки в {@code conditions}
 * (из {@code RETURNING id}), требуется для M2M-связи через
 * {@code group_conditions}.</li>
 * </ul>
 *
 * @param conditionKey бизнес-ключ условия (уникален в таблице
 *                     {@code conditions})
 * @param payload      каноническое условие, сохранённое в колонку
 *                     {@code payload} (JSONB)
 * @param id           персистентный id записи
 */
public record SavedCondition(
        String conditionKey,
        CanonicalConditionDto payload,
        Long id) {
}