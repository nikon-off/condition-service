package com.nikonoff.conditionservice.service.hash;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit-тесты логики нормализации и хеширования {@link Sha256HashCalculator}.
 */
class HashCalculatorServiceTest {

    private final HashCalculatorService hashCalculator = new Sha256HashCalculator();

    @Test
    @DisplayName("Хеш пустого объекта {} стабилен между вызовами")
    void hashOfEmptyObjectIsStable() {
        String first = hashCalculator.calculateHash("{}");
        String second = hashCalculator.calculateHash("{}");

        assertThat(first).isEqualTo(second);
    }

    @Test
    @DisplayName("Логически эквивалентные JSON с разным порядком ключей дают одинаковый хеш")
    void hashIgnoresKeyOrder() {
        String ab = hashCalculator.calculateHash("{\"a\":1,\"b\":2}");
        String ba = hashCalculator.calculateHash("{\"b\":2,\"a\":1}");

        assertThat(ab).isEqualTo(ba);
    }

    @Test
    @DisplayName("Разные значения дают разные хеши")
    void hashDiffersForDifferentValues() {
        String ten = hashCalculator.calculateHash("{\"val\":10}");
        String twenty = hashCalculator.calculateHash("{\"val\":20}");

        assertThat(ten).isNotEqualTo(twenty);
    }

    @Test
    @DisplayName("Ключи вложенных объектов также сортируются")
    void hashNormalizesNestedObjects() {
        String left = hashCalculator.calculateHash("{\"x\":{\"b\":1,\"a\":2},\"z\":true}");
        String right = hashCalculator.calculateHash("{\"z\":true,\"x\":{\"a\":2,\"b\":1}}");

        assertThat(left).isEqualTo(right);
    }

    @Test
    @DisplayName("Хеш — 64-символьная hex-строка в нижнем регистре")
    void hashIsLowercaseHexOfLength64() {
        String hash = hashCalculator.calculateHash("{\"val\":10}");

        assertThat(hash)
                .hasSize(64)
                .matches("[0-9a-f]{64}");
    }

    @Test
    @DisplayName("Некорректный JSON приводит к IllegalArgumentException")
    void invalidJsonThrows() {
        assertThatThrownBy(() -> hashCalculator.calculateHash("{invalid}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JSON");
    }

    @Test
    @DisplayName("null приводит к IllegalArgumentException")
    void nullPayloadThrows() {
        assertThatThrownBy(() -> hashCalculator.calculateHash(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}