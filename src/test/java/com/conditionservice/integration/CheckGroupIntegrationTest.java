package com.conditionservice.integration;

import com.conditionservice.dto.response.CheckResultDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционный (end-to-end) тест проверки групп через {@code POST /v1/check-group}
 * против реального PostgreSQL 17 в Testcontainers.
 *
 * <p>Проверяется оператор containment {@code @>}: все условия группы должны быть
 * подмножеством переданного payload. Тесты покрывают:</p>
 * <ul>
 *   <li>полное совпадение → {@code matched=true};</li>
 *   <li>игнорирование лишних полей входящего объекта;</li>
 *   <li>чувствительность к типу значений (число {@code 5000} ≠ строка {@code "5000"});</li>
 *   <li>частичное совпадение → {@code matched=false} c корректным {@code matchedCount};</li>
 *   <li>пустая группа → {@code matched=false};</li>
 *   <li>отсутствующая группа → HTTP 404.</li>
 * </ul>
 */
class CheckGroupIntegrationTest extends BaseIntegrationTest {

    @Test
    void allConditionsMatchReturnsTrue() {
        createCondition("cond-region", "{\"region\": \"Moscow\"}");
        createCondition("cond-segment", "{\"segment\": \"premium\"}");
        createGroup("group-premium-moscow", "Premium в Москве");
        bindConditions("group-premium-moscow", java.util.List.of("cond-region", "cond-segment"));

        ResponseEntity<CheckResultDto> response =
                checkGroup("group-premium-moscow", """
                        {"region": "Moscow", "segment": "premium"}
                        """);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        CheckResultDto result = response.getBody();
        assertThat(result).isNotNull();
        assertThat(result.matched()).isTrue();
        assertThat(result.totalConditions()).isEqualTo(2);
        assertThat(result.matchedCount()).isEqualTo(2);
    }

    @Test
    void extraFieldsInPayloadAreIgnored() {
        createCondition("cond-required", "{\"region\": \"Moscow\", \"segment\": \"premium\"}");
        createGroup("group-extra-fields", null);
        bindConditions("group-extra-fields", java.util.List.of("cond-required"));

        // Входящий объект содержит лишние поля "channel" и "loyaltyLevel",
        // которые не указаны в условии — они игнорируются оператором @>.
        ResponseEntity<CheckResultDto> response =
                checkGroup("group-extra-fields", """
                        {"region": "Moscow", "segment": "premium", "channel": "mobile", "loyaltyLevel": 3}
                        """);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        CheckResultDto result = response.getBody();
        assertThat(result).isNotNull();
        assertThat(result.matched()).isTrue();
        assertThat(result.totalConditions()).isEqualTo(1);
        assertThat(result.matchedCount()).isEqualTo(1);
    }

    @Test
    void typeMismatchPreventsMatch() {
        createCondition("cond-price-number", "{\"price\": 5000}");
        createGroup("group-type-check", null);
        bindConditions("group-type-check", java.util.List.of("cond-price-number"));

        // Условие хранит число 5000, а во входящем payload приходит строка "5000".
        // Оператор @> чувствителен к типу: число ≠ строка → совпадения нет.
        ResponseEntity<CheckResultDto> response =
                checkGroup("group-type-check", "{\"price\": \"5000\"}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        CheckResultDto result = response.getBody();
        assertThat(result).isNotNull();
        assertThat(result.matched()).isFalse();
        assertThat(result.totalConditions()).isEqualTo(1);
        assertThat(result.matchedCount()).isZero();
    }

    @Test
    void partialMatchReturnsFalseWithCount() {
        createCondition("cond-region", "{\"region\": \"Moscow\"}");
        createCondition("cond-segment", "{\"segment\": \"premium\"}");
        createGroup("group-partial", null);
        bindConditions("group-partial", java.util.List.of("cond-region", "cond-segment"));

        // Совпадает только одно из двух условий → matched=false, matchedCount=1.
        ResponseEntity<CheckResultDto> response =
                checkGroup("group-partial", "{\"region\": \"Moscow\"}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        CheckResultDto result = response.getBody();
        assertThat(result).isNotNull();
        assertThat(result.matched()).isFalse();
        assertThat(result.totalConditions()).isEqualTo(2);
        assertThat(result.matchedCount()).isEqualTo(1);
    }

    @Test
    void emptyGroupReturnsFalse() {
        createGroup("group-empty", null);

        // Группа существует, но не содержит условий → по правилу matched=false.
        ResponseEntity<CheckResultDto> response =
                checkGroup("group-empty", "{\"region\": \"Moscow\"}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        CheckResultDto result = response.getBody();
        assertThat(result).isNotNull();
        assertThat(result.matched()).isFalse();
        assertThat(result.totalConditions()).isZero();
        assertThat(result.matchedCount()).isZero();
    }

    @Test
    void missingGroupReturns404() {
        ResponseEntity<CheckResultDto> response =
                checkGroup("group-does-not-exist", "{\"region\": \"Moscow\"}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}