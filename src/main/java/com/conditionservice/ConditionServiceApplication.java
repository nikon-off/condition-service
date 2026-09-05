package com.conditionservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Точка входа «Сервиса проверки условий отбора» (Condition Checking Service).
 *
 * <p>{@code @SpringBootApplication} включает автоконфигурацию Spring Boot 3.x,
 * сканирование компонентов пакета {@code com.conditionservice} и включение
 * {@code @Configuration} для JPA/Flyway/Actuator.</p>
 */
@SpringBootApplication
public class ConditionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConditionServiceApplication.class, args);
    }
}