import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { SavingsGoalCard } from '../components/savings-goal-card';
import { SavingsGoalsStateService } from '../services/savings-goals-state.service';

/**
 * Savings Goals dashboard. Route-level page that composes the feature state
 * service and presentational cards; it contains no HTTP or business logic.
 */
@Component({
  selector: 'app-savings-goals-dashboard',
  imports: [SavingsGoalCard],
  templateUrl: './savings-goals-dashboard-page.html',
  styleUrl: './savings-goals-dashboard-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SavingsGoalsDashboardPage {
  protected readonly state = inject(SavingsGoalsStateService);

  constructor() {
    this.state.loadGoals();
  }

  /** Entry point for the contribution form (implemented in a later block). */
  protected onContribute(goalId: number): void {
    // TODO(open-form): open the contribution form for the given goal.
  }

  /** Entry point for the goal creation form (implemented in a later block). */
  protected onNewGoal(): void {
    // TODO(open-form): open the goal creation form.
  }

  /** Reloads the goals after a load error, as offered by the error state UI. */
  protected retryLoad(): void {
    this.state.loadGoals();
  }
}