package com.example.condition.service.api;

import com.example.condition.service.api.dto.ConvertRequest;
import com.example.condition.service.converter.ConditionConverterService;
import com.example.condition.service.converter.dto.SavedCondition;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-контроллер конвертации XML-фильтров 1С DCS в канонический JSON.
 *
 * <p>
 * Только оркестрирует вызов {@link ConditionConverterService} и маппит
 * HTTP-контракт — бизнес-логика (парсинг, нормализация, хеширование, Upsert)
 * инкапсулирована в сервисе.
 * </p>
 *
 * <p>
 * Endpoint: {@code POST /api/v1/conditions/convert}.
 * </p>
 *
 * <p>
 * <b>Явное имя бина:</b> {@code "conditionConvertController"} — в проекте уже
 * существует {@code com.conditionservice.controller.ConditionController}
 * (CRUD-эндпоинты {@code /v1/conditions}), чьё имя бина по умолчанию совпало бы
 * с этим классом и вызвало {@code ConflictingBeanDefinitionException}.
 * </p>
 */
@RestController("conditionConvertController")
@RequestMapping("/api/v1/conditions")
public class ConditionController {

    private final ConditionConverterService conditionConverterService;

    public ConditionController(ConditionConverterService conditionConverterService) {
        this.conditionConverterService = conditionConverterService;
    }

    /**
     * Конвертирует XML-фильтр 1С в каноническое условие и сохраняет его в БД
     * идемпотентно (Upsert по {@code condition_key}).
     *
     * @param request тело запроса с сырым XML
     * @return HTTP 201 Created + {@link SavedCondition} (id, conditionKey,
     *         каноническое условие); повторная отправка того же XML возвращает
     *         тот же id и обновляет {@code updated_at}
     */
    @PostMapping("/convert")
    public ResponseEntity<SavedCondition> convert(@Valid @RequestBody ConvertRequest request) {
        SavedCondition saved = conditionConverterService.saveOrConvertWithResult(request.xmlFilter());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}