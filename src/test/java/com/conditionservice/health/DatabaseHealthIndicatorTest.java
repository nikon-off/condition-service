package com.conditionservice.health;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Юнит-тесты {@link DatabaseHealthIndicator}: проверяем статус UP при доступной
 * БД и DOWN — при ошибке подключения (легкий запрос SELECT 1).
 */
class DatabaseHealthIndicatorTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

    @Test
    void healthShouldBeUpWhenDatabaseResponds() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);
        DatabaseHealthIndicator indicator = new DatabaseHealthIndicator(jdbcTemplate);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        verify(jdbcTemplate).queryForObject(eq("SELECT 1"), eq(Integer.class));
    }

    @Test
    void healthShouldBeDownWhenDatabaseUnavailable() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class)))
                .thenThrow(new DataAccessResourceFailureException("Connection refused"));
        DatabaseHealthIndicator indicator = new DatabaseHealthIndicator(jdbcTemplate);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("error");
        assertThat(health.getDetails()).containsEntry("error", "DataAccessResourceFailureException");
    }

    @Test
    void healthShouldBeDownWhenQueryReturnsUnexpectedValue() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(42);
        DatabaseHealthIndicator indicator = new DatabaseHealthIndicator(jdbcTemplate);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("error", "Unexpected result from validation query");
    }
}