package com.conditionservice.repository;

import com.conditionservice.entity.ConditionGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Репозиторий сущности {@link ConditionGroup} (таблица {@code condition_groups}).
 */
public interface ConditionGroupRepository extends JpaRepository<ConditionGroup, Long> {

    /**
     * Поиск группы по бизнес-ключу.
     *
     * @param key бизнес-ключ группы
     * @return группа условий или {@link Optional#empty()}, если не найдена
     */
    Optional<ConditionGroup> findByKey(String key);

    /**
     * Подсчёт условий группы, чьи JSONB-payload полностью содержатся ({@code @>})
     * во входящем объекте {@code incomingPayload}.
     * <p>
     * Используется GIN-индекс на колонке {@code conditions.payload} (jsonb_path_ops).
     * <b>Важно:</b> containment ({@code @>}) выполняется только нативно — через JPA
     * Criteria это сделать нельзя. Параметр приводится к {@code jsonb} явным CAST,
     * чтобы оператор {@code @>} был корректно разрешён. Тип значений учитывается
     * (число 5000 ≠ строка "5000"), а лишние поля входящего объекта игнорируются.
     *
     * @param groupId          идентификатор группы условий
     * @param incomingPayload  JSON-объект (строка), проверяемый на containment
     * @return количество совпавших условий (0, если payload не объект или нет совпадений)
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM conditions c
            JOIN group_conditions gc ON c.id = gc.condition_id
            WHERE gc.group_id = :groupId
              AND CAST(:incomingPayload AS jsonb) @> c.payload
            """, nativeQuery = true)
    long countMatchingConditions(@Param("groupId") Long groupId,
                                 @Param("incomingPayload") String incomingPayload);

    /**
     * Общее количество условий, привязанных к группе.
     * <p>
     * Используется для определения «пустой группы» (0 условий → результат
     * {@code matched=false}) и для сравнения с количеством совпавших условий.
     *
     * @param groupId идентификатор группы условий
     * @return количество условий в группе (0, если группа пуста)
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM group_conditions gc
            WHERE gc.group_id = :groupId
            """, nativeQuery = true)
    long countConditionsByGroupId(@Param("groupId") Long groupId);
}