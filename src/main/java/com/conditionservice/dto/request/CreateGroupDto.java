package com.conditionservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Входящий контракт для создания группы условий отбора.
 *
 * <p>{@code groupKey} — бизнес-ключ группы (используется в {@code check-group}),
 * {@code description} — необязательное человекочитаемое описание группы.</p>
 */
public class CreateGroupDto {

    @NotBlank(message = "Ключ группы не может быть пустым")
    @Size(max = 255, message = "Ключ группы не должен превышать 255 символов")
    private String groupKey;

    @Size(max = 1000, message = "Описание группы не должно превышать 1000 символов")
    private String description;

    public CreateGroupDto() {
        // Для Jackson
    }

    public CreateGroupDto(String groupKey, String description) {
        this.groupKey = groupKey;
        this.description = description;
    }

    public String getGroupKey() {
        return groupKey;
    }

    public void setGroupKey(String groupKey) {
        this.groupKey = groupKey;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}