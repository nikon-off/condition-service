package com.conditionservice.controller;

import com.conditionservice.dto.request.CreateConditionDto;
import com.conditionservice.dto.response.ConditionResponseDto;
import com.conditionservice.entity.Condition;
import com.conditionservice.mapper.ConditionMapper;
import com.conditionservice.service.ConditionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-контроллер управления условиями отбора.
 *
 * <p>Только оркестрирует вызовы {@link ConditionService} и маппит ответные DTO —
 * бизнес-логика в контроллере не дублируется.</p>
 */
@RestController
@RequestMapping("/v1/conditions")
public class ConditionController {

    private final ConditionService conditionService;

    public ConditionController(ConditionService conditionService) {
        this.conditionService = conditionService;
    }

    /**
     * POST /v1/conditions — создание условия отбора.
     *
     * @param dto входящий контракт
     * @return HTTP 201 Created + созданное условие
     */
    @PostMapping
    public ResponseEntity<ConditionResponseDto> create(@Valid @RequestBody CreateConditionDto dto) {
        Condition condition = conditionService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ConditionMapper.toResponse(condition));
    }

    /**
     * GET /v1/conditions/{key} — получение условия по бизнес-ключу.
     *
     * @param key бизнес-ключ условия
     * @return HTTP 200 OK + условие (404, если не найдено)
     */
    @GetMapping("/{key}")
    public ResponseEntity<ConditionResponseDto> getByKey(@PathVariable("key") String key) {
        Condition condition = conditionService.getByKey(key);
        return ResponseEntity.ok(ConditionMapper.toResponse(condition));
    }
}