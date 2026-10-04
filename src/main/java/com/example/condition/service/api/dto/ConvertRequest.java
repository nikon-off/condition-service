package com.example.condition.service.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Входящий контракт {@code POST /api/v1/conditions/convert}.
 *
 * @param xmlFilter сырой XML-фильтр настроек 1С DCS (поле {@code xmlFilter}),
 *                  передаётся в {@code ConditionConverterService} как есть
 *                  (включая спецсимволы: табы {@code \u0009}, переносы строк)
 */
public record ConvertRequest(
        @NotBlank(message = "XML-фильтр не может быть пустым") String xmlFilter) {
}