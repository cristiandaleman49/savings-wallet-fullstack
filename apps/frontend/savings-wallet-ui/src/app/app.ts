import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterOutlet } from '@angular/router';
import { GoalCompletedEvent } from './core/models/goal-completed-event.model';
import { SavingsGoalsEventService } from './core/services/savings-goals-event.service';
import { GoalCompletedDialog } from './features/savings-goals/components/goal-completed-dialog';

/**
 * Application shell. Renders the active route; feature pages compose their own
 * headers and content.
 *
 * It also owns the single shared SSE connection AND the celebration dialog for
 * its whole lifetime. Both must live here, not in the dashboard: navigating to
 * the creation or contribution pages destroys the dashboard, which would close
 * the stream (and drop the subscription) exactly while the contribution POST
 * publishes `goal-completed` — the backend delivers synchronously with no
 * replay, so the event would be lost and the dialog would never appear.
 */
@Component({
  imports: [RouterOutlet, GoalCompletedDialog],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {
  private readonly events = inject(SavingsGoalsEventService);
  private readonly destroyRef = inject(DestroyRef);

  /** Latest completed-goal event; null hides the celebration dialog. */
  protected readonly completedEvent = signal<GoalCompletedEvent | null>(null);

  constructor() {
    // Singleton service guards against duplicate connections internally.
    // The subscription lives as long as the app, so events published while
    // any page (e.g. the contribution form) is active are never missed.
    this.events.connect();
    this.events.goalCompleted$
      .pipe(takeUntilDestroyed())
      .subscribe((event) => this.completedEvent.set(event));
    this.destroyRef.onDestroy(() => this.events.disconnect());
  }

  /** Dismisses the celebration dialog after a goal-completed event. */
  protected dismissCompletedDialog(): void {
    this.completedEvent.set(null);
  }
}
