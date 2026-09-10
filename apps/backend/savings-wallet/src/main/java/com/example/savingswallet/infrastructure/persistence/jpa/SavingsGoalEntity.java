package com.example.savingswallet.infrastructure.persistence.jpa;

import com.example.savingswallet.domain.savingsgoal.SavingsGoal;
import com.example.savingswallet.domain.savingsgoal.SavingsGoalStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * JPA entity mapping the {@code savings_goal} table. It is an infrastructure
 * representation of the {@link SavingsGoal} aggregate; the domain model itself
 * remains free of persistence annotations.
 *
 * <p>Money amounts are stored as {@link BigDecimal}; the currency code is stored
 * once because the aggregate guarantees that target and accumulated amounts share
 * the same currency. The lifecycle status is persisted for queryability; when
 * reconstructing the aggregate, the status is derived again by the domain rules.
 */
@Entity
@Table(name = "savings_goal")
public class SavingsGoalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "target_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal targetAmount;

    @Column(name = "accumulated_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal accumulatedAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SavingsGoalStatus status;

    protected SavingsGoalEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(BigDecimal targetAmount) {
        this.targetAmount = targetAmount;
    }

    public BigDecimal getAccumulatedAmount() {
        return accumulatedAmount;
    }

    public void setAccumulatedAmount(BigDecimal accumulatedAmount) {
        this.accumulatedAmount = accumulatedAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public SavingsGoalStatus getStatus() {
        return status;
    }

    public void setStatus(SavingsGoalStatus status) {
        this.status = status;
    }
}
