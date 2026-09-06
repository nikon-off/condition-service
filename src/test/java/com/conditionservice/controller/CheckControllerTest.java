package com.conditionservice.controller;

import com.conditionservice.dto.response.CheckResultDto;
import com.conditionservice.exception.GroupNotFoundException;
import com.conditionservice.service.ConditionCheckService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CheckController.class)
class CheckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConditionCheckService conditionCheckService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void checkGroupShouldReturn200WithResult() throws Exception {
        when(conditionCheckService.checkGroup(eq("group-1"), any()))
                .thenReturn(new CheckResultDto(true, 1, 1));

        mockMvc.perform(post("/v1/check-group")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupKey": "group-1", "payload": {"field": "value"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matched").value(true))
                .andExpect(jsonPath("$.totalConditions").value(1))
                .andExpect(jsonPath("$.matchedCount").value(1));
    }

    @Test
    void checkGroupShouldReturn400OnInvalidBody() throws Exception {
        mockMvc.perform(post("/v1/check-group")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupKey": "", "payload": null}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkGroupShouldReturn404WhenGroupNotFound() throws Exception {
        when(conditionCheckService.checkGroup(eq("missing"), any()))
                .thenThrow(new GroupNotFoundException("missing"));

        mockMvc.perform(post("/v1/check-group")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupKey": "missing", "payload": {"field": "value"}}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void checkBatchShouldReturn200WithPerGroupResults() throws Exception {
        when(conditionCheckService.checkGroup(eq("group-1"), any()))
                .thenReturn(new CheckResultDto(true, 1, 1));
        when(conditionCheckService.checkGroup(eq("group-2"), any()))
                .thenReturn(new CheckResultDto(false, 2, 0));

        mockMvc.perform(post("/v1/check-groups-batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groups": [
                                    {"groupKey": "group-1", "payload": {"field": "value"}},
                                    {"groupKey": "group-2", "payload": {"field": "other"}}
                                ]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].groupKey").value("group-1"))
                .andExpect(jsonPath("$[0].result.matched").value(true))
                .andExpect(jsonPath("$[1].groupKey").value("group-2"))
                .andExpect(jsonPath("$[1].result.matched").value(false));
    }
}