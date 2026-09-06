package com.conditionservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Входящий контракт полной замены состава группы условий (PUT /v1/groups/{key}/conditions).
 *
 * <p>{@code conditionKeys} — бизнес-ключи условий, которые должны стать новым
 * составом группы. Пустой список очищает группу (идемпотентно).</p>
 */
public class UpdateGroupConditionsDto {

    @NotNull(message = "Список ключей условий не может быть null")
    private List<
            @NotBlank(message = "Ключ условия не может быть пустым")
            @Size(max = 255, message = "Ключ условия не должен превышать 255 символов")
            String> conditionKeys;

    public UpdateGroupConditionsDto() {
        // Для Jackson
    }

    public UpdateGroupConditionsDto(List<String> conditionKeys) {
        this.conditionKeys = conditionKeys;
    }

    public List<String> getConditionKeys() {
        return conditionKeys;
    }

    public void setConditionKeys(List<String> conditionKeys) {
        this.conditionKeys = conditionKeys;
    }
}