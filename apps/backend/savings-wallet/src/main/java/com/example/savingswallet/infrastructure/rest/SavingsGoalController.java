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
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Currency;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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