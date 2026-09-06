package com.conditionservice.integration;

import com.conditionservice.dto.response.CheckResultDto;
import com.conditionservice.dto.response.ConditionResponseDto;
import com.conditionservice.dto.response.GroupResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционный тест CRUD-операций над условиями и группами
 * против реального PostgreSQL 17 в Testcontainers.
 *
 * <p>Покрывает создание и получение условий, создание групп, привязку условий
 * к группе (полная идемпотентная замена состава) и проверку персистентности
 * связей через {@code /v1/check-group}.</p>
 */
class CrudIntegrationTest extends BaseIntegrationTest {

    @Test
    void createConditionReturnsCreatedAndRetrievable() {
        ResponseEntity<ConditionResponseDto> created =
                createCondition("cond-crud", "{\"region\": \"SPb\"}");

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).isNotNull();
        assertThat(created.getBody().conditionKey()).isEqualTo("cond-crud");
        assertThat(created.getBody().payload().path("region").asText()).isEqualTo("SPb");
        assertThat(created.getBody().createdAt()).isNotNull();

        ResponseEntity<ConditionResponseDto> fetched = getCondition("cond-crud");

        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody()).isNotNull();
        assertThat(fetched.getBody().conditionKey()).isEqualTo("cond-crud");
        assertThat(fetched.getBody().payload().path("region").asText()).isEqualTo("SPb");
    }

    @Test
    void getMissingConditionReturns404() {
        ResponseEntity<ConditionResponseDto> fetched = getCondition("no-such-condition");
        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createGroupReturnsCreatedWithNameEqualToKey() {
        ResponseEntity<GroupResponseDto> created =
                createGroup("group-crud", "Тестовая группа");

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).isNotNull();
        assertThat(created.getBody().key()).isEqualTo("group-crud");
        // В DTO нет поля name, поэтому маппер подставляет в name бизнес-ключ.
        assertThat(created.getBody().name()).isEqualTo("group-crud");
        assertThat(created.getBody().description()).isEqualTo("Тестовая группа");
        assertThat(created.getBody().createdAt()).isNotNull();
    }

    @Test
    void replaceConditionsPersistsBinding() {
        createCondition("cond-a", "{\"fieldA\": 1}");
        createCondition("cond-b", "{\"fieldB\": 2}");
        createGroup("group-bind", null);

        ResponseEntity<Void> bind = bindConditions("group-bind",
                java.util.List.of("cond-a", "cond-b"));

        assertThat(bind.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Привязка персистентна: check-group видит оба условия и совпадает.
        ResponseEntity<CheckResultDto> check = checkGroup("group-bind", """
                {"fieldA": 1, "fieldB": 2}
                """);

        assertThat(check.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(check.getBody()).isNotNull();
        assertThat(check.getBody().matched()).isTrue();
        assertThat(check.getBody().totalConditions()).isEqualTo(2);
        assertThat(check.getBody().matchedCount()).isEqualTo(2);
    }

    @Test
    void replaceConditionsIsIdempotent() {
        createCondition("cond-x", "{\"x\": true}");
        createGroup("group-idem", null);

        bindConditions("group-idem", java.util.List.of("cond-x"));
        // Повторный вызов с тем же составом не меняет результат.
        ResponseEntity<Void> second = bindConditions("group-idem", java.util.List.of("cond-x"));

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<CheckResultDto> check = checkGroup("group-idem", "{\"x\": true}");
        assertThat(check.getBody()).isNotNull();
        assertThat(check.getBody().totalConditions()).isEqualTo(1);
        assertThat(check.getBody().matched()).isTrue();
    }
}