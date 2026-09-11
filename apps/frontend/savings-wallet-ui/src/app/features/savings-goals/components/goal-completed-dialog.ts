import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { GoalCompletedEvent } from '../../../core/models/goal-completed-event.model';

/**
 * Celebration dialog shown when a savings goal is completed.
 *
 * Purely presentational: receives the already-validated SSE payload as input
 * and notifies the parent when the user dismisses it. It owns no connection,
 * navigation or state logic.
 *
 * Note: GoalCompletedEvent carries no currency, so the target amount is
 * rendered with DecimalPipe instead of CurrencyPipe.
 */
@Component({
  selector: 'app-goal-completed-dialog',
  imports: [DecimalPipe],
  templateUrl: './goal-completed-dialog.html',
  styleUrl: './goal-completed-dialog.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GoalCompletedDialog {
  readonly completedEvent = input.required<GoalCompletedEvent>();

  /** Emitted when the user acknowledges the dialog. */
  readonly closed = output<void>();

  protected onContinue(): void {
    this.closed.emit();
  }
}
