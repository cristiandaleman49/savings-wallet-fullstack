package com.example.savingswallet.infrastructure.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SavingsGoalRestIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String CREATE_BODY = """
            {"name":"Vacaciones","targetAmount":1000.00,"currency":"USD"}
            """;

    private Long createGoal(String body, String userId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/savings-goals")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("id").asLong();
    }

    @Test
    void postCreatesGoalReturns201WithGeneratedIdAndLocation() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/savings-goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.name").value("Vacaciones"))
                .andExpect(jsonPath("$.targetAmount").value(1000.00))
                .andExpect(jsonPath("$.accumulatedAmount").value(0.00))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("id").asLong()).isPositive();
        assertThat(result.getResponse().getHeader("Location"))
                .endsWith("/api/v1/savings-goals/" + json.get("id").asLong());
    }

    @Test
    void getReturnsOnlyGoalsOfTheCallingUser() throws Exception {
        Long userGoalId = createGoal(CREATE_BODY, "1");

        mockMvc.perform(get("/api/v1/savings-goals").header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(userGoalId.intValue()))
                .andExpect(jsonPath("$[0].userId").value(1));

        mockMvc.perform(get("/api/v1/savings-goals").header("X-User-Id", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void contributionReturnsUpdatedGoalAndPersistsIt() throws Exception {
        Long goalId = createGoal(CREATE_BODY, "1");

        mockMvc.perform(post("/api/v1/savings-goals/" + goalId + "/contributions")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":150.00,"currency":"USD"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(goalId.intValue()))
                .andExpect(jsonPath("$.accumulatedAmount").value(150.00))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get("/api/v1/savings-goals").header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accumulatedAmount").value(150.00));
    }

    @Test
    void contributionCompletesTheGoalWhenTargetIsReached() throws Exception {
        Long goalId = createGoal(CREATE_BODY, "1");

        mockMvc.perform(post("/api/v1/savings-goals/" + goalId + "/contributions")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":1000.00,"currency":"USD"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accumulatedAmount").value(1000.00))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void ownershipBoundaryIsEnforced() throws Exception {
        Long goalId = createGoal(CREATE_BODY, "1");

        mockMvc.perform(post("/api/v1/savings-goals/" + goalId + "/contributions")
                        .header("X-User-Id", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":10.00,"currency":"USD"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void contributionToUnknownGoalReturns404() throws Exception {
        mockMvc.perform(post("/api/v1/savings-goals/9999/contributions")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":10.00,"currency":"USD"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void contributionExceedingTargetReturns422() throws Exception {
        Long goalId = createGoal(CREATE_BODY, "1");

        mockMvc.perform(post("/api/v1/savings-goals/" + goalId + "/contributions")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":1000.01,"currency":"USD"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void createWithValidationErrorsReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/savings-goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","targetAmount":0,"currency":"US"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void getWithNoGoalsReturnsEmptyArray() throws Exception {
        mockMvc.perform(get("/api/v1/savings-goals").header("X-User-Id", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}