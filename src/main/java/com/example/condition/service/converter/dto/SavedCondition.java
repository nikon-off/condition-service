package com.example.condition.service.converter.dto;

/**
 * Результат конвертации XML-фильтра 1С с сохранением в БД.
 *
 * <p>
 * Возвращается методом
 * {@code ConditionConverterService#saveOrConvertWithResult(String)} и является
 * телом ответа {@code POST /api/v1/conditions/convert}. Содержит:
 * </p>
 * <ul>
 * <li>{@code id} — персистентный id записи из таблицы {@code conditions}
 * (возвращается {@code RETURNING id} Upsert-запроса, одинаков для Insert и
 * Update — удобно для проверки идемпотентности);</li>
 * <li>{@code conditionKey} — детерминированный бизнес-ключ (SHA-256 hex от
 * канонического JSON), по которому запись ищется через
 * {@code ConditionRepository#findByConditionKey};</li>
 * <li>{@code condition} — каноническое условие ({@code logic} + {@code rules}),
 * ровно то, что было записано в колонку {@code payload}.</li>
 * </ul>
 *
 * @param id           персистентный id записи в БД
 * @param conditionKey бизнес-ключ условия (SHA-256, 64 hex-символа в нижнем
 *                     регистре)
 * @param condition    каноническое условие, сохранённое в {@code payload}
 */
public record SavedCondition(Long id, String conditionKey, CanonicalConditionDto condition) {
}