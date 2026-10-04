package com.nikonoff.conditionservice.dto.imports;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * Группа условий внутри импортируемого договора.
 * Группирует {@link ConditionImportDto} для последующей привязки через
 * таблицу group_conditions (M2M: conditions <-> condition_groups).
 */
@Data
@Builder
public class GroupImportDto {

    /** Идентификатор группы условий в 1С (uuid справочника). */
    @NotNull(message = "groupId не может быть null")
    private UUID groupId;

    /** Название группы условий. */
    @NotBlank(message = "groupName не может быть пустым")
    private String groupName;

    /** Условия, входящие в группу. */
    @Valid
    @NotNull(message = "conditions не может быть null")
    private List<ConditionImportDto> conditions;
}