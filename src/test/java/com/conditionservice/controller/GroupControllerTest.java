package com.conditionservice.controller;

import com.conditionservice.entity.ConditionGroup;
import com.conditionservice.exception.GroupNotFoundException;
import com.conditionservice.service.GroupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GroupController.class)
class GroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GroupService groupService;

    @Test
    void createShouldReturn201WithCreatedGroup() throws Exception {
        ConditionGroup saved = new ConditionGroup("group-1", "group-1", null);

        when(groupService.create(any())).thenReturn(saved);

        mockMvc.perform(post("/v1/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupKey": "group-1"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value("group-1"));
    }

    @Test
    void createShouldReturn400OnInvalidBody() throws Exception {
        mockMvc.perform(post("/v1/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupKey": " "}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void replaceConditionsShouldReturn200WhenSuccessful() throws Exception {
        mockMvc.perform(put("/v1/groups/group-1/conditions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conditionKeys": ["cond-1", "cond-2"]}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void replaceConditionsShouldReturn400WhenListIsNull() throws Exception {
        mockMvc.perform(put("/v1/groups/group-1/conditions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conditionKeys": null}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void replaceConditionsShouldReturn404WhenGroupNotFound() throws Exception {
        doThrow(new GroupNotFoundException("missing"))
                .when(groupService).replaceConditions(eq("missing"), any());

        mockMvc.perform(put("/v1/groups/missing/conditions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conditionKeys": ["cond-1"]}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }
}