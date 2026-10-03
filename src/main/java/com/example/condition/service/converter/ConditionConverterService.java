package com.example.condition.service.converter;

import com.example.condition.service.converter.dto.CanonicalConditionDto;
import com.example.condition.service.converter.exception.ConversionException;
import com.example.condition.service.converter.parser.DcsFilterParser;
import com.example.condition.service.converter.transformer.CanonicalConditionBuilder;
import org.springframework.stereotype.Service;

/**
 * Сервис процесса конвертации XML-настроек 1С DCS в каноническое условие.
 *
 * <p>
 * Оркестрирует две стадии:
 * <ol>
 * <li>{@link DcsFilterParser#parse(String)} — разбор XML в промежуточный
 * {@link CanonicalConditionDto};</li>
 * <li>{@link CanonicalConditionBuilder#build(CanonicalConditionDto)} —
 * нормализация в канонический формат (поля, операторы, значения).</li>
 * </ol>
 * </p>
 *
 * <p>
 * Ошибки формата входного XML ({@link IllegalArgumentException} парсера)
 * транслируются в {@link ConversionException}, отделяя их от ошибок
 * логики нормализации.
 * </p>
 */
@Service
public class ConditionConverterService {

    private final DcsFilterParser parser;
    private final CanonicalConditionBuilder builder;

    /**
     * Конструктор с внедрением зависимостей.
     *
     * @param parser  парсер XML-настроек 1С DCS
     * @param builder нормализатор результата парсинга в канонический формат
     */
    public ConditionConverterService(DcsFilterParser parser, CanonicalConditionBuilder builder) {
        this.parser = parser;
        this.builder = builder;
    }

    /**
     * Конвертирует XML-фильтр 1С DCS в каноническое условие.
     *
     * @param xmlFilter сырой XML настроек DCS (поле {@code xmlFilter})
     * @return каноническое условие; пустой объект, если секции
     *         {@code <filter>} нет или входной XML пуст
     * @throws ConversionException если входной XML не является well-formed
     */
    public CanonicalConditionDto convert(String xmlFilter) {
        CanonicalConditionDto parsed;
        try {
            parsed = parser.parse(xmlFilter);
        } catch (IllegalArgumentException e) {
            throw new ConversionException(
                    "Невалидный XML-фильтр 1С DCS: " + e.getMessage(), e);
        }
        return builder.build(parsed);
    }
}