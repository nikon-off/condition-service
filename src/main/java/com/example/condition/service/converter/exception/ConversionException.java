package com.example.condition.service.converter.exception;

/**
 * Исключение процесса конвертации XML-фильтра 1С DCS в каноническое условие.
 *
 * <p>
 * Транслирует ошибки формата входных данных (например,
 * {@link IllegalArgumentException} парсера на невалидном XML) в единый
 * тип исключения сервисного слоя — тем самым ошибки формата отделяются
 * от ошибок бизнес-логики конвертации.
 * </p>
 */
public class ConversionException extends RuntimeException {

    /**
     * Конструктор с сообщением.
     *
     * @param message описание ошибки конвертации
     */
    public ConversionException(String message) {
        super(message);
    }

    /**
     * Конструктор с сообщением и первопричиной.
     *
     * @param message описание ошибки конвертации
     * @param cause   исходное исключение (например,
     *                {@link IllegalArgumentException}
     *                парсера на невалидном XML)
     */
    public ConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}