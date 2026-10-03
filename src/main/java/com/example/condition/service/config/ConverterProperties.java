package com.example.condition.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Внешние настройки трансформатора XML-фильтров 1С DCS.
 *
 * <p>
 * Биндится из секции {@code converter.transformer} файла
 * {@code application.yml} (реляксация kebab-case: свойство
 * {@code preserve-original-field-names} → поле
 * {@code preserveOriginalFieldNames}).
 * </p>
 *
 * <pre>{@code
 * converter:
 *   transformer:
 *     preserve-original-field-names: false
 *     field-mapping: {}
 * }</pre>
 */
@ConfigurationProperties(prefix = "converter.transformer")
public class ConverterProperties {

    /**
     * Строгий режим имён полей: {@code true} — имена полей из XML
     * передаются в канонический JSON без изменений (регистр и точки),
     * {@code false} — применяется snake_case-нормализация.
     */
    private boolean preserveOriginalFieldNames = false;

    /**
     * Явный маппинг «сырое имя 1С» → «каноническое имя» поля.
     * Пустой по умолчанию; применяется до общей нормализации.
     */
    private Map<String, String> fieldMapping = new HashMap<>();

    public boolean isPreserveOriginalFieldNames() {
        return preserveOriginalFieldNames;
    }

    public void setPreserveOriginalFieldNames(boolean preserveOriginalFieldNames) {
        this.preserveOriginalFieldNames = preserveOriginalFieldNames;
    }

    public Map<String, String> getFieldMapping() {
        return fieldMapping;
    }

    public void setFieldMapping(Map<String, String> fieldMapping) {
        this.fieldMapping = fieldMapping != null ? fieldMapping : new HashMap<>();
    }
}