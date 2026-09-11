package com.example.savingswallet.infrastructure.sse;

import com.example.savingswallet.application.port.out.DomainEventPublisher;
import com.example.savingswallet.domain.event.DomainEvent;
import com.example.savingswallet.domain.event.GoalCompleted;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * In-process {@link DomainEventPublisher} that broadcasts {@link GoalCompleted}
 * events over Server-Sent Events to the {@link SseEmitter} connections
 * registered for the owner user.
 *
 * <p>This is an in-memory, single-process implementation suitable for the MVP.
 * Connections are scoped by {@code userId}, so a user never receives events
 * belonging to another user. There is no delivery guarantee across process
 * restarts or crashes: events are published synchronously within the
 * request thread and are lost if no client is connected or if the process dies
 * before the event is flushed. A production deployment would back this with a
 * transactional outbox and a durable message broker (Kafka, Redis, RabbitMQ).
 */
@Component
public class SavingsGoalSsePublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SavingsGoalSsePublisher.class);

    private static final String EVENT_NAME = "goal-completed";

    private final Map<Long, List<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public SavingsGoalSsePublisher(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    /**
     * Registers a new SSE connection for the given user and returns the
     * {@link SseEmitter} the controller should return. The connection is
     * automatically removed when it completes, times out or errors out.
     */
    /**
     * Registers a connection, wiring the standard lifecycle callbacks. Extracted
     * so tests can register a known emitter directly.
     */
    void register(Long userId, SseEmitter emitter) {
        emittersByUser.computeIfAbsent(userId, key -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(throwable -> remove(userId, emitter));
    }

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(0L);
        register(userId, emitter);
        log.info("SSE subscription opened for user {}, active connections: {}", userId,
                subscriberCount(userId));
        return emitter;
    }

    @Override
    public void publish(DomainEvent event) {
        if (event instanceof GoalCompleted goalCompleted) {
            notifySubscribers(goalCompleted);
        }
    }

    private void notifySubscribers(GoalCompleted event) {
        List<SseEmitter> subscribers = emittersByUser.getOrDefault(event.userId(), List.of());
        log.info("Publishing goal-completed event for goal {} to {} subscriber(s) of user {}",
                event.goalId(), subscribers.size(), event.userId());
        for (SseEmitter emitter : List.copyOf(subscribers)) {
            send(event, emitter);
        }
    }

    private void send(GoalCompleted event, SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event()
                    .name(EVENT_NAME)
                    .data(objectMapper.writeValueAsString(payload(event)), MediaType.APPLICATION_JSON));
        } catch (Exception ex) {
            // Best-effort delivery: a dead client (broken pipe, reset connection,
            // completed/timeout emitter, serialization failure, ...) must never
            // fail the contribution request running on this thread. Just drop the
            // connection from the registry and let the client reconnect.
            //
            // Deliberately NOT calling emitter.complete() here: after a failed
            // send the underlying output is already broken, so complete() would
            // flush again, fail, and call DeferredResult.setErrorResult(ex).
            // That schedules an async error dispatch for GET /events which ends
            // up in GlobalExceptionHandler trying to render an ApiError JSON
            // into a response already committed as text/event-stream, producing
            // HttpMessageNotWritableException. Removing without completing avoids
            // the REST error path entirely; the container reclaims the dead
            // async request on its own.
            log.debug("Failed to deliver goal-completed event to user {}, removing connection", event.userId(), ex);
            remove(event.userId(), emitter);
        }
    }

    private void remove(Long userId, SseEmitter emitter) {
        List<SseEmitter> subscribers = emittersByUser.get(userId);
        if (subscribers != null) {
            subscribers.remove(emitter);
        }
    }

    /**
     * Number of currently registered connections for the given user. Exposed
     * for observability and tests.
     */
    public int subscriberCount(Long userId) {
        List<SseEmitter> subscribers = emittersByUser.get(userId);
        return subscribers == null ? 0 : subscribers.size();
    }

    /**
     * Snapshot of the currently registered connections for the given user.
     * Exposed for observability and tests.
     */
    public List<SseEmitter> connections(Long userId) {
        List<SseEmitter> subscribers = emittersByUser.get(userId);
        return subscribers == null ? List.of() : List.copyOf(subscribers);
    }

    private GoalCompletedSseEvent payload(GoalCompleted event) {
        return new GoalCompletedSseEvent(
                event.goalId(),
                event.goalName(),
                event.targetAmount().amount().setScale(2),
                event.completedAt().toString());
    }

    /**
     * Wire-format of a {@link GoalCompleted} event sent over SSE. The
     * {@code completedAt} is rendered as an ISO-8601 string to match the
     * frontend contract.
     */
    record GoalCompletedSseEvent(Long goalId, String goalName, BigDecimal targetAmount, String completedAt) {
    }
}