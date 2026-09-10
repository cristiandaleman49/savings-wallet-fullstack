import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { provideRouter } from '@angular/router';
import { SavingsGoal } from '../../../core/models/savings-goal.model';
import { AddContributionRequest } from '../../../core/models/savings-goal-request.model';
import { SavingsGoalsStateService } from '../services/savings-goals-state.service';
import { SavingsGoalContributePage } from './savings-goal-contribute-page';

describe('SavingsGoalContributePage', () => {
  const activeGoal: SavingsGoal = {
    id: 1,
    userId: 1,
    name: 'Bicicleta nueva',
    targetAmount: 1000,
    accumulatedAmount: 250,
    currency: 'EUR',
    status: 'ACTIVE',
  };

  const updatedGoal: SavingsGoal = { ...activeGoal, accumulatedAmount: 350 };

  let stateMock: ReturnType<typeof createStateMock>;
  let router: Router;

  function createStateMock() {
    return {
      goals: signal<SavingsGoal[]>([]),
      loading: signal(false),
      error: signal<string | null>(null),
      addContribution: vi.fn<(goalId: number, request: AddContributionRequest) => void>(),
      loadGoals: vi.fn(),
      createGoal: vi.fn(),
    };
  }

  function renderPage(goalId: number): ComponentFixture<SavingsGoalContributePage> {
    const fixture = TestBed.createComponent(SavingsGoalContributePage);
    fixture.componentRef.setInput('goalId', goalId);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
    return fixture;
  }

  /** Triggers the ngSubmit handler bypassing the (disabled) submit button. */
  function submitForm(el: HTMLElement): void {
    const form = el.querySelector<HTMLFormElement>('form');
    if (!form) {
      throw new Error('form not found');
    }
    form.dispatchEvent(new Event('submit'));
  }

  function setAmount(el: HTMLElement, value: string): void {
    const input = el.querySelector<HTMLInputElement>('#contribution-amount');
    if (!input) {
      throw new Error('amount input not found');
    }
    input.value = value;
    input.dispatchEvent(new Event('input'));
    input.dispatchEvent(new Event('blur'));
  }

  beforeEach(() => {
    stateMock = createStateMock();
    TestBed.configureTestingModule({
      imports: [SavingsGoalContributePage],
      providers: [provideRouter([]), { provide: SavingsGoalsStateService, useValue: stateMock }],
    });
  });

  it('renders the contribution form with its fields and actions', () => {
    stateMock.goals.set([activeGoal]);
    const el = renderPage(activeGoal.id).nativeElement as HTMLElement;

    expect(el.querySelector('.contribute__title')?.textContent).toContain('Bicicleta nueva');
    expect(el.querySelector('#contribution-amount')).toBeTruthy();
    expect(el.textContent).toContain('Cancelar');
    expect(el.textContent).toContain('Contribuir');
    expect(el.querySelector<HTMLButtonElement>('.contribute__actions .button--primary')?.disabled).toBe(
      true,
    );
  });

  it('shows the selected goal summary (accumulated and target)', () => {
    stateMock.goals.set([activeGoal]);
    const el = renderPage(activeGoal.id).nativeElement as HTMLElement;

    expect(el.textContent).toContain('Acumulado');
    expect(el.textContent).toContain('Objetivo');
    expect(el.textContent).toContain('250.00');
    expect(el.textContent).toContain('1,000.00');
  });

  it('shows the required-amount message for a touched empty field', () => {
    stateMock.goals.set([activeGoal]);
    const fixture = renderPage(activeGoal.id);
    const el = fixture.nativeElement as HTMLElement;

    setAmount(el, '');
    fixture.detectChanges();

    expect(el.textContent).toContain('El monto es obligatorio.');
  });

  it('rejects a non-positive contribution amount', () => {
    stateMock.goals.set([activeGoal]);
    const fixture = renderPage(activeGoal.id);
    const el = fixture.nativeElement as HTMLElement;

    setAmount(el, '0');
    submitForm(el);
    fixture.detectChanges();

    expect(el.textContent).toContain('El monto debe ser mayor que 0.');
    expect(stateMock.addContribution).not.toHaveBeenCalled();
  });

  it('does not call the state service when the submit is invalid', () => {
    stateMock.goals.set([activeGoal]);
    const fixture = renderPage(activeGoal.id);
    const el = fixture.nativeElement as HTMLElement;

    submitForm(el);

    expect(stateMock.addContribution).not.toHaveBeenCalled();
  });

  it('calls addContribution with the goal id and payload on a valid submit', () => {
    stateMock.goals.set([activeGoal]);
    const fixture = renderPage(activeGoal.id);
    const el = fixture.nativeElement as HTMLElement;

    setAmount(el, '100');
    submitForm(el);

    expect(stateMock.addContribution).toHaveBeenCalledWith(1, { amount: 100, currency: 'EUR' });
  });

  it('shows the sending state while the contribution is in flight', () => {
    stateMock.goals.set([activeGoal]);
    const fixture = renderPage(activeGoal.id);
    const el = fixture.nativeElement as HTMLElement;

    stateMock.addContribution.mockImplementation(() => stateMock.loading.set(true));
    setAmount(el, '100');
    submitForm(el);
    fixture.detectChanges();

    const submitButton = el.querySelector<HTMLButtonElement>('.contribute__actions .button--primary');
    expect(submitButton?.disabled).toBe(true);
    expect(submitButton?.textContent).toContain('Contribuyendo…');

    stateMock.loading.set(false);
  });

  it('shows the backend rejection message and stays on the form on error', () => {
    stateMock.goals.set([activeGoal]);
    const fixture = renderPage(activeGoal.id);
    const el = fixture.nativeElement as HTMLElement;

    stateMock.addContribution.mockImplementation(() => {
      stateMock.loading.set(false);
      stateMock.error.set('No se pudo registrar la contribución. Inténtalo de nuevo.');
    });
    setAmount(el, '100');
    submitForm(el);
    fixture.detectChanges();

    expect(el.querySelector('[role="alert"]')?.textContent).toContain(
      'No se pudo registrar la contribución.',
    );
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('navigates back to the dashboard when Cancelar is clicked', () => {
    stateMock.goals.set([activeGoal]);
    const fixture = renderPage(activeGoal.id);
    const el = fixture.nativeElement as HTMLElement;

    const cancelButton = el.querySelector<HTMLButtonElement>('.contribute__actions .button--ghost');
    cancelButton?.click();

    expect(router.navigate).toHaveBeenCalledWith(['/savings-goals']);
  });

  it('navigates back to the dashboard once the updated goal is in state', async () => {
    stateMock.goals.set([activeGoal]);
    const fixture = renderPage(activeGoal.id);
    const el = fixture.nativeElement as HTMLElement;

    stateMock.addContribution.mockImplementation(() => {
      stateMock.loading.set(false);
      stateMock.goals.set([updatedGoal]);
    });
    setAmount(el, '100');
    submitForm(el);

    await fixture.whenStable();
    fixture.detectChanges();

    expect(router.navigate).toHaveBeenCalledWith(['/savings-goals']);
  });

  it('does not allow contributing to a COMPLETED goal', () => {
    stateMock.goals.set([{ ...activeGoal, id: 2, name: 'Vacaciones', status: 'COMPLETED' }]);
    const fixture = renderPage(2);
    const el = fixture.nativeElement as HTMLElement;

    expect(el.textContent).toContain('Esta meta ya está completada');
    expect(el.querySelector('form')).toBeNull();
    expect(stateMock.addContribution).not.toHaveBeenCalled();
  });

  it('shows a not-found state and no form when the goal is not in state', () => {
    stateMock.goals.set([activeGoal]);
    const el = renderPage(99).nativeElement as HTMLElement;

    expect(el.textContent).toContain('No se encontró la meta solicitada.');
    expect(el.querySelector('form')).toBeNull();
    expect(stateMock.addContribution).not.toHaveBeenCalled();
  });
});