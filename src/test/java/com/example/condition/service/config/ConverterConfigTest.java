package com.example.condition.service.config;

import com.example.condition.service.converter.ConditionConverterService;
import com.example.condition.service.converter.parser.DcsFilterParser;
import com.example.condition.service.converter.transformer.CanonicalConditionBuilder;
import com.example.condition.service.converter.transformer.TransformerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Контекстный срез-тест конфигурации {@link ConverterConfig}.
 *
 * <p>
 * Проверяет критерии приёмки ТЗ №2:
 * <ul>
 * <li>бин {@link ConditionConverterService} создаётся Spring-контекстом;</li>
 * <li>настройки секции {@code converter.transformer} подхватываются и
 * передаются в {@link CanonicalConditionBuilder} через
 * {@link TransformerConfig}.</li>
 * </ul>
 * Срез поднимает только модуль конвертации — без БД/Testcontainers.
 * </p>
 */
class ConverterConfigTest {

        private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                        .withUserConfiguration(ConverterConfig.class, ConditionConverterService.class);

        @Test
        void createsAllConverterBeansFromExternalProperties() {
                contextRunner
                                .withPropertyValues(
                                                "converter.transformer.preserve-original-field-names=true",
                                                "converter.transformer.field-mapping.CityFias=city_fias")
                                .run(context -> {
                                        assertThat(context).hasSingleBean(ConverterProperties.class);
                                        assertThat(context).hasSingleBean(TransformerConfig.class);
                                        assertThat(context).hasSingleBean(DcsFilterParser.class);
                                        assertThat(context).hasSingleBean(CanonicalConditionBuilder.class);
                                        assertThat(context).hasSingleBean(ConditionConverterService.class);

                                        TransformerConfig config = context.getBean(TransformerConfig.class);
                                        assertThat(config.isPreserveOriginalFieldNames())
                                                        .as("Настройка preserve-original-field-names попадает в TransformerConfig")
                                                        .isTrue();
                                        assertThat(config.getFieldMapping())
                                                        .as("Настройка field-mapping попадает в TransformerConfig")
                                                        .containsEntry("CityFias", "city_fias");
                                });
        }

        @Test
        void usesDefaultsWhenPropertiesAreAbsent() {
                contextRunner.run(context -> {
                        TransformerConfig config = context.getBean(TransformerConfig.class);
                        assertThat(config.isPreserveOriginalFieldNames())
                                        .as("По умолчанию строгий режим выключен")
                                        .isFalse();
                        assertThat(config.getFieldMapping())
                                        .as("По умолчанию маппинг полей пуст")
                                        .isEmpty();
                });
        }
}