package com.conditionservice.repository;

import com.conditionservice.entity.Condition;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Репозиторий сущности {@link Condition} (таблица {@code conditions}).
 */
public interface ConditionRepository extends JpaRepository<Condition, Long> {
}