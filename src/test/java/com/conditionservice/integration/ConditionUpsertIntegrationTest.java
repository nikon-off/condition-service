package com.conditionservice.integration;

import com.example.condition.service.converter.ConditionConverterService;
import com.example.condition.service.converter.dto.SavedCondition;
import com.example.condition.service.converter.exception.ConversionException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Интеграционный тест Upsert-логики сохранения канонического условия
 * против реального PostgreSQL 17 в Testcontainers.
 *
 * <p>
 * Проверяются критерии приёмки ТЗ №3:
 * </p>
 * <ul>
 * <li>повторная конвертация одного и того же XML не создаёт дублей
 * (ON CONFLICT (condition_key) DO UPDATE);</li>
 * <li>{@code payload} всегда хранится как JSON-объект
 * ({@code jsonb_typeof(payload) = 'object'});</li>
 * <li>при повторном вызове {@code updated_at} обновляется, а
 * {@code condition_key} и структура {@code payload} остаются прежними;</li>
 * <li>невалидный XML → {@link ConversionException}, запись не создаётся.</li>
 * </ul>
 */
class ConditionUpsertIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ConditionConverterService converterService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String TEST_FILTER_XML = readTestFilterXml();

    private static final String COUNT_SQL = "SELECT count(*) FROM conditions WHERE condition_key = ?";

    private static final String PAYLOAD_SQL = "SELECT payload FROM conditions WHERE condition_key = ?";

    private static final String PAYLOAD_TYPE_SQL = "SELECT jsonb_typeof(payload) FROM conditions WHERE condition_key = ?";

    private static final String UPDATED_AT_SQL = "SELECT updated_at FROM conditions WHERE condition_key = ?";

    @Test
    void saveOrConvertInsertsConditionWithJsonObjectPayload() {
        SavedCondition saved = converterService.saveOrConvert(TEST_FILTER_XML);

        // Ключ — детерминированный SHA-256 в hex (64 символа, нижний регистр).
        assertThat(saved.conditionKey()).matches("[0-9a-f]{64}");
        assertThat(saved.id()).isNotNull();
        assertThat(saved.payload()).isNotNull();

        // В БД ровно одна запись с этим ключом.
        Long count = jdbcTemplate.queryForObject(COUNT_SQL, Long.class, saved.conditionKey());
        assertThat(count).isEqualTo(1L);

        // Payload — JSON-объект (удовлетворяет CHECK jsonb_typeof(payload) = 'object').
        String type = jdbcTemplate.queryForObject(
                PAYLOAD_TYPE_SQL, String.class, saved.conditionKey());
        assertThat(type).isEqualTo("object");

        // Структура payload соответствует каноническому условию из test-filter.xml:
        // logic + 2 правила (AND-группа).
        JsonNode stored = readPayload(saved.conditionKey());
        assertThat(stored).isNotNull();
        assertThat(stored.isObject()).isTrue();
        assertThat(stored.path("logic").asText()).isEqualTo("AND");
        assertThat(stored.path("rules")).hasSize(2);
    }

    @Test
    void repeatedSaveOrConvertUpdatesWithoutDuplicates() {
        SavedCondition first = converterService.saveOrConvert(TEST_FILTER_XML);
        Timestamp firstUpdatedAt = jdbcTemplate.queryForObject(
                UPDATED_AT_SQL, Timestamp.class, first.conditionKey());

        // Гарантируем различимую разницу во времени между NOW() вызовами.
        sleepMillis(20);
        SavedCondition second = converterService.saveOrConvert(TEST_FILTER_XML);
        Timestamp secondUpdatedAt = jdbcTemplate.queryForObject(
                UPDATED_AT_SQL, Timestamp.class, second.conditionKey());

        // ON CONFLICT DO UPDATE RETURNING id возвращает id существующей строки.
        assertThat(second.id()).isEqualTo(first.id());

        // Дублей нет.
        Long count = jdbcTemplate.queryForObject(COUNT_SQL, Long.class, first.conditionKey());
        assertThat(count).isEqualTo(1L);

        // Бизнес-ключ детерминирован и не меняется.
        assertThat(second.conditionKey()).isEqualTo(first.conditionKey());

        // Структура payload не изменилась.
        JsonNode stored = readPayload(first.conditionKey());
        JsonNode expected = MAPPER.valueToTree(second.payload());
        assertThat(stored).isEqualTo(expected);

        // updated_at обновился при повторном сохранении.
        assertThat(secondUpdatedAt).isAfter(firstUpdatedAt);
    }

    @Test
    void invalidXmlThrowsConversionExceptionAndPersistsNothing() {
        String invalidXml = "<Settings><filter>";

        assertThatThrownBy(() -> converterService.saveOrConvert(invalidXml))
                .isInstanceOf(ConversionException.class)
                .hasMessageContaining("Невалидный XML-фильтр");

        Long total = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM conditions", Long.class);
        assertThat(total).isZero();
    }

    /**
     * Читает JSONB-колонку payload как строку и разбирает в {@link JsonNode}.
     * JdbcTemplate не умеет конвертировать PGobject в JsonNode напрямую.
     */
    private JsonNode readPayload(String conditionKey) {
        String json = jdbcTemplate.queryForObject(PAYLOAD_SQL, String.class, conditionKey);
        try {
            return MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Payload в БД не является валидным JSON: " + json, e);
        }
    }

    private static void sleepMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Ожидание прервано", e);
        }
    }

    private static String readTestFilterXml() {
        try {
            return new ClassPathResource("test-filter.xml")
                    .getContentAsString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось прочитать test-filter.xml", e);
        }
    }
}