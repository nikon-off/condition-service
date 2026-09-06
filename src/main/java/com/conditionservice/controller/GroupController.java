package com.conditionservice.controller;

import com.conditionservice.dto.request.CreateGroupDto;
import com.conditionservice.dto.request.UpdateGroupConditionsDto;
import com.conditionservice.dto.response.GroupResponseDto;
import com.conditionservice.entity.ConditionGroup;
import com.conditionservice.mapper.ConditionMapper;
import com.conditionservice.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-контроллер управления группами условий отбора.
 *
 * <p>Только оркестрирует вызовы {@link GroupService} — бизнес-логика
 * в контроллере не дублируется.</p>
 */
@RestController
@RequestMapping("/v1/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    /**
     * POST /v1/groups — создание группы условий отбора.
     *
     * @param dto входящий контракт
     * @return HTTP 201 Created + созданная группа
     */
    @PostMapping
    public ResponseEntity<GroupResponseDto> create(@Valid @RequestBody CreateGroupDto dto) {
        ConditionGroup group = groupService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ConditionMapper.toResponse(group));
    }

    /**
     * PUT /v1/groups/{key}/conditions — полная (идемпотентная) замена состава группы.
     *
     * @param key бизнес-ключ группы
     * @param dto новый состав (список бизнес-ключей условий)
     * @return HTTP 200 OK с пустым телом (404, если группа или условие не найдены)
     */
    @PutMapping("/{key}/conditions")
    public ResponseEntity<Void> replaceConditions(@PathVariable("key") String key,
                                                  @Valid @RequestBody UpdateGroupConditionsDto dto) {
        groupService.replaceConditions(key, dto.getConditionKeys());
        return ResponseEntity.ok().build();
    }
}