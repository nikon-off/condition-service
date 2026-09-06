package com.conditionservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Входящий контракт пакетной проверки групп условий (POST /v1/check-groups-batch).
 *
 * <p>{@code groups} — список проверяемых групп. На текущем этапе обработка
 * выполняется простым последовательным циклом; оптимизация — в бэклоге.</p>
 */
public class CheckGroupsBatchRequest {

    @NotNull(message = "Список проверяемых групп не может быть null")
    private List<@Valid CheckGroupRequest> groups;

    public CheckGroupsBatchRequest() {
        // Для Jackson
    }

    public CheckGroupsBatchRequest(List<CheckGroupRequest> groups) {
        this.groups = groups;
    }

    public List<CheckGroupRequest> getGroups() {
        return groups;
    }

    public void setGroups(List<CheckGroupRequest> groups) {
        this.groups = groups;
    }
}