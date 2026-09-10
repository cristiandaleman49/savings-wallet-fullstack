import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { SavingsGoal } from '../../../core/models/savings-goal.model';

/**
 * Presentational card for a single savings goal. Renders server data as-is; the
 * progress percentage is a presentation-only derived value and is never stored on
 * the SavingsGoal model.
 */
@Component({
  selector: 'app-savings-goal-card',
  imports: [CurrencyPipe],
  templateUrl: './savings-goal-card.html',
  styleUrl: './savings-goal-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SavingsGoalCard {
  readonly goal = input.required<SavingsGoal>();

  /** Emits the id of the goal the user wants to contribute to. */
  readonly contribute = output<number>();

  readonly isCompleted = computed(() => this.goal().status === 'COMPLETED');

  /** Progress percentage clamped to the 0-100 range for display purposes. */
  readonly progressPercent = computed(() => {
    const { accumulatedAmount, targetAmount } = this.goal();
    if (targetAmount <= 0) {
      return 0;
    }
    return Math.min(100, Math.max(0, Math.round((accumulatedAmount / targetAmount) * 100)));
  });
}