package com.conditionservice.controller;

import com.conditionservice.dto.request.CheckGroupRequest;
import com.conditionservice.dto.request.CheckGroupsBatchRequest;
import com.conditionservice.dto.response.BatchCheckResultDto;
import com.conditionservice.dto.response.CheckResultDto;
import com.conditionservice.service.ConditionCheckService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST-контроллер проверки условий отбора.
 *
 * <p>Проверка одной группы делегируется в {@link ConditionCheckService}.
 * Пакетная проверка пока выполняется простым последовательным циклом
 * (оптимизация — в бэклоге).</p>
 */
@RestController
@RequestMapping("/v1")
public class CheckController {

    private final ConditionCheckService conditionCheckService;

    public CheckController(ConditionCheckService conditionCheckService) {
        this.conditionCheckService = conditionCheckService;
    }

    /**
     * POST /v1/check-group — проверка одной группы условий.
     *
     * @param request ключ группы и входящие поля
     * @return HTTP 200 OK + результат проверки (404, если группа не найдена)
     */
    @PostMapping("/check-group")
    public ResponseEntity<CheckResultDto> checkGroup(@Valid @RequestBody CheckGroupRequest request) {
        CheckResultDto result = conditionCheckService.checkGroup(
                request.getGroupKey(), request.getPayload());
        return ResponseEntity.ok(result);
    }

    /**
     * POST /v1/check-groups-batch — пакетная проверка групп.
     * <p>На текущем этапе — последовательный цикл по списку; результат — список
     * пар «ключ группы → результат проверки».</p>
     *
     * @param request список проверяемых групп
     * @return HTTP 200 OK + результаты по каждой группе (404, если какая-то группа не найдена)
     */
    @PostMapping("/check-groups-batch")
    public ResponseEntity<List<BatchCheckResultDto>> checkGroupsBatch(
            @Valid @RequestBody CheckGroupsBatchRequest request) {

        List<BatchCheckResultDto> results = request.getGroups().stream()
                .map(group -> new BatchCheckResultDto(
                        group.getGroupKey(),
                        conditionCheckService.checkGroup(group.getGroupKey(), group.getPayload())))
                .toList();
        return ResponseEntity.ok(results);
    }
}