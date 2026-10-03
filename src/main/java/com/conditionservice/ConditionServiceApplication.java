package com.conditionservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Точка входа «Сервиса проверки условий отбора» (Condition Checking Service).
 *
 * <p>
 * {@code @SpringBootApplication} включает автоконфигурацию Spring Boot 3.x,
 * сканирование компонентов пакета {@code com.conditionservice} и включение
 * {@code @Configuration} для JPA/Flyway/Actuator.
 * </p>
 *
 * <p>
 * Пакет {@code com.example.condition.service} (модуль конвертации
 * XML-фильтров 1С DCS) находится вне корневого пакета приложения, поэтому
 * добавляется в {@code scanBasePackages} явно — иначе бины
 * {@code ConditionConverterService}, {@code ConverterConfig} и пр. не будут
 * созданы контекстом.
 * </p>
 */
@SpringBootApplication(scanBasePackages = {
        "com.conditionservice",
        "com.example.condition.service"
})
public class ConditionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConditionServiceApplication.class, args);
    }
}