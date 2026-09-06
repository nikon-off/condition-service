package com.conditionservice.service;

import com.conditionservice.dto.request.CreateGroupDto;
import com.conditionservice.entity.Condition;
import com.conditionservice.entity.ConditionGroup;
import com.conditionservice.entity.GroupCondition;
import com.conditionservice.exception.ConditionNotFoundException;
import com.conditionservice.exception.GroupNotFoundException;
import com.conditionservice.mapper.ConditionMapper;
import com.conditionservice.repository.ConditionGroupRepository;
import com.conditionservice.repository.ConditionRepository;
import com.conditionservice.repository.GroupConditionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Сервис управления группами условий отбора (CRUD).
 *
 * <p>Инкапсулирует операции над группами и их составом. Контроллеры
 * обращаются только к этому сервису (конструкторная инъекция).</p>
 */
@Service
public class GroupService {

    private final ConditionGroupRepository groupRepository;
    private final ConditionRepository conditionRepository;
    private final GroupConditionRepository groupConditionRepository;

    public GroupService(ConditionGroupRepository groupRepository,
                        ConditionRepository conditionRepository,
                        GroupConditionRepository groupConditionRepository) {
        this.groupRepository = groupRepository;
        this.conditionRepository = conditionRepository;
        this.groupConditionRepository = groupConditionRepository;
    }

    /**
     * Создаёт новую группу условий отбора.
     *
     * @param dto входящий контракт создания
     * @return сохранённая сущность с персистентным id
     */
    @Transactional
    public ConditionGroup create(CreateGroupDto dto) {
        ConditionGroup group = ConditionMapper.toGroup(dto);
        return groupRepository.save(group);
    }

    /**
     * Полностью заменяет состав группы: удаляет старые связи «условие ↔ группа»
     * и создаёт новые по переданному набору бизнес-ключей условий.
     * <p>Операция идемпотентна: повторный вызов с тем же составом не меняет результат.</p>
     *
     * @param groupKey      бизнес-ключ группы
     * @param conditionKeys бизнес-ключи условий нового состава (пустой список очищает группу)
     * @throws GroupNotFoundException         если группа с указанным ключом не существует
     * @throws ConditionNotFoundException     если какое-либо из условий отсутствует
     */
    @Transactional
    public void replaceConditions(String groupKey, List<String> conditionKeys) {
        ConditionGroup group = groupRepository.findByKey(groupKey)
                .orElseThrow(() -> new GroupNotFoundException(groupKey));

        // Удаляем старые связи (ON DELETE CASCADE также предусмотрен на уровне БД)
        groupConditionRepository.deleteByGroup_Id(group.getId());

        // Дубликаты исключаем, чтобы не нарушить составной первичный ключ
        conditionKeys.stream().distinct().forEach(conditionKey -> {
            Condition condition = conditionRepository.findByConditionKey(conditionKey)
                    .orElseThrow(() -> new ConditionNotFoundException(conditionKey));
            groupConditionRepository.save(new GroupCondition(group, condition));
        });
    }
}