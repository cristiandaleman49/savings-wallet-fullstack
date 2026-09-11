import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { SavingsGoalCard } from '../components/savings-goal-card';
import { SavingsGoalsStateService } from '../services/savings-goals-state.service';

/**
 * Savings Goals dashboard. Route-level page that composes the feature state
 * service and presentational cards; it contains no HTTP or business logic.
 *
 * Realtime wiring (SSE connection, `goal-completed` subscription and the
 * celebration dialog) lives in the App shell, whose lifetime spans navigation:
 * this page is destroyed while the user fills the contribution form, exactly
 * when the backend publishes the completion event.
 */
@Component({
  selector: 'app-savings-goals-dashboard',
  imports: [SavingsGoalCard],
  templateUrl: './savings-goals-dashboard-page.html',
  styleUrl: './savings-goals-dashboard-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SavingsGoalsDashboardPage {
  private readonly router = inject(Router);
  protected readonly state = inject(SavingsGoalsStateService);

  constructor() {
    this.state.loadGoals();
  }

  /** Opens the contribution flow for the given goal. */
  protected onContribute(goalId: number): void {
    void this.router.navigate(['/savings-goals', goalId, 'contribute']);
  }

  /** Opens the goal creation form. */
  protected onNewGoal(): void {
    void this.router.navigate(['/savings-goals/new']);
  }

  /** Reloads the goals after a load error, as offered by the error state UI. */
  protected retryLoad(): void {
    this.state.loadGoals();
  }
}