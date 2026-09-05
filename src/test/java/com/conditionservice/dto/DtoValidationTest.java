package com.conditionservice.dto;

import com.conditionservice.dto.request.CheckGroupRequest;
import com.conditionservice.dto.request.CreateConditionDto;
import com.conditionservice.dto.request.CreateGroupDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DtoValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Test
    void createConditionDtoShouldRejectBlankKeyAndNullPayload() {
        CreateConditionDto dto = new CreateConditionDto("   ", null);

        Set<ConstraintViolation<CreateConditionDto>> violations = validator.validate(dto);

        assertEquals(2, violations.size());
        assertTrue(violations.stream().anyMatch(v ->
                v.getPropertyPath().toString().equals("conditionKey")
                        && v.getMessage().equals("Ключ условия не может быть пустым")));
        assertTrue(violations.stream().anyMatch(v ->
                v.getPropertyPath().toString().equals("payload")
                        && v.getMessage().equals("Payload условия не может быть null")));
    }

    @Test
    void createConditionDtoShouldPassWithValidData() throws Exception {
        JsonNode payload = MAPPER.readTree("{\"field\": \"value\"}");
        CreateConditionDto dto = new CreateConditionDto("cond-1", payload);

        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void checkGroupRequestShouldRejectBlankKeyAndNullPayload() {
        CheckGroupRequest dto = new CheckGroupRequest("", null);

        Set<ConstraintViolation<CheckGroupRequest>> violations = validator.validate(dto);

        assertEquals(2, violations.size());
        assertTrue(violations.stream().anyMatch(v ->
                v.getPropertyPath().toString().equals("groupKey")
                        && v.getMessage().equals("Ключ группы не может быть пустым")));
        assertTrue(violations.stream().anyMatch(v ->
                v.getPropertyPath().toString().equals("payload")
                        && v.getMessage().equals("Payload для проверки не может быть null")));
    }

    @Test
    void createGroupDtoShouldAllowNullDescriptionButRejectBlankKey() {
        CreateGroupDto valid = new CreateGroupDto("group-1", null);
        assertTrue(validator.validate(valid).isEmpty());

        CreateGroupDto invalid = new CreateGroupDto(" ", "desc");
        Set<ConstraintViolation<CreateGroupDto>> violations = validator.validate(invalid);
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v ->
                v.getPropertyPath().toString().equals("groupKey")));
    }
}