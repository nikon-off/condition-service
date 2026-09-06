package com.conditionservice.repository;

import com.conditionservice.entity.Condition;
import org.springframework.data.jpa.repository.JpaRepository;

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
     * <p>Используется для проверки существования условий при полной замене
     * состава группы (идемпотентная операция PUT).</p>
     *
     * @param keys набор бизнес-ключей условий
     * @return найденные условия
     */
    List<Condition> findByConditionKeyIn(Collection<String> keys);
}