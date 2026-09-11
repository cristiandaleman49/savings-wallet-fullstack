package com.example.savingswallet.infrastructure.rest;

import com.example.savingswallet.application.usecase.AddContribution;
import com.example.savingswallet.application.usecase.CreateSavingsGoal;
import com.example.savingswallet.application.usecase.GetSavingsGoals;
import com.example.savingswallet.domain.money.Money;
import com.example.savingswallet.domain.savingsgoal.SavingsGoal;
import com.example.savingswallet.infrastructure.rest.dto.AddContributionRequest;
import com.example.savingswallet.infrastructure.rest.dto.CreateSavingsGoalRequest;
import com.example.savingswallet.infrastructure.rest.dto.SavingsGoalResponse;
import com.example.savingswallet.infrastructure.rest.error.InvalidCurrencyCodeException;
import com.example.savingswallet.infrastructure.sse.SavingsGoalSsePublisher;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Currency;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Inbound REST adapter for savings goals.
 *
 * <p>Kept thin: validates request shape, translates DTOs into domain objects,
 * delegates to the use cases and builds responses. No business rules live here.
 *
 * <p>MVP identity comes from the {@code X-User-Id} header (default demo user
 * {@code 1}); production identity will come from the security context.
 */
@RestController
@RequestMapping("/api/v1/savings-goals")
public class SavingsGoalController {

    private static final Logger log = LoggerFactory.getLogger(SavingsGoalController.class);

    static final String USER_HEADER = "X-User-Id";
    static final String DEMO_USER_ID = "1";

    private final GetSavingsGoals getSavingsGoals;
    private final CreateSavingsGoal createSavingsGoal;
    private final AddContribution addContribution;
    private final SavingsGoalSsePublisher ssePublisher;

    public SavingsGoalController(GetSavingsGoals getSavingsGoals, CreateSavingsGoal createSavingsGoal,
                                 AddContribution addContribution, SavingsGoalSsePublisher ssePublisher) {
        this.getSavingsGoals = getSavingsGoals;
        this.createSavingsGoal = createSavingsGoal;
        this.addContribution = addContribution;
        this.ssePublisher = ssePublisher;
    }

    @GetMapping
    public List<SavingsGoalResponse> getSavingsGoals(
            @RequestHeader(value = USER_HEADER, defaultValue = DEMO_USER_ID) Long userId) {
        return getSavingsGoals.execute(userId).stream()
                .map(SavingsGoalResponse::from)
                .toList();
    }

    @GetMapping("/events")
    public SseEmitter streamEvents(@RequestParam("userId") Long userId) {
        return ssePublisher.subscribe(userId);
    }

    /**
     * Handles SSE lifecycle failures locally so they never reach the REST
     * {@code GlobalExceptionHandler}.
     *
     * <p>When the async dispatch of {@code GET /events} fails because the
     * underlying request is already gone (client disconnected mid-stream:
     * broken pipe, reset connection, closed emitter), the failure surfaces
     * during async dispatch — as {@link AsyncRequestNotUsableException} or as
     * the raw {@link IOException} stored via
     * {@code SseEmitter.completeWithError(ex)} and rethrown by Spring's
     * {@code DeferredResult} machinery. Returning {@code void} tells Spring
     * the error is handled: no body is written, so the global handler never
     * tries to render an {@code ApiError} JSON into a response already
     * committed as {@code text/event-stream} (which produced
     * {@code HttpMessageNotWritableException}). The dead emitter itself is
     * removed by the {@code SseEmitter} lifecycle callbacks
     * ({@code onCompletion} / {@code onTimeout} / {@code onError}) and the
     * best-effort delivery in {@code SavingsGoalSsePublisher}.
     *
     * <p>This handler applies ONLY to async dispatches of the
     * {@code /events} stream endpoint. Anything else is rethrown so the
     * global REST error rendering is preserved exactly:
     *
     * <ul>
     *   <li>Any failure on a REST endpoint (REQUEST dispatch or non-SSE URI)
     *   falls through to the global {@code GlobalExceptionHandler}.
     *   <li>Synchronous errors on {@code /events} itself (missing
     *   {@code userId}, type mismatch) are dispatched as REQUEST and also
     *   fall through to the global handler, preserving the JSON
     *   {@code ApiError} contract.
     * </ul>
     */
    @ExceptionHandler({AsyncRequestNotUsableException.class, IOException.class})
    void handleSseLifecycleFailure(Exception ex, HttpServletRequest request) {
        if (isSseAsyncDispatch(request)) {
            log.debug("SSE async dispatch failed (client gone), skipping error body", ex);
            return;
        }
        throw new SseLifecycleFailure(ex, request.getRequestURI());
    }

    private static boolean isSseAsyncDispatch(HttpServletRequest request) {
        return request.getDispatcherType() == DispatcherType.ASYNC
                && request.getRequestURI() != null
                && request.getRequestURI().endsWith("/events");
    }

    /**
     * Internal wrapper used to rethrow a non-SSE lifecycle failure so it can
     * still be rendered as a JSON {@code ApiError} by
     * {@code GlobalExceptionHandler}. It extends plain {@link RuntimeException}
     * on purpose: the global {@code handleUnexpected} fallback maps it to 500
     * with the fixed "Unexpected server error" message — byte-identical to the
     * response the raw exception would have produced before this local handler
     * existed, so REST error semantics are preserved exactly.
     */
    static final class SseLifecycleFailure extends RuntimeException {

        SseLifecycleFailure(Throwable cause, String requestUri) {
            super("Failure outside the SSE stream (" + requestUri + ")", cause);
        }
    }

    @PostMapping
    public ResponseEntity<SavingsGoalResponse> createSavingsGoal(
            @Valid @RequestBody CreateSavingsGoalRequest request,
            @RequestHeader(value = USER_HEADER, defaultValue = DEMO_USER_ID) Long userId) {
        SavingsGoal goal = createSavingsGoal.execute(
                userId, request.name(), toMoney(request.targetAmount(), request.currency()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(goal.id())
                .toUri();
        return ResponseEntity.created(location).body(SavingsGoalResponse.from(goal));
    }

    @PostMapping("/{goalId}/contributions")
    public SavingsGoalResponse addContribution(
            @PathVariable Long goalId,
            @Valid @RequestBody AddContributionRequest request,
            @RequestHeader(value = USER_HEADER, defaultValue = DEMO_USER_ID) Long userId) {
        SavingsGoal goal = addContribution.execute(goalId, userId, toMoney(request.amount(), request.currency()));
        return SavingsGoalResponse.from(goal);
    }

    private static Money toMoney(BigDecimal amount, String currencyCode) {
        try {
            return new Money(amount, Currency.getInstance(currencyCode));
        } catch (IllegalArgumentException ex) {
            throw new InvalidCurrencyCodeException(currencyCode);
        }
    }
}