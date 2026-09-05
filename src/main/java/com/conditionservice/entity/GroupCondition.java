package com.conditionservice.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * Связующая сущность M2M «условие ↔ группа» (таблица {@code group_conditions}).
 * <p>
 * Используется {@link EmbeddedId} (составной ключ из group_id и condition_id)
 * и {@link MapsId} для синхронизации значений ключа с внешними ключами.
 * Каскадное удаление (ON DELETE CASCADE) задано на уровне БД.
 */
@Entity
@Table(name = "group_conditions")
public class GroupCondition {

    @EmbeddedId
    private GroupConditionId id;

    @MapsId("groupId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private ConditionGroup group;

    @MapsId("conditionId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condition_id", nullable = false)
    private Condition condition;

    protected GroupCondition() {
        // Для JPA/Hibernate
    }

    public GroupCondition(ConditionGroup group, Condition condition) {
        this.group = group;
        this.condition = condition;
        this.id = new GroupConditionId(group.getId(), condition.getId());
    }

    public GroupConditionId getId() {
        return id;
    }

    public void setId(GroupConditionId id) {
        this.id = id;
    }

    public ConditionGroup getGroup() {
        return group;
    }

    public void setGroup(ConditionGroup group) {
        this.group = group;
    }

    public Condition getCondition() {
        return condition;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GroupCondition that)) {
            return false;
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "GroupCondition{" +
                "id=" + id +
                '}';
    }
}