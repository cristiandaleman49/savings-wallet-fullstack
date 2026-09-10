package com.example.savingswallet.infrastructure.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.savingswallet.domain.event.DomainEvent;
import com.example.savingswallet.domain.event.GoalCompleted;
import com.example.savingswallet.domain.money.Money;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class SavingsGoalSsePublisherTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Long USER_ID = 42L;

    private SavingsGoalSsePublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new SavingsGoalSsePublisher(OBJECT_MAPPER);
    }

    private static Money usd(String amount) {
        return new Money(new BigDecimal(amount), USD);
    }

    private static GoalCompleted event(Long goalId, Long userId) {
        return new GoalCompleted(goalId, userId, "Vacaciones", usd("1000.00"), Instant.now());
    }

    @Test
    void subscribeRegistersConnectionForUser() {
        SseEmitter emitter = publisher.subscribe(USER_ID);

        assertThat(emitter).isNotNull();
        assertThat(publisher.subscriberCount(USER_ID)).isEqualTo(1);
    }

    @Test
    void subscribeTracksMultipleUsersIndependently() {
        publisher.subscribe(1L);
        publisher.subscribe(1L);
        publisher.subscribe(2L);

        assertThat(publisher.subscriberCount(1L)).isEqualTo(2);
        assertThat(publisher.subscriberCount(2L)).isEqualTo(1);
        assertThat(publisher.subscriberCount(3L)).isEqualTo(0);
    }

    @Test
    void publishSendsEventOnlyToSubscribedUser() throws Exception {
        SseEmitter userEmitter = mock(SseEmitter.class);
        SseEmitter otherUserEmitter = mock(SseEmitter.class);

        SavingsGoalSsePublisher spied = new SavingsGoalSsePublisherForTest(userEmitter);
        spied.register(1L, otherUserEmitter);

        spied.publish(event(1L, USER_ID));

        verify(userEmitter).send(any(SseEmitter.SseEventBuilder.class));
        verify(otherUserEmitter, never()).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void publishIgnoresUnknownEventTypes() throws Exception {
        SseEmitter userEmitter = mock(SseEmitter.class);
        SavingsGoalSsePublisher spied = new SavingsGoalSsePublisherForTest(userEmitter);

        assertThatNoException().isThrownBy(() -> spied.publish(new UnknownDomainEvent()));
        verify(userEmitter, never()).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void failedDeliveryRemovesTheConnection() throws Exception {
        SseEmitter failingEmitter = mock(SseEmitter.class);
        doThrow(new java.io.IOException("boom")).when(failingEmitter).send(any(SseEmitter.SseEventBuilder.class));

        SavingsGoalSsePublisher spied = new SavingsGoalSsePublisherForTest(failingEmitter);

        spied.publish(event(1L, USER_ID));

        assertThat(spied.subscriberCount(USER_ID)).isEqualTo(0);
    }

    private static final class SavingsGoalSsePublisherForTest extends SavingsGoalSsePublisher {

        SavingsGoalSsePublisherForTest(SseEmitter emitter) {
            super(OBJECT_MAPPER);
            register(USER_ID, emitter);
        }
    }

    private static final class UnknownDomainEvent implements DomainEvent {
        @Override
        public Instant occurredAt() {
            return Instant.now();
        }
    }
}
