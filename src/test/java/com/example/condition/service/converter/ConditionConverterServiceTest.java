package com.example.condition.service.converter;

import com.example.condition.service.converter.dto.CanonicalConditionDto;
import com.example.condition.service.converter.dto.RuleDto;
import com.example.condition.service.converter.exception.ConversionException;
import com.example.condition.service.converter.parser.DcsFilterParser;
import com.example.condition.service.converter.transformer.CanonicalConditionBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Юнит-тесты сервиса {@link ConditionConverterService} (Mockito).
 *
 * <p>
 * Проверяется делегирование в {@link DcsFilterParser} и
 * {@link CanonicalConditionBuilder}, а также трансляция
 * {@link IllegalArgumentException} (невалидный XML) в кастомное
 * исключение {@link ConversionException}.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class ConditionConverterServiceTest {

        @Mock
        private DcsFilterParser parser;

        @Mock
        private CanonicalConditionBuilder builder;

        @InjectMocks
        private ConditionConverterService service;

        private static final String VALID_XML = "<Settings><filter/></Settings>";
        private static final String INVALID_XML = "<Settings><filter>";

        @Test
        void convertDelegatesToParserAndBuilder() {
                CanonicalConditionDto parsed = new CanonicalConditionDto(
                                "AND", List.of(new RuleDto("cityFias", "EQ", List.of("A"))));
                CanonicalConditionDto normalized = new CanonicalConditionDto(
                                "AND", List.of(new RuleDto("city_fias", "EQ", List.of("A"))));
                when(parser.parse(VALID_XML)).thenReturn(parsed);
                when(builder.build(parsed)).thenReturn(normalized);

                CanonicalConditionDto result = service.convert(VALID_XML);

                verify(parser).parse(VALID_XML);
                verify(builder).build(parsed);
                assertEquals(normalized, result, "Сервис возвращает результат нормализации билдера");
        }

        @Test
        void convertWrapsInvalidXmlIntoConversionException() {
                when(parser.parse(INVALID_XML))
                                .thenThrow(new IllegalArgumentException("XML not well-formed"));

                ConversionException exception = assertThrows(
                                ConversionException.class,
                                () -> service.convert(INVALID_XML),
                                "Невалидный XML должен транслироваться в ConversionException");

                assertInstanceOf(IllegalArgumentException.class, exception.getCause(),
                                "Причина ConversionException — исходный IllegalArgumentException парсера");
                verifyNoInteractions(builder);
        }
}