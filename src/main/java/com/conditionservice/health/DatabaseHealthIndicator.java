package com.conditionservice.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Кастомный {@link HealthIndicator} для проверки доступности БД.
 *
 * <p>Выполняет лёгкий запрос {@code SELECT 1} к источнику данных. Если БД
 * недоступна (или запрос вернул неожиданный результат) — компонент сообщает
 * статус {@link Status#DOWN}, и общий статус {@code /actuator/health} также
 * становится {@code DOWN} (в т.ч. группа {@code readiness}).</p>
 *
 * <p>Используется конструкторная инъекция (без {@code @Autowired} на поле),
 * как принято в проекте.</p>
 */
@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    /** Лёгкий проверочный запрос, не нагружающий БД. */
    private static final String PING_QUERY = "SELECT 1";

    private final JdbcTemplate jdbcTemplate;

    public DatabaseHealthIndicator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Health health() {
        try {
            Integer result = jdbcTemplate.queryForObject(PING_QUERY, Integer.class);
            if (result != null && result == 1) {
                return Health.up()
                        .withDetail("database", "PostgreSQL")
                        .withDetail("validationQuery", PING_QUERY)
                        .build();
            }
            return Health.down()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("error", "Unexpected result from validation query")
                    .build();
        } catch (Exception ex) {
            // DataAccessException и любые другие ошибки подключения -> DOWN
            return Health.down()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("validationQuery", PING_QUERY)
                    .withDetail("error", ex.getClass().getSimpleName())
                    .withDetail("message", ex.getMessage())
                    .build();
        }
    }
}