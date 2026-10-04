package com.nikonoff.conditionservice.dto.imports;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

/**
 * Одно условие отбора, импортируемое из 1С.
 * Сырое XML-представление (xmlPayload) в дальнейшем парсится в канонический
 * JSONB-payload условия таблицы conditions.
 */
@Data
@Builder
public class ConditionImportDto {

    /** Название условия (например, «Статус оплаты»). */
    @NotBlank(message = "conditionName не может быть пустым")
    private String conditionName;

    /** Сырая строка XML-настройки условия из 1С. */
    @NotBlank(message = "xmlPayload не может быть пустым")
    private String xmlPayload;

    /** Тип оператора сравнения; опционально, если передаётся извне. */
    private String operatorType;
}