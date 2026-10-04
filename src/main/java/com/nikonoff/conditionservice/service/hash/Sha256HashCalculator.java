package com.nikonoff.conditionservice.service.hash;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

/**
 * Реализация {@link HashCalculatorService} на базе стандартного
 * {@link MessageDigest}
 * (алгоритм SHA-256) и Jackson для нормализации JSON.
 *
 * <p>
 * <b>Почему недостаточно одного {@code SORT_PROPERTIES_ALPHABETICALLY}:</b>
 * эта фича Jackson упорядочивает свойства только при сериализации POJO и
 * <b>не действует</b> на {@code java.util.Map}/{@link ObjectNode}. Произвольный
 * JSON-payload парсится в дерево {@link JsonNode}, поэтому сортировка ключей
 * (включая вложенные объекты) выполняется явно рекурсивным методом
 * {@link #normalize(JsonNode)}. Флаг включён дополнительно, как «страховка»
 * и по требованиям контракта.
 * </p>
 */
@Service
public class Sha256HashCalculator implements HashCalculatorService {

    private static final String SHA_256_ALGORITHM = "SHA-256";

    /**
     * Mapper для парсинга и канонической сериализации. Создаётся внутри класса,
     * а не внедряется через DI, чтобы настройки сортировки были неотъемлемой
     * частью семантики хеширования и не влияли на другие потребители
     * автосконфигурированного {@link ObjectMapper} Spring Boot.
     */
    private final ObjectMapper objectMapper = JsonMapper.builder()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .build();

    @Override
    public String calculateHash(String jsonPayload) {
        if (jsonPayload == null) {
            throw new IllegalArgumentException("jsonPayload не может быть null");
        }
        String normalizedJson = normalizeJson(jsonPayload);
        return sha256Hex(normalizedJson);
    }

    /**
     * Приводит JSON к канонической строковой форме: компактная сериализация
     * (без пробелов) с отсортированными по алфавиту ключами.
     *
     * @param jsonPayload исходная JSON-строка
     * @return канонический JSON
     * @throws IllegalArgumentException если вход не является корректным JSON
     */
    private String normalizeJson(String jsonPayload) {
        try {
            JsonNode tree = objectMapper.readTree(jsonPayload);
            return objectMapper.writeValueAsString(normalize(tree));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                    "Входная строка не является корректным JSON: " + e.getOriginalMessage(), e);
        }
    }

    /**
     * Рекурсивно строит каноническое дерево: ключи объектов сортируются
     * в алфавитном порядке, порядок элементов массивов сохраняется.
     *
     * @param node узел исходного дерева
     * @return нормализованный узел
     */
    private JsonNode normalize(JsonNode node) {
        if (node.isObject()) {
            ObjectNode sorted = objectMapper.createObjectNode();
            node.properties().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> sorted.set(entry.getKey(), normalize(entry.getValue())));
            return sorted;
        }
        if (node.isArray()) {
            ArrayNode normalized = objectMapper.createArrayNode();
            node.forEach(child -> normalized.add(normalize(child)));
            return normalized;
        }
        return node.deepCopy();
    }

    /**
     * Вычисляет SHA-256 хеш строки и возвращает его в hex-представлении.
     *
     * @param input строка (уже нормализованный JSON)
     * @return 64-символьная hex-строка в нижнем регистре
     */
    private String sha256Hex(String input) {
        byte[] digest;
        try {
            digest = MessageDigest.getInstance(SHA_256_ALGORITHM)
                    .digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 гарантированно присутствует в любой JVM (спецификация JCA),
            // поэтому данная ветка фактически недостижима.
            throw new IllegalStateException("Алгоритм SHA-256 недоступен", e);
        }
        return HexFormat.of().formatHex(digest);
    }
}