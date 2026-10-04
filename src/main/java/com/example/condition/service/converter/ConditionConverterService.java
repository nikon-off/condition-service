package com.example.condition.service.converter;

import com.example.condition.service.converter.dto.CanonicalConditionDto;
import com.example.condition.service.converter.dto.SavedCondition;
import com.example.condition.service.converter.exception.ConversionException;
import com.example.condition.service.converter.parser.DcsFilterParser;
import com.example.condition.service.converter.transformer.CanonicalConditionBuilder;
import com.conditionservice.repository.ConditionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Сервис процесса конвертации XML-настроек 1С DCS в каноническое условие
 * и его сохранения в БД (Upsert).
 *
 * <p>
 * Оркестрирует три стадии:
 * <ol>
 * <li>{@link DcsFilterParser#parse(String)} — разбор XML в промежуточный
 * {@link CanonicalConditionDto};</li>
 * <li>{@link CanonicalConditionBuilder#build(CanonicalConditionDto)} —
 * нормализация в канонический формат (поля, операторы, значения);</li>
 * <li>{@link #saveOrConvertWithResult(String)} — генерация детерминированного
 * {@code condition_key} (SHA-256 от канонического JSON) и Upsert в таблицу
 * {@code conditions} по уникальному бизнес-ключу.</li>
 * </ol>
 * </p>
 *
 * <p>
 * Ошибки формата входного XML ({@link IllegalArgumentException} парсера)
 * транслируются в {@link ConversionException}, отделяя их от ошибок
 * логики нормализации. При ошибке парсинга запись в БД не создаётся.
 * </p>
 */
@Service
public class ConditionConverterService {

    private final DcsFilterParser parser;
    private final CanonicalConditionBuilder builder;
    private final ConditionRepository conditionRepository;

    /**
     * ObjectMapper для канонической сериализации: без пробелов и отступов
     * ({@code INDENT_OUTPUT=false}), ключи объекта сортируются
     * ({@link SerializationFeature#ORDER_MAP_ENTRIES_BY_KEYS}), чтобы одинаковые
     * по смыслу условия давали один и тот же {@code condition_key}.
     * Потокобезопасен после конфигурации (копия создаётся один раз).
     */
    private final ObjectMapper canonicalJsonMapper;

    /**
     * Конструктор с внедрением зависимостей.
     *
     * @param parser              парсер XML-настроек 1С DCS
     * @param builder             нормализатор результата парсинга в канонический
     *                            формат
     * @param conditionRepository репозиторий условий (Upsert по business-ключу)
     * @param objectMapper        базовый Jackson ObjectMapper (Spring bean);
     *                            для хеширования используется его копия с
     *                            сортировкой ключей
     */
    public ConditionConverterService(DcsFilterParser parser,
            CanonicalConditionBuilder builder,
            ConditionRepository conditionRepository,
            ObjectMapper objectMapper) {
        this.parser = parser;
        this.builder = builder;
        this.conditionRepository = conditionRepository;
        ObjectMapper base = objectMapper != null ? objectMapper : new ObjectMapper();
        this.canonicalJsonMapper = base.copy()
                .disable(SerializationFeature.INDENT_OUTPUT)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
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

    /**
     * Конвертирует XML-фильтр в каноническое условие, сохраняет его в БД
     * идемпотентно (Upsert по {@code condition_key}) и возвращает полный
     * результат операции.
     *
     * <p>
     * Последовательность операций:
     * </p>
     * <ol>
     * <li>{@link #convert(String)} — парсинг + нормализация;</li>
     * <li>генерация {@code conditionKey} — SHA-256 hex (нижний регистр) от
     * канонического JSON условия (без пробелов, ключи отсортированы);</li>
     * <li>сериализация DTO в JSON-строку (объект — удовлетворяет
     * CHECK {@code jsonb_typeof(payload) = 'object'});</li>
     * <li>вызов {@link ConditionRepository#upsert(String, String)};</li>
     * <li>сборка {@link SavedCondition}: id из {@code RETURNING id}, бизнес-ключ
     * и каноническое условие (единый источник истины для контроллера).</li>
     * </ol>
     *
     * @param xmlFilter сырой XML настроек DCS (поле {@code xmlFilter})
     * @return сохранённое условие: персистентный id, бизнес-ключ и
     *         каноническое представление
     * @throws ConversionException если входной XML невалиден или каноническое
     *                             условие не удалось сериализовать
     */
    @Transactional
    public SavedCondition saveOrConvertWithResult(String xmlFilter) {
        CanonicalConditionDto dto = convert(xmlFilter);
        String conditionKey = generateConditionKey(dto);
        String payloadJson = toCanonicalJson(dto);
        Long id = conditionRepository.upsert(conditionKey, payloadJson);
        return new SavedCondition(id, conditionKey, dto);
    }

    /**
     * Обёртка над {@link #saveOrConvertWithResult(String)}, возвращающая только
     * персистентный id.
     *
     * <p>
     * Оставлен для обратной совместимости (используется существующими
     * интеграционными тестами). Новый код предпочитает
     * {@link #saveOrConvertWithResult(String)} — он возвращает id, ключ и DTO
     * за одну операцию без дублирования логики.
     * </p>
     *
     * @param xmlFilter сырой XML настроек DCS (поле {@code xmlFilter})
     * @return персистентный id записи в таблице {@code conditions}
     * @throws ConversionException если входной XML невалиден или каноническое
     *                             условие не удалось сериализовать
     */
    @Transactional
    public Long saveOrConvert(String xmlFilter) {
        return saveOrConvertWithResult(xmlFilter).id();
    }

    /**
     * Генерирует детерминированный бизнес-ключ условия.
     *
     * <p>
     * Ключ = SHA-256 (hex, нижний регистр) от канонического JSON
     * представления DTO. Сортировка ключей JSON и фиксированный порядок полей
     * DTO гарантируют, что одинаковые по смыслу (нормализованные) условия
     * дают один и тот же ключ. Порядок правил при этом НЕ нормализуется:
     * изменение порядка правил предсказуемо меняет ключ.
     * </p>
     *
     * @param dto каноническое условие
     * @return hex-строка SHA-256 (64 символа, нижний регистр)
     */
    private String generateConditionKey(CanonicalConditionDto dto) {
        String canonicalJson = toCanonicalJson(dto);
        return sha256Hex(canonicalJson);
    }

    /**
     * Сериализует каноническое условие в JSON-строку без пробелов с
     * отсортированными ключами объектов.
     *
     * @param dto каноническое условие
     * @return каноническая JSON-строка
     * @throws ConversionException если сериализация не удалась (не должно
     *                             происходить для примитивного DTO)
     */
    private String toCanonicalJson(CanonicalConditionDto dto) {
        try {
            return canonicalJsonMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new ConversionException(
                    "Не удалось сериализовать каноническое условие: " + e.getMessage(), e);
        }
    }

    /**
     * Вычисляет SHA-256 хеш строки в UTF-8 и возвращает hex-представление
     * в нижнем регистре.
     *
     * @param value исходная строка (канонический JSON)
     * @return hex-строка SHA-256
     */
    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 гарантированно присутствует в любой JVM (JCA).
            throw new IllegalStateException("SHA-256 недоступен в JVM", e);
        }
    }
}