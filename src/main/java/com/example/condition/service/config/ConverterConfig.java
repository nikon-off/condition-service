package com.example.condition.service.config;

import com.example.condition.service.converter.parser.DcsFilterParser;
import com.example.condition.service.converter.transformer.CanonicalConditionBuilder;
import com.example.condition.service.converter.transformer.TransformerConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация процесса конвертации XML-фильтров 1С DCS.
 *
 * <p>
 * Регистрирует {@link ConverterProperties} (типобезопасный биндинг секции
 * {@code converter.transformer}) и собирает граф бинов конвертера:
 * {@link TransformerConfig} из внешних настроек, {@link DcsFilterParser}
 * и {@link CanonicalConditionBuilder} с этой конфигурацией.
 * </p>
 */
@Configuration
@EnableConfigurationProperties(ConverterProperties.class)
public class ConverterConfig {

    /**
     * Собирает конфигурацию трансформатора из внешних настроек
     * {@code converter.transformer}.
     *
     * @param properties типизированные настройки секции
     *                   {@code converter.transformer}
     * @return неизменяемая конфигурация трансформатора
     */
    @Bean
    public TransformerConfig transformerConfig(ConverterProperties properties) {
        return new TransformerConfig(
                properties.isPreserveOriginalFieldNames(),
                properties.getFieldMapping());
    }

    /**
     * Бин парсера XML-фильтров 1С DCS.
     *
     * @return новый экземпляр {@link DcsFilterParser}
     */
    @Bean
    public DcsFilterParser dcsFilterParser() {
        return new DcsFilterParser();
    }

    /**
     * Бин нормализатора результата парсинга в канонический формат.
     *
     * @param transformerConfig конфигурация трансформатора (внешние настройки)
     * @return новый экземпляр {@link CanonicalConditionBuilder}
     */
    @Bean
    public CanonicalConditionBuilder canonicalConditionBuilder(TransformerConfig transformerConfig) {
        return new CanonicalConditionBuilder(transformerConfig);
    }
}