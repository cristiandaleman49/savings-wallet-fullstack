import { ChangeDetectionStrategy, Component, effect, inject } from '@angular/core';
import { FormBuilder, FormGroup, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { SavingsGoalsStateService } from '../services/savings-goals-state.service';

type GoalFormField = 'name' | 'targetAmount' | 'currency';

/**
 * Goal creation form (standalone, Reactive Forms). Frontend validation is UX only:
 * the backend remains the source of truth for business rules.
 */
@Component({
  selector: 'app-savings-goals-create-page',
  imports: [ReactiveFormsModule],
  templateUrl: './savings-goals-create-page.html',
  styleUrl: './savings-goals-create-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SavingsGoalsCreatePage {
  private readonly fb = inject(FormBuilder);
  private readonly nonNullableFb = inject(NonNullableFormBuilder);
  private readonly router = inject(Router);

  protected readonly state = inject(SavingsGoalsStateService);

  protected readonly form = new FormGroup({
    name: this.nonNullableFb.control('', {
      validators: [Validators.required, Validators.pattern(/\S/)],
    }),
    targetAmount: this.fb.control<number | null>(null, {
      validators: [Validators.required, Validators.min(0.01)],
    }),
    currency: this.nonNullableFb.control('', {
      validators: [Validators.required, Validators.pattern(/^[A-Za-z]{3}$/)],
    }),
  });

  /** Navigation back to the dashboard is triggered once the created goal is in state. */
  private goalsCountBeforeCreate = 0;
  private awaitingCreation = false;

  private readonly navigateAfterCreation = effect(() => {
    // Always read the tracked signals first so the effect re-runs on changes.
    const loading = this.state.loading();
    const creationError = this.state.error();
    const goalsCount = this.state.goals().length;

    if (!this.awaitingCreation) {
      return;
    }
    if (creationError) {
      this.awaitingCreation = false;
      return;
    }
    if (!loading && goalsCount > this.goalsCountBeforeCreate) {
      this.awaitingCreation = false;
      void this.router.navigate(['/savings-goals']);
    }
  });

  protected hasError(path: GoalFormField, errorKey: string): boolean {
    const control = this.form.get(path);
    return control?.hasError(errorKey) ?? false;
  }

  protected isInvalid(path: GoalFormField): boolean {
    const control = this.form.get(path);
    return control !== null && control.invalid && (control.touched || control.dirty);
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    this.goalsCountBeforeCreate = this.state.goals().length;
    this.awaitingCreation = true;
    this.state.createGoal({
      name: raw.name.trim(),
      targetAmount: raw.targetAmount ?? 0,
      currency: raw.currency.trim(),
    });
  }

  protected cancel(): void {
    void this.router.navigate(['/savings-goals']);
  }
}