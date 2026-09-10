import { Injectable, inject, signal } from '@angular/core';
import { Observable, finalize } from 'rxjs';
import { SavingsGoal } from '../../../core/models/savings-goal.model';
import { AddContributionRequest, CreateSavingsGoalRequest } from '../../../core/models/savings-goal-request.model';
import { DEMO_USER_ID, SavingsGoalsApiService } from '../../../core/services/savings-goals-api.service';

const LOAD_GOALS_ERROR_MESSAGE = 'Could not load your savings goals. Please try again.';
const CREATE_GOAL_ERROR_MESSAGE = 'Could not create the savings goal. Please try again.';
const ADD_CONTRIBUTION_ERROR_MESSAGE = 'Could not add the contribution. Please try again.';

/**
 * Feature state for savings goals. Components read the readonly signals and call
 * the public methods; all HTTP access stays inside SavingsGoalsApiService and no
 * SavingsGoal business rule is applied here (server responses are stored as-is).
 */
@Injectable({
  providedIn: 'root',
})
export class SavingsGoalsStateService {
  private readonly apiService = inject(SavingsGoalsApiService);

  private readonly goalsSignal = signal<SavingsGoal[]>([]);
  private readonly loadingSignal = signal(false);
  private readonly errorSignal = signal<string | null>(null);

  /** Read-only projections of the feature state for the UI. */
  readonly goals = this.goalsSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  /** Loads the savings goals of the given user (demo user by default). */
  loadGoals(userId?: number): void {
    this.subscribeWithState(
      this.apiService.getSavingsGoals(userId ?? DEMO_USER_ID),
      (goals) => this.goalsSignal.set(goals),
      LOAD_GOALS_ERROR_MESSAGE,
    );
  }

  /** Creates a savings goal and appends it to the current state. */
  createGoal(request: CreateSavingsGoalRequest): void {
    this.subscribeWithState(
      this.apiService.createSavingsGoal(request),
      (createdGoal) => this.goalsSignal.update((goals) => [...goals, createdGoal]),
      CREATE_GOAL_ERROR_MESSAGE,
    );
  }

  /**
   * Registers a contribution and replaces the affected goal with the updated
   * version returned by the API (no local monetary calculation).
   */
  addContribution(goalId: number, request: AddContributionRequest): void {
    this.subscribeWithState(
      this.apiService.addContribution(goalId, request),
      (updatedGoal) =>
        this.goalsSignal.update((goals) =>
          goals.map((goal) => (goal.id === updatedGoal.id ? updatedGoal : goal)),
        ),
      ADD_CONTRIBUTION_ERROR_MESSAGE,
    );
  }

  /**
   * Shared request bookkeeping: toggles `loading` around the request, clears
   * `error` on success and records an actionable message on failure without
   * discarding the currently loaded goals.
   */
  private subscribeWithState<T>(
    request: Observable<T>,
    onSuccess: (result: T) => void,
    errorMessage: string,
  ): void {
    this.loadingSignal.set(true);
    request.pipe(finalize(() => this.loadingSignal.set(false))).subscribe({
      next: (result) => {
        this.errorSignal.set(null);
        onSuccess(result);
      },
      error: () => this.errorSignal.set(errorMessage),
    });
  }
}