package com.conditionservice.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Входящий контракт для создания условия отбора.
 *
 * <p>{@code conditionKey} — бизнес-ключ условия (уникален в рамках сервиса),
 * {@code payload} — JSONB-структура условия, используемая для оператора containment
 * ({@code @>}) при проверке группы.</p>
 */
public class CreateConditionDto {

    @NotBlank(message = "Ключ условия не может быть пустым")
    @Size(max = 255, message = "Ключ условия не должен превышать 255 символов")
    private String conditionKey;

    @NotNull(message = "Payload условия не может быть null")
    private JsonNode payload;

    public CreateConditionDto() {
        // Для Jackson
    }

    public CreateConditionDto(String conditionKey, JsonNode payload) {
        this.conditionKey = conditionKey;
        this.payload = payload;
    }

    public String getConditionKey() {
        return conditionKey;
    }

    public void setConditionKey(String conditionKey) {
        this.conditionKey = conditionKey;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public void setPayload(JsonNode payload) {
        this.payload = payload;
    }
}