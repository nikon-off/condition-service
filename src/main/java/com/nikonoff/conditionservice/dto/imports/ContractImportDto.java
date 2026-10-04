package com.nikonoff.conditionservice.dto.imports;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * Контракт импорта условий для одного договора (контракта).
 * Верхний уровень структуры импорта: договор -> группы -> условия.
 */
@Data
@Builder
public class ContractImportDto {

    /** Идентификатор договора в 1С (uuid справочника). */
    @NotNull(message = "contractId не может быть null")
    private UUID contractId;

    /** Номер договора (человекочитаемый бизнес-ключ). */
    @NotBlank(message = "contractNumber не может быть пустым")
    private String contractNumber;

    /** Группы условий, импортируемые вместе с договором. */
    @Valid
    @NotNull(message = "groups не может быть null")
    private List<GroupImportDto> groups;
}