import { CurrencyPipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  numberAttribute,
} from '@angular/core';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { SavingsGoal } from '../../../core/models/savings-goal.model';
import { AddContributionRequest } from '../../../core/models/savings-goal-request.model';
import { SavingsGoalsStateService } from '../services/savings-goals-state.service';

/**
 * Contribution form (standalone, Reactive Forms). Frontend validation is UX only:
 * the backend remains the source of truth and definitively validates the
 * contribution (e.g. not exceeding the remaining amount).
 */
@Component({
  selector: 'app-savings-goal-contribute-page',
  imports: [ReactiveFormsModule, CurrencyPipe],
  templateUrl: './savings-goal-contribute-page.html',
  styleUrl: './savings-goal-contribute-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SavingsGoalContributePage {
  /** Route parameter bound via withComponentInputBinding. */
  readonly goalId = input.required<number, unknown>({
    // numberAttribute's overloads don't satisfy the transform type directly.
    transform: (value: unknown) => numberAttribute(value, 0),
  });

  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  protected readonly state = inject(SavingsGoalsStateService);

  protected readonly form = new FormGroup({
    amount: this.fb.control<number | null>(null, {
      validators: [Validators.required, Validators.min(0.01)],
    }),
  });

  protected readonly goal = computed(() =>
    this.state.goals().find((goal) => goal.id === this.goalId()),
  );

  protected readonly isCompleted = computed(() => this.goal()?.status === 'COMPLETED');

  /** Navigation back to the dashboard happens once the updated goal is in state. */
  private goalBeforeSubmit: SavingsGoal | null = null;
  private awaitingContribution = false;

  private readonly navigateAfterContribution = effect(() => {
    // Always read the tracked signals first so the effect re-runs on changes.
    const loading = this.state.loading();
    const contributionError = this.state.error();
    const currentGoal = this.goal();

    if (!this.awaitingContribution) {
      return;
    }
    if (contributionError) {
      this.awaitingContribution = false;
      return;
    }
    if (
      !loading &&
      this.goalBeforeSubmit !== null &&
      currentGoal !== undefined &&
      currentGoal !== this.goalBeforeSubmit
    ) {
      this.awaitingContribution = false;
      void this.router.navigate(['/savings-goals']);
    }
  });

  protected hasError(path: 'amount', errorKey: string): boolean {
    const control = this.form.get(path);
    return control?.hasError(errorKey) ?? false;
  }

  protected isInvalid(path: 'amount'): boolean {
    const control = this.form.get(path);
    return control !== null && control.invalid && (control.touched || control.dirty);
  }

  protected submit(): void {
    const goal = this.goal();
    if (this.form.invalid || !goal || goal.status === 'COMPLETED') {
      this.form.markAllAsTouched();
      return;
    }
    const amount = this.form.getRawValue().amount ?? 0;
    this.goalBeforeSubmit = goal;
    this.awaitingContribution = true;
    const request: AddContributionRequest = { amount, currency: goal.currency };
    this.state.addContribution(goal.id, request);
  }

  protected cancel(): void {
    void this.router.navigate(['/savings-goals']);
  }
}