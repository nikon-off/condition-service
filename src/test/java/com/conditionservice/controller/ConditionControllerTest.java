package com.conditionservice.controller;

import com.conditionservice.entity.Condition;
import com.conditionservice.exception.ConditionNotFoundException;
import com.conditionservice.service.ConditionService;
import com.fasterxml.jackson.databind.JsonNode;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConditionController.class)
class ConditionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConditionService conditionService;

    @Test
    void createShouldReturn201WithCreatedCondition() throws Exception {
        Condition saved = new Condition("cond-1",
                objectMapper.readTree("{\"field\": \"value\"}"));

        when(conditionService.create(any())).thenReturn(saved);

        mockMvc.perform(post("/v1/conditions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conditionKey": "cond-1", "payload": {"field": "value"}}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.conditionKey").value("cond-1"))
                .andExpect(jsonPath("$.payload.field").value("value"));
    }

    @Test
    void createShouldReturn400OnInvalidBody() throws Exception {
        mockMvc.perform(post("/v1/conditions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conditionKey": "", "payload": null}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getByKeyShouldReturn200WhenFound() throws Exception {
        Condition found = new Condition("cond-1",
                objectMapper.readTree("{\"field\": \"value\"}"));

        when(conditionService.getByKey(eq("cond-1"))).thenReturn(found);

        mockMvc.perform(get("/v1/conditions/cond-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conditionKey").value("cond-1"));
    }

    @Test
    void getByKeyShouldReturn404WhenNotFound() throws Exception {
        when(conditionService.getByKey(eq("missing")))
                .thenThrow(new ConditionNotFoundException("missing"));

        mockMvc.perform(get("/v1/conditions/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }
}