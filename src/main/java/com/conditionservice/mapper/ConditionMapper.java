package com.conditionservice.mapper;

import com.conditionservice.dto.request.CreateConditionDto;
import com.conditionservice.dto.request.CreateGroupDto;
import com.conditionservice.dto.response.ConditionResponseDto;
import com.conditionservice.dto.response.GroupResponseDto;
import com.conditionservice.entity.Condition;
import com.conditionservice.entity.ConditionGroup;

/**
 * Ручной маппер DTO ↔ Entity.
 *
 * <p>Сознательно не используем MapStruct/ModelMapper — на текущем этапе
 * преобразования тривиальны (YAGNI), достаточно статических методов.</p>
 *
 * <p>Особенность {@code toGroup}: входящий DTO {@link CreateGroupDto} не содержит
 * поля {@code name}, но в таблице {@code condition_groups} колонка {@code name}
 * объявлена {@code NOT NULL}. Поэтому в качестве имени группы подставляется
 * её бизнес-ключ ({@code groupKey}), что сохраняет целостность схемы.</p>
 */
public final class ConditionMapper {

    private ConditionMapper() {
        // Утилитарный класс — запрещаем инстанцирование
    }

    /**
     * Преобразует входящий DTO создания условия в сущность {@link Condition}.
     *
     * @param dto входящий контракт (не может быть null)
     * @return новая сущность без персистентного id
     */
    public static Condition toCondition(CreateConditionDto dto) {
        return new Condition(dto.getConditionKey(), dto.getPayload());
    }

    /**
     * Преобразует входящий DTO создания группы в сущность {@link ConditionGroup}.
     *
     * @param dto входящий контракт (не может быть null)
     * @return новая сущность без персистентного id
     */
    public static ConditionGroup toGroup(CreateGroupDto dto) {
        return new ConditionGroup(
                dto.getGroupKey(),   // key
                dto.getGroupKey(),   // name ← подставляем business-ключ (в DTO нет name)
                dto.getDescription() // description (nullable)
        );
    }

    /**
     * Преобразует сущность {@link Condition} в ответное представление.
     *
     * @param condition сущность условия
     * @return ответный DTO
     */
    public static ConditionResponseDto toResponse(Condition condition) {
        return new ConditionResponseDto(
                condition.getConditionKey(),
                condition.getPayload(),
                condition.getCreatedAt(),
                condition.getUpdatedAt());
    }

    /**
     * Преобразует сущность {@link ConditionGroup} в ответное представление.
     *
     * @param group сущность группы
     * @return ответный DTO
     */
    public static GroupResponseDto toResponse(ConditionGroup group) {
        return new GroupResponseDto(
                group.getKey(),
                group.getName(),
                group.getDescription(),
                group.getCreatedAt(),
                group.getUpdatedAt());
    }
}