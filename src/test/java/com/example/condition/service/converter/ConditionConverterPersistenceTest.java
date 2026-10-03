package com.example.condition.service.converter;

import com.example.condition.service.converter.dto.CanonicalConditionDto;
import com.example.condition.service.converter.dto.RuleDto;
import com.example.condition.service.converter.dto.SavedCondition;
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
 * Юнит-тесты персистентной стадии {@link ConditionConverterService}:
 * генерация детерминированного {@code condition_key} (SHA-256 от канонического
 * JSON) и поведение {@code saveOrConvert}.
 *
 * <p>
 * База данных не участвует — {@link ConditionRepository} замокан.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class ConditionConverterPersistenceTest {

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

        @Test
        void identicalDtosProduceIdenticalConditionKey() {
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
                assertEquals(2, keys.size(), "upsert вызывается один раз на каждый saveOrConvert");
                assertEquals(keys.get(0), keys.get(1),
                                "Идентичные канонические условия дают один и тот же condition_key");
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
        void conditionKeyIsSha256HexLowercase() {
                when(parser.parse(VALID_XML)).thenReturn(SAMPLE_DTO);
                when(builder.build(SAMPLE_DTO)).thenReturn(SAMPLE_DTO);
                when(conditionRepository.upsert(ArgumentMatchers.anyString(),
                                ArgumentMatchers.anyString())).thenReturn(1L);

                service.saveOrConvert(VALID_XML);

                ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
                verify(conditionRepository).upsert(keyCaptor.capture(), ArgumentMatchers.anyString());
                assertThat(keyCaptor.getValue())
                                .as("condition_key = SHA-256 в hex, 64 символа, нижний регистр")
                                .matches("[0-9a-f]{64}");
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
        void saveOrConvertReturnsSavedConditionWithIdKeyAndPayload() {
                when(parser.parse(VALID_XML)).thenReturn(SAMPLE_DTO);
                when(builder.build(SAMPLE_DTO)).thenReturn(SAMPLE_DTO);
                when(conditionRepository.upsert(ArgumentMatchers.anyString(),
                                ArgumentMatchers.anyString())).thenReturn(42L);

                SavedCondition saved = service.saveOrConvert(VALID_XML);

                assertNotNull(saved.id(), "Возвращается id из RETURNING");
                assertEquals(42L, saved.id());
                assertEquals(SAMPLE_DTO, saved.payload(), "Возвращается исходное каноническое условие");
                assertNotNull(saved.conditionKey(), "Возвращается сгенерированный condition_key");
                assertThat(saved.conditionKey()).matches("[0-9a-f]{64}");
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