package com.conditionservice.repository;

import com.conditionservice.entity.Condition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий сущности {@link Condition} (таблица {@code conditions}).
 */
public interface ConditionRepository extends JpaRepository<Condition, Long> {

    /**
     * Поиск условия по бизнес-ключу.
     *
     * @param conditionKey бизнес-ключ условия
     * @return условие или {@link Optional#empty()}, если не найдено
     */
    Optional<Condition> findByConditionKey(String conditionKey);

    /**
     * Поиск условий по набору бизнес-ключей.
     * <p>
     * Используется для проверки существования условий при полной замене
     * состава группы (идемпотентная операция PUT).
     * </p>
     *
     * @param keys набор бизнес-ключей условий
     * @return найденные условия
     */
    List<Condition> findByConditionKeyIn(Collection<String> keys);

    /**
     * Идемпотентная вставка/обновление условия по уникальному бизнес-ключу.
     *
     * <p>
     * Нативный SQL с {@code ON CONFLICT (condition_key) DO UPDATE}: при первом
     * вызове создаётся запись ({@code created_at = NOW()}), при повторном —
     * обновляется только {@code payload} и {@code updated_at}. {@code RETURNING id}
     * возвращает персистентный id строки в обоих сценариях.
     * </p>
     *
     * <p>
     * {@code payload} передаётся строкой и приводится к {@code jsonb} на стороне
     * БД ({@code ::jsonb}) — это гарантирует, что CHECK-ограничение
     * {@code jsonb_typeof(payload) = 'object'} проверяется PostgreSQL, а не JPA.
     * </p>
     *
     * @param conditionKey бизнес-ключ условия (уникален,
     *                     {@code uk_conditions_condition_key})
     * @param payloadJson  JSON-строка канонического условия (объект)
     * @return id строки в таблице {@code conditions}
     */
    @Query(value = """
            INSERT INTO conditions (condition_key, payload, created_at, updated_at)
            VALUES (:conditionKey, CAST(:payloadJson AS jsonb), NOW(), NOW())
            ON CONFLICT (condition_key)
            DO UPDATE SET payload = EXCLUDED.payload, updated_at = NOW()
            RETURNING id
            """, nativeQuery = true)
    Long upsert(@Param("conditionKey") String conditionKey,
            @Param("payloadJson") String payloadJson);
}