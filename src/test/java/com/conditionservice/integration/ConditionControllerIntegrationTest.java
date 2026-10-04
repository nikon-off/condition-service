package com.conditionservice.integration;

import com.example.condition.service.converter.dto.SavedCondition;
import com.conditionservice.entity.Condition;
import com.conditionservice.repository.ConditionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционный тест endpoint {@code POST /api/v1/conditions/convert}
 * (конвертация XML-фильтра 1С DCS в канонический JSON + сохранение в БД).
 *
 * <p>
 * Сценарии (ТЗ №4):
 * </p>
 * <ul>
 * <li><b>Happy Path:</b> валидный XML → 201 Created, тело ответа — валидный
 * {@link SavedCondition} (id, SHA-256 conditionKey, логика AND, 2 правила);
 * в БД запись с {@code payload} типа {@code jsonb} объект
 * ({@code jsonb_typeof(payload) = 'object'});</li>
 * <li><b>Idempotency/Upsert:</b> повторный запрос того же XML → тот же id,
 * количество записей не растёт, {@code updated_at} обновляется;</li>
 * <li><b>Invalid XML:</b> битый XML → 400 Bad Request, запись не
 * создаётся.</li>
 * </ul>
 *
 * <p>
 * Входной XML — реальный контракт условия «ОСАГО Перестрахование»
 * (два {@code FilterItemComparison} напрямую в {@code <filter>}); отступы —
 * табы {@code \u0009}, как в нативной выгрузке 1С, что проверяет корректную
 * обработку спецсимволов парсером.
 * </p>
 *
 * <p>
 * NB: имена полей в каноническом выводе нормализованы в snake_case нижнего
 * регистра ({@code converter.transformer.preserve-original-field-names=false}
 * по умолчанию): {@code ПерестрахованиеРСА} → {@code перестрахование_рса}.
 * </p>
 */
class ConditionControllerIntegrationTest extends BaseIntegrationTest {

    private static final String CONVERT_URL = "/api/v1/conditions/convert";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Общие namespace-объявления из документации 1С DCS. */
    private static final String NAMESPACES = "xmlns=\"http://v8.1c.ru/8.1/data-composition-system/settings\" "
            + "xmlns:dcscor=\"http://v8.1c.ru/8.1/data-composition-system/core\" "
            + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
            + "xmlns:v8=\"http://v8.1c.ru/8.1/data/core\" "
            + "xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"";

    /**
     * XML-фильтр условия «ОСАГО Перестрахование» (реальный формат DCS из
     * test_to_json_condition.docx): два FilterItemComparison напрямую в
     * {@code <filter>} без FilterItemGroup → AND + 2 правила.
     * Отступы заданы escape-последовательностями {@code \t} (U+0009).
     */
    private static final String TEST_FILTER_XML = """
            <Settings %s>
            \t<filter>
            \t\t<item xsi:type="FilterItemComparison">
            \t\t\t<left xsi:type="dcscor:Field">ПерестрахованиеРСА</left>
            \t\t\t<comparisonType>Equal</comparisonType>
            \t\t\t<right xsi:type="xs:boolean">false</right>
            \t\t</item>
            \t\t<item xsi:type="FilterItemComparison">
            \t\t\t<left xsi:type="dcscor:Field">СтраховойПродукт</left>
            \t\t\t<comparisonType>Equal</comparisonType>
            \t\t\t<right xmlns:d4p1="http://v8.1c.ru/8.1/data/enterprise/current-config"
            \t\t\t       xsi:type="d4p1:CatalogRef.СтраховойПродукт">342ec861-3f65-11e6-9e61-7824af33beda</right>
            \t\t</item>
            \t</filter>
            </Settings>
            """.formatted(NAMESPACES);

    private static final String INVALID_XML = "<broken>";

    private static final String PAYLOAD_TYPE_BY_KEY_SQL = "SELECT jsonb_typeof(payload) FROM conditions WHERE condition_key = ?";

    private static final String PAYLOAD_BY_ID_SQL = "SELECT payload FROM conditions WHERE id = ?";

    private static final String UPDATED_AT_BY_ID_SQL = "SELECT updated_at FROM conditions WHERE id = ?";

    @Autowired
    private ConditionRepository conditionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Сценарий 1 (Happy Path): POST с валидным XML → 201 Created,
     * ответ содержит SavedCondition, в БД сохранён JSON-объект.
     */
    @Test
    void convertReturns201WithSavedConditionAndJsonObjectPayload() {
        ResponseEntity<SavedCondition> response = postConvert(TEST_FILTER_XML);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        SavedCondition saved = response.getBody();
        assertThat(saved).isNotNull();
        assertThat(saved.id()).isNotNull();
        assertThat(saved.conditionKey())
                .as("Бизнес-ключ = SHA-256 hex в нижнем регистре")
                .matches("[0-9a-f]{64}");

        // Структура канонического условия: AND + 2 правила.
        // Имена полей нормализованы в snake_case нижнего регистра
        // (preserve-original-field-names=false).
        assertThat(saved.condition()).isNotNull();
        assertThat(saved.condition().getLogic()).isEqualTo("AND");
        assertThat(saved.condition().getRules()).hasSize(2);
        assertThat(saved.condition().getRules().get(0).getField()).isEqualTo("перестрахование_рса");
        assertThat(saved.condition().getRules().get(0).getValues())
                .containsExactly("false");
        assertThat(saved.condition().getRules().get(1).getField()).isEqualTo("страховой_продукт");
        assertThat(saved.condition().getRules().get(1).getValues())
                .containsExactly("342ec861-3f65-11e6-9e61-7824af33beda");

        // Проверка БД: запись найдена по сгенерированному ключу.
        Optional<Condition> stored = conditionRepository.findByConditionKey(saved.conditionKey());
        assertThat(stored).isPresent();
        assertThat(stored.get().getId()).isEqualTo(saved.id());
        assertThat(conditionRepository.count()).isEqualTo(1L);

        // CHECK-ограничение jsonb_typeof(payload) = 'object' выполнено.
        String type = jdbcTemplate.queryForObject(
                PAYLOAD_TYPE_BY_KEY_SQL, String.class, saved.conditionKey());
        assertThat(type).isEqualTo("object");

        // Содержимое payload соответствует телу ответа (AND + 2 правила).
        JsonNode payload = readPayload(saved.id());
        assertThat(payload.isObject()).isTrue();
        assertThat(payload.path("logic").asText()).isEqualTo("AND");
        assertThat(payload.path("rules")).hasSize(2);
    }

    /**
     * Сценарий 2 (Idempotency/Upsert): повторная отправка того же XML
     * не создаёт дублей, id тот же, updated_at обновился.
     */
    @Test
    void repeatedConvertDoesNotDuplicateAndUpdatesUpdatedAt() {
        ResponseEntity<SavedCondition> first = postConvert(TEST_FILTER_XML);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        SavedCondition firstSaved = first.getBody();
        assertThat(firstSaved).isNotNull();
        Timestamp firstUpdatedAt = updatedAt(firstSaved.id());

        // Гарантируем различимую разницу во времени между NOW() вызовами.
        sleepMillis(20);
        ResponseEntity<SavedCondition> second = postConvert(TEST_FILTER_XML);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        SavedCondition secondSaved = second.getBody();
        assertThat(secondSaved).isNotNull();

        // ON CONFLICT DO UPDATE: id и ключ не меняются, дублей нет.
        assertThat(secondSaved.id()).isEqualTo(firstSaved.id());
        assertThat(secondSaved.conditionKey()).isEqualTo(firstSaved.conditionKey());
        assertThat(conditionRepository.count()).isEqualTo(1L);

        // updated_at обновился при повторном сохранении.
        Timestamp secondUpdatedAt = updatedAt(secondSaved.id());
        assertThat(secondUpdatedAt).isAfter(firstUpdatedAt);
    }

    /**
     * Сценарий 3 (Invalid XML): битый XML → 400 Bad Request,
     * запись в БД не создаётся.
     */
    @Test
    void invalidXmlReturns400AndPersistsNothing() {
        ResponseEntity<String> response = postConvertRaw(INVALID_XML);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .contains("Невалидный XML-фильтр 1С DCS");

        assertThat(conditionRepository.count()).isZero();
    }

    /**
     * POST /api/v1/conditions/convert с телом {"xmlFilter": "..."}.
     *
     * @param xmlFilter сырой XML-фильтр 1С DCS
     * @return ответ, десериализованный в {@link SavedCondition}
     */
    private ResponseEntity<SavedCondition> postConvert(String xmlFilter) {
        return restTemplate.postForEntity(CONVERT_URL, jsonRequest(xmlFilter), SavedCondition.class);
    }

    /**
     * POST /api/v1/conditions/convert с телом {"xmlFilter": "..."},
     * ответ читается как строка (для проверки тела ошибки).
     *
     * @param xmlFilter сырой XML-фильтр 1С DCS
     * @return ответ как строка
     */
    private ResponseEntity<String> postConvertRaw(String xmlFilter) {
        return restTemplate.postForEntity(CONVERT_URL, jsonRequest(xmlFilter), String.class);
    }

    private HttpEntity<String> jsonRequest(String xmlFilter) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            return new HttpEntity<>(MAPPER.writeValueAsString(Map.of("xmlFilter", xmlFilter)), headers);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Не удалось сериализовать тело запроса", e);
        }
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

    private Timestamp updatedAt(Long id) {
        return jdbcTemplate.queryForObject(UPDATED_AT_BY_ID_SQL, Timestamp.class, id);
    }

    private static void sleepMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Ожидание прервано", e);
        }
    }
}