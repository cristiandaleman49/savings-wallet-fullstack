package com.example.savingswallet.infrastructure.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.savingswallet.domain.money.Money;
import com.example.savingswallet.domain.savingsgoal.SavingsGoal;
import com.example.savingswallet.infrastructure.sse.SavingsGoalSsePublisher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SavingsGoalSseIntegrationTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private SavingsGoalSsePublisher ssePublisher;
    @Autowired
    private com.example.savingswallet.application.port.out.SavingsGoalRepository repository;

    private static Money usd(String amount) {
        return new Money(new BigDecimal(amount), USD);
    }

    private Long createGoal(Long userId, String name, String target) {
        return repository.save(SavingsGoal.open(userId, name, usd(target))).id();
    }

    private MvcResult openEventsStream(Long userId) throws Exception {
        return mockMvc.perform(get("/api/v1/savings-goals/events").param("userId", userId.toString()))
                .andExpect(request().asyncStarted())
                .andExpect(status().isOk())
                .andReturn();
    }

    private void completeConnections(Long userId) {
        ssePublisher.connections(userId).forEach(SseEmitter::complete);
    }

    @Test
    void eventsEndpointRespondsWithTextEventStream() throws Exception {
        MvcResult result = openEventsStream(1L);
        completeConnections(1L);

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
    }

    @Test
    void completingGoalDeliversEventToSubscribedUser() throws Exception {
        Long goalId = createGoal(1L, "Vacaciones", "1000.00");

        MvcResult sseResult = openEventsStream(1L);

        mockMvc.perform(contributionRequest(goalId, "1", "1000.00"))
                .andExpect(status().isOk());

        completeConnections(1L);

        MvcResult dispatched = mockMvc.perform(asyncDispatch(sseResult)).andReturn();
        String body = dispatched.getResponse().getContentAsString();

        assertThat(body).contains("event:goal-completed");
        JsonNode payload = OBJECT_MAPPER.readTree(extractData(body));
        assertThat(payload.get("goalId").asLong()).isEqualTo(goalId);
        assertThat(payload.get("goalName").asText()).isEqualTo("Vacaciones");
        assertThat(payload.get("targetAmount").asDouble()).isEqualTo(1000.00);
        assertThat(payload.get("completedAt").asText()).isNotEmpty();
    }

    @Test
    void eventIsNotDeliveredToOtherUser() throws Exception {
        Long goalId = createGoal(1L, "Vacaciones", "1000.00");

        MvcResult sseResult = openEventsStream(2L);

        mockMvc.perform(contributionRequest(goalId, "1", "1000.00"))
                .andExpect(status().isOk());

        completeConnections(2L);

        MvcResult dispatched = mockMvc.perform(asyncDispatch(sseResult)).andReturn();
        assertThat(dispatched.getResponse().getContentAsString()).doesNotContain("goal-completed");
    }

    @Test
    void multipleSubscribersOfTheSameUserAllReceiveTheEvent() throws Exception {
        Long goalId = createGoal(1L, "Vacaciones", "1000.00");

        MvcResult first = openEventsStream(1L);
        MvcResult second = openEventsStream(1L);

        mockMvc.perform(contributionRequest(goalId, "1", "1000.00")).andExpect(status().isOk());

        completeConnections(1L);

        assertThat(mockMvc.perform(asyncDispatch(first)).andReturn().getResponse().getContentAsString())
                .contains("goal-completed");
        assertThat(mockMvc.perform(asyncDispatch(second)).andReturn().getResponse().getContentAsString())
                .contains("goal-completed");
    }

    @Test
    void disconnectRemovesTheConnection() throws Exception {
        MvcResult result = openEventsStream(1L);
        assertThat(ssePublisher.subscriberCount(1L)).isEqualTo(1);

        SseEmitter emitter = ssePublisher.connections(1L).get(0);
        emitter.complete();

        // asyncDispatch inicializa el emisor, detecta complete=true y dispara onCompletion -> remove.
        mockMvc.perform(asyncDispatch(result)).andReturn();

        assertThat(ssePublisher.subscriberCount(1L)).isEqualTo(0);
    }

    @Test
    void sseAsyncFailureDoesNotRenderApiErrorAsEventStream() throws Exception {
        // Regression test for the production warning:
        //   HttpMessageNotWritableException: No converter for [ApiError]
        //   with preset Content-Type 'text/event-stream'
        //
        // When the async SSE dispatch fails after the response was committed
        // as text/event-stream (client disconnected mid-stream), Spring must
        // NOT try to render the global ApiError JSON into that response:
        // no message converter can write ApiError with the SSE content type.
        // The controller-local @ExceptionHandler(AsyncRequestNotUsableException)
        // in SavingsGoalController swallows the failure silently (void, no
        // body), so the failure stays silent and REST error handling is
        // untouched. The dead emitter is removed by the lifecycle callbacks.
        MvcResult result = openEventsStream(1L);
        SseEmitter emitter = ssePublisher.connections(1L).get(0);
        emitter.completeWithError(new AsyncRequestNotUsableException("Simulated client abort"));

        MvcResult dispatched = mockMvc.perform(asyncDispatch(result)).andReturn();

        assertThat(dispatched.getResponse().getContentAsString()).doesNotContain("Internal Server Error");
        assertThat(dispatched.getResponse().getContentAsString()).doesNotContain("\"status\":500");
        assertThat(ssePublisher.subscriberCount(1L)).isEqualTo(0);
    }

    private MockHttpServletRequestBuilder contributionRequest(Long goalId, String userId, String amount) {
        return post("/api/v1/savings-goals/" + goalId + "/contributions")
                .header("X-User-Id", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"amount":%s,"currency":"USD"}
                        """.formatted(amount));
    }

    private static String extractData(String sseBody) {
        for (String line : sseBody.split("\\R")) {
            if (line.startsWith("data:")) {
                return line.substring("data:".length());
            }
        }
        throw new IllegalStateException("No data line found in SSE body: " + sseBody);
    }
}
