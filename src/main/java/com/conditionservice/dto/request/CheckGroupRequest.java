package com.conditionservice.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Входящие данные для проверки группы условий отбора.
 *
 * <p>{@code groupKey} — ключ проверяемой группы, {@code payload} — входящие поля,
 * которые должны быть подмножеством payload'ов всех условий группы (оператор {@code @>}).</p>
 */
public class CheckGroupRequest {

    @NotBlank(message = "Ключ группы не может быть пустым")
    private String groupKey;

    @NotNull(message = "Payload для проверки не может быть null")
    private JsonNode payload;

    public CheckGroupRequest() {
        // Для Jackson
    }

    public CheckGroupRequest(String groupKey, JsonNode payload) {
        this.groupKey = groupKey;
        this.payload = payload;
    }

    public String getGroupKey() {
        return groupKey;
    }

    public void setGroupKey(String groupKey) {
        this.groupKey = groupKey;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public void setPayload(JsonNode payload) {
        this.payload = payload;
    }
}