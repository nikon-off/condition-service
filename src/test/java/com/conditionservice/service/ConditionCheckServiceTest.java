package com.conditionservice.service;

import com.conditionservice.dto.response.CheckResultDto;
import com.conditionservice.entity.ConditionGroup;
import com.conditionservice.exception.GroupNotFoundException;
import com.conditionservice.repository.ConditionGroupRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Юнит-тесты логики {@link ConditionCheckService}.
 * <p>
 * Containment ({@code @>}) выполняется в БД, поэтому здесь репозиторий замокан:
 * проверяется оркестрация — поиск группы, обработка «пустой группы»,
 * правило «все условия должны совпасть» и ошибка 404.
 */
@ExtendWith(MockitoExtension.class)
class ConditionCheckServiceTest {

    @Mock
    private ConditionGroupRepository groupRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ConditionCheckService service;

    @BeforeEach
    void setUp() {
        // Сервис собираем вручную: ObjectMapper не является моком и не инжектится через @InjectMocks
        service = new ConditionCheckService(groupRepository, objectMapper);
    }

    @Test
    void checkGroup_whenGroupNotFound_throws() {
        when(groupRepository.findByKey("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.checkGroup("missing", node("{}")))
                .isInstanceOf(GroupNotFoundException.class);

        verify(groupRepository, never()).countConditionsByGroupId(anyLong());
        verify(groupRepository, never()).countMatchingConditions(anyLong(), any());
    }

    @Test
    void checkGroup_whenEmptyGroup_returnsMatchedFalse() {
        when(groupRepository.findByKey("empty")).thenReturn(Optional.of(group(1L)));
        when(groupRepository.countConditionsByGroupId(1L)).thenReturn(0L);

        CheckResultDto result = service.checkGroup("empty", node("{\"a\":1}"));

        assertThat(result.matched()).isFalse();
        assertThat(result.totalConditions()).isZero();
        assertThat(result.matchedCount()).isZero();

        verify(groupRepository, never()).countMatchingConditions(anyLong(), any());
    }

    @Test
    void checkGroup_whenAllConditionsMatch_returnsMatchedTrue() {
        when(groupRepository.findByKey("g")).thenReturn(Optional.of(group(7L)));
        when(groupRepository.countConditionsByGroupId(7L)).thenReturn(3L);
        when(groupRepository.countMatchingConditions(eq(7L), any())).thenReturn(3L);

        CheckResultDto result = service.checkGroup("g", node("{\"a\":1,\"b\":2}"));

        assertThat(result.matched()).isTrue();
        assertThat(result.totalConditions()).isEqualTo(3);
        assertThat(result.matchedCount()).isEqualTo(3);
    }

    @Test
    void checkGroup_whenPartialMatch_returnsMatchedFalse() {
        when(groupRepository.findByKey("g")).thenReturn(Optional.of(group(7L)));
        when(groupRepository.countConditionsByGroupId(7L)).thenReturn(3L);
        when(groupRepository.countMatchingConditions(eq(7L), any())).thenReturn(2L);

        CheckResultDto result = service.checkGroup("g", node("{\"a\":1}"));

        assertThat(result.matched()).isFalse();
        assertThat(result.totalConditions()).isEqualTo(3);
        assertThat(result.matchedCount()).isEqualTo(2);
    }

    // --- helpers ----------------------------------------------------------

    private ConditionGroup group(Long id) {
        ConditionGroup g = new ConditionGroup("key-" + id, "name-" + id, null);
        g.setId(id);
        return g;
    }

    private JsonNode node(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}