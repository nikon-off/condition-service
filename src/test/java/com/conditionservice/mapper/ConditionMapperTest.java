package com.conditionservice.mapper;

import com.conditionservice.dto.request.CreateConditionDto;
import com.conditionservice.dto.request.CreateGroupDto;
import com.conditionservice.entity.Condition;
import com.conditionservice.entity.ConditionGroup;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты ручного маппера DTO ↔ Entity.
 */
class ConditionMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void toCondition_mapsConditionKeyAndPayload() throws Exception {
        JsonNode payload = objectMapper.readTree("{\"region\":\"ru\"}");
        CreateConditionDto dto = new CreateConditionDto("cond-1", payload);

        Condition condition = ConditionMapper.toCondition(dto);

        assertThat(condition.getConditionKey()).isEqualTo("cond-1");
        assertThat(condition.getPayload()).isEqualTo(payload);
    }

    @Test
    void toGroup_mapsGroupKeyIntoNameSinceDtoHasNoName() {
        CreateGroupDto dto = new CreateGroupDto("group-alpha", "Описание группы");

        ConditionGroup group = ConditionMapper.toGroup(dto);

        assertThat(group.getKey()).isEqualTo("group-alpha");
        // В DTO нет поля name → подставляем бизнес-ключ как имя (колонка NOT NULL)
        assertThat(group.getName()).isEqualTo("group-alpha");
        assertThat(group.getDescription()).isEqualTo("Описание группы");
    }
}