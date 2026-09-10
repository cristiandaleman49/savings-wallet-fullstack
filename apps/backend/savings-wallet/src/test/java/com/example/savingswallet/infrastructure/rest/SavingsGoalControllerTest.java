package com.example.savingswallet.infrastructure.rest;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.savingswallet.application.usecase.AddContribution;
import com.example.savingswallet.application.usecase.CreateSavingsGoal;
import com.example.savingswallet.application.usecase.GetSavingsGoals;
import com.example.savingswallet.application.usecase.SavingsGoalNotFoundException;
import com.example.savingswallet.domain.money.Money;
import com.example.savingswallet.domain.savingsgoal.SavingsGoal;
import com.example.savingswallet.infrastructure.sse.SavingsGoalSsePublisher;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@WebMvcTest(SavingsGoalController.class)
class SavingsGoalControllerTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetSavingsGoals getSavingsGoals;

    @MockitoBean
    private CreateSavingsGoal createSavingsGoal;

    @MockitoBean
    private AddContribution addContribution;

    @MockitoBean
    private SavingsGoalSsePublisher ssePublisher;

    private static Money usd(String amount) {
        return new Money(new BigDecimal(amount), USD);
    }

    private static SavingsGoal goal(Long id, Long userId, String name, String target, String accumulated) {
        return new SavingsGoal(id, userId, name, usd(target), usd(accumulated));
    }

    private static final String CREATE_BODY = """
            {"name":"Vacaciones","targetAmount":1000.00,"currency":"USD"}
            """;

    @Test
    void getReturnsGoalsForTheRequestedUser() throws Exception {
        SavingsGoal goal = goal(1L, USER_ID, "Vacaciones", "1000.00", "0.00");
        when(getSavingsGoals.execute(USER_ID)).thenReturn(List.of(goal));

        mockMvc.perform(get("/api/v1/savings-goals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].userId").value(1))
                .andExpect(jsonPath("$[0].name").value("Vacaciones"))
                .andExpect(jsonPath("$[0].targetAmount").value(1000.00))
                .andExpect(jsonPath("$[0].accumulatedAmount").value(0.00))
                .andExpect(jsonPath("$[0].currency").value("USD"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void getUsesTheXUserIdHeader() throws Exception {
        when(getSavingsGoals.execute(42L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/savings-goals").header("X-User-Id", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void postCreatesGoalReturns201WithLocation() throws Exception {
        SavingsGoal created = goal(5L, USER_ID, "Vacaciones", "1000.00", "0.00");
        when(createSavingsGoal.execute(eq(USER_ID), eq("Vacaciones"), any(Money.class))).thenReturn(created);

        mockMvc.perform(post("/api/v1/savings-goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/savings-goals/5")))
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.name").value("Vacaciones"));
    }

    @Test
    void contributionReturnsUpdatedGoal() throws Exception {
        SavingsGoal updated = goal(1L, USER_ID, "Vacaciones", "1000.00", "150.00");
        when(addContribution.execute(eq(1L), eq(USER_ID), any(Money.class))).thenReturn(updated);

        mockMvc.perform(post("/api/v1/savings-goals/1/contributions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":150.00,"currency":"USD"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.accumulatedAmount").value(150.00))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    // --- validation / errors ---

    @Test
    void createWithBlankNameReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/savings-goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"   ","targetAmount":1000.00,"currency":"USD"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')]").exists());
    }

    @Test
    void createWithZeroTargetAmountReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/savings-goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Vacaciones","targetAmount":0,"currency":"USD"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'targetAmount')]").exists());
    }

    @Test
    void eventsEndpointRequiresUserId() throws Exception {
        mockMvc.perform(get("/api/v1/savings-goals/events"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Missing required parameter 'userId'"));
    }

    @Test
    void eventsEndpointSubscribesTheUserViaPublisher() throws Exception {
        SseEmitter emitter = new SseEmitter();
        when(ssePublisher.subscribe(USER_ID)).thenReturn(emitter);

        mockMvc.perform(get("/api/v1/savings-goals/events").param("userId", "1"))
                .andExpect(status().isOk());

        verify(ssePublisher).subscribe(USER_ID);
    }

    @Test
    void createWithUnsupportedCurrencyReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/savings-goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Vacaciones","targetAmount":1000.00,"currency":"ZZZ"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("unsupported currency: ZZZ"));
    }

    @Test
    void malformedBodyReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/savings-goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void contributionToUnknownGoalReturns404() throws Exception {
        when(addContribution.execute(eq(99L), eq(USER_ID), any(Money.class)))
                .thenThrow(new SavingsGoalNotFoundException(99L, USER_ID));

        mockMvc.perform(post("/api/v1/savings-goals/99/contributions")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":10.00,"currency":"USD"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void domainRuleViolationReturns422() throws Exception {
        when(createSavingsGoal.execute(eq(USER_ID), eq("Vacaciones"), any(Money.class)))
                .thenThrow(new IllegalArgumentException("exceeds the target"));

        mockMvc.perform(post("/api/v1/savings-goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void unexpectedErrorReturns500() throws Exception {
        when(getSavingsGoals.execute(USER_ID)).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/api/v1/savings-goals"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));
    }
}