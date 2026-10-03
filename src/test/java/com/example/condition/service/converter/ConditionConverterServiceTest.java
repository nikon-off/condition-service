package com.example.condition.service.converter;

import com.example.condition.service.converter.dto.CanonicalConditionDto;
import com.example.condition.service.converter.dto.RuleDto;
import com.example.condition.service.converter.exception.ConversionException;
import com.example.condition.service.converter.parser.DcsFilterParser;
import com.example.condition.service.converter.transformer.CanonicalConditionBuilder;
import com.conditionservice.repository.ConditionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Юнит-тесты сервиса {@link ConditionConverterService} (Mockito).
 *
 * <p>
 * Проверяется делегирование в {@link DcsFilterParser} и
 * {@link CanonicalConditionBuilder}, трансляция
 * {@link IllegalArgumentException}
 * (невалидный XML) в кастомное исключение {@link ConversionException}, а также
 * персистентная стадия: детерминированная генерация {@code condition_key}
 * (SHA-256 от канонического JSON) и вызов Upsert в {@link ConditionRepository}.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class ConditionConverterServiceTest {

        @Mock
        private DcsFilterParser parser;

        @Mock
        private CanonicalConditionBuilder builder;

        @Mock
        private ConditionRepository conditionRepository;

        @Spy
        private ObjectMapper objectMapper = new ObjectMapper();

        @InjectMocks
        private ConditionConverterService service;

        private static final String VALID_XML = "<Settings><filter/></Settings>";
        private static final String INVALID_XML = "<Settings><filter>";

        private static final CanonicalConditionDto SAMPLE_DTO = new CanonicalConditionDto(
                        "AND", List.of(
                                        new RuleDto("ПерестрахованиеРСА", "EQ", List.of("false")),
                                        new RuleDto("СтраховойПродукт", "EQ",
                                                        List.of("342ec861-3f65-11e6-9e61-7824af33beda"))));

        // ------------------------------------------------------------------
        // Существующие тесты: стадия конвертации
        // ------------------------------------------------------------------

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

        // ------------------------------------------------------------------
        // Доработка ТЗ №3: генерация ключа + Upsert
        // ------------------------------------------------------------------

        @Test
        void testGenerateKeyDeterministic() {
                // Два одинаковых канонических условия должны давать одинаковый ключ.
                when(parser.parse(VALID_XML)).thenReturn(SAMPLE_DTO);
                when(builder.build(SAMPLE_DTO)).thenReturn(SAMPLE_DTO);
                when(conditionRepository.upsert(ArgumentMatchers.anyString(),
                                ArgumentMatchers.anyString())).thenReturn(1L);

                service.saveOrConvert(VALID_XML);
                service.saveOrConvert(VALID_XML);

                ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
                verify(conditionRepository, org.mockito.Mockito.times(2))
                                .upsert(keyCaptor.capture(), ArgumentMatchers.anyString());
                List<String> keys = keyCaptor.getAllValues();
                assertEquals(keys.get(0), keys.get(1),
                                "Одинаковые DTO дают один и тот же condition_key");
        }

        @Test
        void testSaveOrConvertSuccess() {
                when(parser.parse(VALID_XML)).thenReturn(SAMPLE_DTO);
                when(builder.build(SAMPLE_DTO)).thenReturn(SAMPLE_DTO);
                when(conditionRepository.upsert(ArgumentMatchers.anyString(),
                                ArgumentMatchers.anyString())).thenReturn(42L);

                Long id = service.saveOrConvert(VALID_XML);

                assertNotNull(id, "Возвращается id из RETURNING");
                assertEquals(42L, id);

                // При успешной конвертации upsert вызывается с ключом и JSON-строкой.
                ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
                ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
                verify(conditionRepository).upsert(keyCaptor.capture(), jsonCaptor.capture());

                assertThat(keyCaptor.getValue())
                                .as("condition_key = SHA-256 в hex, 64 символа, нижний регистр")
                                .matches("[0-9a-f]{64}");
                assertThat(jsonCaptor.getValue())
                                .as("В БД уходит канонический JSON без пробелов и отступов")
                                .doesNotContain("\n").doesNotContain(" ");
        }

        @Test
        void differentPayloadsProduceDifferentKeys() {
                CanonicalConditionDto other = new CanonicalConditionDto(
                                "OR", List.of(new RuleDto("ДругойРеквизит", "EQ", List.of("x"))));
                when(parser.parse(VALID_XML)).thenReturn(SAMPLE_DTO);
                when(builder.build(SAMPLE_DTO)).thenReturn(SAMPLE_DTO, other);
                when(conditionRepository.upsert(ArgumentMatchers.anyString(),
                                ArgumentMatchers.anyString())).thenReturn(1L);

                service.saveOrConvert(VALID_XML);
                service.saveOrConvert(VALID_XML);

                ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
                verify(conditionRepository, org.mockito.Mockito.times(2))
                                .upsert(keyCaptor.capture(), ArgumentMatchers.anyString());
                assertNotEquals(keyCaptor.getAllValues().get(0), keyCaptor.getAllValues().get(1),
                                "Разные по содержимому условия дают разные ключи");
        }

        @Test
        void ruleOrderChangeProducesDifferentKeyPredictably() {
                // Тот же набор правил, но в другом порядке. Порядок НЕ нормализуется,
                // поэтому ключ предсказуемо отличается (документированное решение).
                CanonicalConditionDto reversed = new CanonicalConditionDto(
                                "AND", List.of(
                                                new RuleDto("СтраховойПродукт", "EQ",
                                                                List.of("342ec861-3f65-11e6-9e61-7824af33beda")),
                                                new RuleDto("ПерестрахованиеРСА", "EQ", List.of("false"))));
                when(parser.parse(VALID_XML)).thenReturn(SAMPLE_DTO);
                when(builder.build(SAMPLE_DTO)).thenReturn(SAMPLE_DTO, reversed);
                when(conditionRepository.upsert(ArgumentMatchers.anyString(),
                                ArgumentMatchers.anyString())).thenReturn(1L);

                service.saveOrConvert(VALID_XML);
                service.saveOrConvert(VALID_XML);

                ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
                verify(conditionRepository, org.mockito.Mockito.times(2))
                                .upsert(keyCaptor.capture(), ArgumentMatchers.anyString());
                assertNotEquals(keyCaptor.getAllValues().get(0), keyCaptor.getAllValues().get(1),
                                "Изменение порядка правил даёт другой (предсказуемый) ключ");
        }

        @Test
        void invalidXmlThrowsConversionExceptionAndDoesNotPersist() {
                when(parser.parse(INVALID_XML))
                                .thenThrow(new IllegalArgumentException("XML not well-formed"));

                ConversionException exception = assertThrows(
                                ConversionException.class,
                                () -> service.saveOrConvert(INVALID_XML),
                                "Невалидный XML должен транслироваться в ConversionException");

                assertInstanceOf(IllegalArgumentException.class, exception.getCause(),
                                "Причина ConversionException — исходная ошибка парсера");
                // При ошибке парсинга запись в БД не создаётся.
                verifyNoInteractions(conditionRepository);
        }
}