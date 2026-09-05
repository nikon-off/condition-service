package com.conditionservice.service;

import com.conditionservice.dto.response.CheckResultDto;
import com.conditionservice.entity.ConditionGroup;
import com.conditionservice.exception.GroupNotFoundException;
import com.conditionservice.repository.ConditionGroupRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ядро системы проверки условий отбора.
 *
 * <p>Правило проверки: группа «совпала» ({@code matched=true}) тогда и только тогда,
 * когда <b>все</b> условия группы являются подмножеством переданного входящего
 * объекта. Проверка выполняется оператором containment {@code @>} в PostgreSQL
 * (native query, см. {@link ConditionGroupRepository#countMatchingConditions}).</p>
 *
 * <ul>
 *   <li>Группа не найдена по ключу → {@link GroupNotFoundException} (HTTP 404).</li>
 *   <li>Пустая группа (0 условий) → {@code matched=false}.</li>
 *   <li>Тип значений учитывается (число 5000 ≠ строка "5000").</li>
 *   <li>Лишние поля входящего JSON игнорируются (свойство {@code @>}).</li>
 * </ul>
 */
@Service
public class ConditionCheckService {

    private final ConditionGroupRepository groupRepository;
    private final ObjectMapper objectMapper;

    public ConditionCheckService(ConditionGroupRepository groupRepository, ObjectMapper objectMapper) {
        this.groupRepository = groupRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Проверяет, удовлетворяет ли входящий объект всем условиям группы.
     *
     * @param groupKey бизнес-ключ группы условий
     * @param payload  входящие поля, проверяемые на containment
     * @return результат проверки {@link CheckResultDto}
     * @throws GroupNotFoundException если группа с указанным ключом не существует
     */
    @Transactional(readOnly = true)
    public CheckResultDto checkGroup(String groupKey, JsonNode payload) {
        ConditionGroup group = groupRepository.findByKey(groupKey)
                .orElseThrow(() -> new GroupNotFoundException(groupKey));

        long totalConditions = groupRepository.countConditionsByGroupId(group.getId());

        // Пустая группа не может «совпасть»
        if (totalConditions == 0) {
            return new CheckResultDto(false, 0, 0);
        }

        long matchedConditions = groupRepository.countMatchingConditions(
                group.getId(), serialize(payload));

        boolean allMatched = matchedConditions == totalConditions;
        return new CheckResultDto(allMatched, (int) totalConditions, (int) matchedConditions);
    }

    /**
     * Сериализует {@link JsonNode} в компактный JSON-текст для передачи в native query.
     *
     * @param payload JSON-дерево
     * @return JSON-строка
     */
    private String serialize(JsonNode payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            // JsonNode из Jackson всегда сериализуем без ошибок; случай недостижим
            throw new IllegalStateException("Не удалось сериализовать payload", e);
        }
    }
}