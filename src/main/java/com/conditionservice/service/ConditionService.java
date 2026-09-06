package com.conditionservice.service;

import com.conditionservice.dto.request.CreateConditionDto;
import com.conditionservice.entity.Condition;
import com.conditionservice.exception.ConditionNotFoundException;
import com.conditionservice.mapper.ConditionMapper;
import com.conditionservice.repository.ConditionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис управления условиями отбора (CRUD).
 *
 * <p>Инкапсулирует работу с репозиторием {@link ConditionRepository}:
 * контроллеры не обращаются к JPA напрямую, а вызывают только этот сервис
 * (конструкторная инъекция).</p>
 */
@Service
public class ConditionService {

    private final ConditionRepository conditionRepository;

    public ConditionService(ConditionRepository conditionRepository) {
        this.conditionRepository = conditionRepository;
    }

    /**
     * Создаёт новое условие отбора.
     *
     * @param dto входящий контракт создания
     * @return сохранённая сущность с персистентным id
     */
    @Transactional
    public Condition create(CreateConditionDto dto) {
        Condition condition = ConditionMapper.toCondition(dto);
        return conditionRepository.save(condition);
    }

    /**
     * Возвращает условие по бизнес-ключу.
     *
     * @param conditionKey бизнес-ключ условия
     * @return найденное условие
     * @throws ConditionNotFoundException если условие с указанным ключом не существует
     */
    @Transactional(readOnly = true)
    public Condition getByKey(String conditionKey) {
        return conditionRepository.findByConditionKey(conditionKey)
                .orElseThrow(() -> new ConditionNotFoundException(conditionKey));
    }
}