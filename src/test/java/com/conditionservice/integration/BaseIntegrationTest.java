package com.conditionservice.integration;

import com.conditionservice.dto.request.CreateConditionDto;
import com.conditionservice.dto.request.CreateGroupDto;
import com.conditionservice.dto.request.UpdateGroupConditionsDto;
import com.conditionservice.dto.response.CheckResultDto;
import com.conditionservice.dto.response.ConditionResponseDto;
import com.conditionservice.dto.response.GroupResponseDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;

/**
 * Базовый класс интеграционных тестов с Testcontainers.
 *
 * <p>Поднимает настоящий PostgreSQL 17 в Docker-контейнере и настраивает
 * Spring Context на него через {@link DynamicPropertySource} (JDBC URL,
 * username, password). Параметры из {@code application.yml} (localhost)
 * при этом автоматически переопределяются.</p>
 *
 * <p>Flyway-миграции ({@code V1__init.sql}, {@code V2__add_condition_key.sql})
 * применяются автоматически при старте Spring Context (свойство
 * {@code spring.flyway.enabled=true}).</p>
 *
 * <p><b>Почему контейнер стартует один раз (singleton):</b> все конкретные
 * интеграционные тесты наследуют этот класс и используют <i>одну и ту же</i>
 * статическую ссылку {@link #POSTGRES}. Если бы контейнер управлялся
 * аннотацией {@code @Container} из {@code @Testcontainers}, он останавливался бы
 * после <b>каждого</b> тестового класса. При этом Spring кэширует
 * ApplicationContext (конфигурация классов идентична) и держит JDBC URL со
 * «старого» порта контейнера, тогда как перезапущенный контейнер получает
 * <i>новый</i> порт → тесты следующего класса падали бы с
 * {@code Connection refused}. Поэтому контейнер стартует в статическом
 * инициализаторе один раз и живёт весь прогон тестов (уборка — через Ryuk).</p>
 *
 * <p><b>Изоляция тестов:</b> перед каждым тестом БД очищается
 * (TRUNCATE с RESTART IDENTITY), поэтому тесты не зависят друг от друга.</p>
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class BaseIntegrationTest {

    /** Единственный контейнер PostgreSQL 17 на весь тестовый прогон. */
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("test_db")
            .withUsername("test_user")
            .withPassword("test_pass");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        // Стартуем ровно один раз; контейнер разделяется между всеми подклассами.
        POSTGRES.start();
    }

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Передаёт параметры подключения тестового контейнера в Spring Context.
     * Вызывается при инициализации (кэшируемого) контекста — контейнер к этому
     * моменту уже запущен, поэтому URL стабилен на весь прогон.
     *
     * @param registry реестр динамических свойств Spring
     */
    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    /**
     * Очищает таблицы БД перед каждым тестом, гарантируя изоляцию.
     * CASCADE + RESTART IDENTITY сбрасывают последовательности (детерминированные id).
     */
    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute(
                "TRUNCATE TABLE group_conditions, conditions, condition_groups RESTART IDENTITY CASCADE");
    }

    // ---------------------------------------------------------------------------
    // Helpers для вызова REST API (для удобства переиспользования в тестах)
    // ---------------------------------------------------------------------------

    protected ResponseEntity<ConditionResponseDto> createCondition(String conditionKey, String payloadJson) {
        String body = toJson(new CreateConditionDto(conditionKey, toJsonNode(payloadJson)));
        return restTemplate.postForEntity("/v1/conditions",
                new HttpEntity<>(body, jsonHeaders()), ConditionResponseDto.class);
    }

    protected ResponseEntity<ConditionResponseDto> getCondition(String conditionKey) {
        return restTemplate.getForEntity("/v1/conditions/" + conditionKey, ConditionResponseDto.class);
    }

    protected ResponseEntity<GroupResponseDto> createGroup(String groupKey, String description) {
        String body = toJson(new CreateGroupDto(groupKey, description));
        return restTemplate.postForEntity("/v1/groups",
                new HttpEntity<>(body, jsonHeaders()), GroupResponseDto.class);
    }

    protected ResponseEntity<Void> bindConditions(String groupKey, List<String> conditionKeys) {
        String body = toJson(new UpdateGroupConditionsDto(conditionKeys));
        return restTemplate.exchange("/v1/groups/" + groupKey + "/conditions",
                HttpMethod.PUT, new HttpEntity<>(body, jsonHeaders()), Void.class);
    }

    protected ResponseEntity<CheckResultDto> checkGroup(String groupKey, String payloadJson) {
        String body = """
                {"groupKey": "%s", "payload": %s}
                """.formatted(groupKey, payloadJson);
        return restTemplate.postForEntity("/v1/check-group",
                new HttpEntity<>(body, jsonHeaders()), CheckResultDto.class);
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private static JsonNode toJsonNode(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Некорректный JSON: " + json, e);
        }
    }

    private static String toJson(Object dto) {
        try {
            return MAPPER.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Не удалось сериализовать DTO", e);
        }
    }
}