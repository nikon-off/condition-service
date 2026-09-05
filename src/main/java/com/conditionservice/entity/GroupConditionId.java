package com.conditionservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/**
 * Составной первичный ключ связующей таблицы {@code group_conditions}.
 * <p>
 * Ключ — пара {@code (group_id, condition_id)}, соответствует
 * констрейнту {@code pk_group_conditions}.
 */
@Embeddable
public class GroupConditionId implements Serializable {

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "condition_id", nullable = false)
    private Long conditionId;

    protected GroupConditionId() {
        // Для JPA/Hibernate
    }

    public GroupConditionId(Long groupId, Long conditionId) {
        this.groupId = groupId;
        this.conditionId = conditionId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getConditionId() {
        return conditionId;
    }

    public void setConditionId(Long conditionId) {
        this.conditionId = conditionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GroupConditionId that)) {
            return false;
        }
        return Objects.equals(groupId, that.groupId)
                && Objects.equals(conditionId, that.conditionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, conditionId);
    }

    @Override
    public String toString() {
        return "GroupConditionId{" +
                "groupId=" + groupId +
                ", conditionId=" + conditionId +
                '}';
    }
}