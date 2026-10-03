package com.conditionservice.integration;

import com.example.condition.service.converter.ConditionConverterService;
import com.example.condition.service.converter.exception.ConversionException;
import com.conditionservice.repository.ConditionRepository;
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
 * Интеграционный тест персистентной стадии конвертации
 * ({@link ConditionConverterService#saveOrConvert}) против реального
 * PostgreSQL 17 в Testcontainers.
 *
 * <p>
 * Проверяются сценарии Insert/Update Upsert-логики (доработка ТЗ №3):
 * </p>
 * <ul>
 * <li><b>Insert:</b> сохранение условия из {@code test-filter.xml} создаёт
 * запись, чей {@code payload} — валидный JSON-объект
 * ({@code jsonb_typeof(payload) = 'object'});</li>
 * <li><b>Update:</b> повторное сохранение того же XML не создаёт дублей
 * (ON CONFLICT (condition_key) DO UPDATE), возвращает тот же id,
 * а {@code updated_at} обновляется;</li>
 * <li>невалидный XML → {@link ConversionException}, запись не создаётся.</li>
 * </ul>
 */
class ConditionPersistenceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ConditionConverterService converterService;

    @Autowired
    private ConditionRepository conditionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String TEST_FILTER_XML = readTestFilterXml();

    private static final String COUNT_BY_ID_SQL = "SELECT count(*) FROM conditions WHERE id = ?";

    private static final String PAYLOAD_TYPE_BY_ID_SQL = "SELECT jsonb_typeof(payload) FROM conditions WHERE id = ?";

    private static final String PAYLOAD_BY_ID_SQL = "SELECT payload FROM conditions WHERE id = ?";

    private static final String UPDATED_AT_BY_ID_SQL = "SELECT updated_at FROM conditions WHERE id = ?";

    /**
     * Сценарий Insert: условие сохраняется, payload — JSON-объект.
     */
    @Test
    void saveOrConvertInsertsConditionWithJsonObjectPayload() {
        Long id = converterService.saveOrConvert(TEST_FILTER_XML);

        assertThat(id).isNotNull();

        // Проверка через JPA-репозиторий: ровно одна запись.
        assertThat(conditionRepository.count()).isEqualTo(1L);
        // Проверка через JDBC: запись существует по возвращённому id.
        assertThat(jdbcTemplate.queryForObject(COUNT_BY_ID_SQL, Long.class, id)).isEqualTo(1L);

        // CHECK-ограничение jsonb_typeof(payload) = 'object' выполнено.
        String type = jdbcTemplate.queryForObject(PAYLOAD_TYPE_BY_ID_SQL, String.class, id);
        assertThat(type).isEqualTo("object");

        // Структура соответствует каноническому условию из test-filter.xml:
        // logic + 2 правила (AND-группа).
        JsonNode stored = readPayload(id);
        assertThat(stored.isObject()).isTrue();
        assertThat(stored.path("logic").asText()).isEqualTo("AND");
        assertThat(stored.path("rules")).hasSize(2);
    }

    /**
     * Сценарий Update: повторное сохранение того же XML не создаёт дублей,
     * id тот же, updated_at обновился.
     */
    @Test
    void repeatedSaveOrConvertUpdatesWithoutDuplicates() {
        Long firstId = converterService.saveOrConvert(TEST_FILTER_XML);
        Timestamp firstUpdatedAt = jdbcTemplate.queryForObject(
                UPDATED_AT_BY_ID_SQL, Timestamp.class, firstId);

        // Гарантируем различимую разницу во времени между NOW() вызовами.
        sleepMillis(20);
        Long secondId = converterService.saveOrConvert(TEST_FILTER_XML);
        Timestamp secondUpdatedAt = jdbcTemplate.queryForObject(
                UPDATED_AT_BY_ID_SQL, Timestamp.class, secondId);

        // ON CONFLICT DO UPDATE RETURNING id возвращает id существующей строки.
        assertThat(secondId).isEqualTo(firstId);

        // Дублей нет: в таблице ровно одна запись (JPA count + SQL count по id).
        assertThat(conditionRepository.count()).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(COUNT_BY_ID_SQL, Long.class, firstId)).isEqualTo(1L);

        // updated_at обновился при повторном сохранении.
        assertThat(secondUpdatedAt).isAfter(firstUpdatedAt);
    }

    /**
     * Невалидный XML: ConversionException, запись не создаётся.
     */
    @Test
    void invalidXmlThrowsConversionExceptionAndPersistsNothing() {
        assertThatThrownBy(() -> converterService.saveOrConvert("<Settings><filter>"))
                .isInstanceOf(ConversionException.class)
                .hasMessageContaining("Невалидный XML-фильтр");

        assertThat(conditionRepository.count()).isZero();
        Long total = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM conditions", Long.class);
        assertThat(total).isZero();
    }

    /**
     * Читает JSONB-колонку payload как строку и разбирает в {@link JsonNode}.
     * JdbcTemplate не умеет конвертировать PGobject в JsonNode напрямую.
     */
    private JsonNode readPayload(Long id) {
        String json = jdbcTemplate.queryForObject(PAYLOAD_BY_ID_SQL, String.class, id);
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