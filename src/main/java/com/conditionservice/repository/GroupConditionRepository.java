package com.conditionservice.repository;

import com.conditionservice.entity.GroupCondition;
import com.conditionservice.entity.GroupConditionId;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Репозиторий связующей сущности {@link GroupCondition} (таблица {@code group_conditions}).
 * <p>
 * Первичный ключ — составной {@link GroupConditionId} (group_id, condition_id).
 */
public interface GroupConditionRepository extends JpaRepository<GroupCondition, GroupConditionId> {

    /**
     * Удаляет все связи «условие ↔ группа» для указанной группы.
     * <p>Используется в идемпотентной операции полной замены состава группы
     * (PUT /v1/groups/{key}/conditions): сначала старые связи удаляются,
     * затем создаются новые.</p>
     *
     * @param groupId идентификатор группы условий
     */
    void deleteByGroup_Id(Long groupId);
}