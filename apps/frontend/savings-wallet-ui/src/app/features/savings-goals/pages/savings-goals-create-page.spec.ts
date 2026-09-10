import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { provideRouter } from '@angular/router';
import { SavingsGoal } from '../../../core/models/savings-goal.model';
import { CreateSavingsGoalRequest } from '../../../core/models/savings-goal-request.model';
import { SavingsGoalsStateService } from '../services/savings-goals-state.service';
import { SavingsGoalsCreatePage } from './savings-goals-create-page';

describe('SavingsGoalsCreatePage', () => {
  const createdGoal: SavingsGoal = {
    id: 10,
    userId: 1,
    name: 'Bicicleta nueva',
    targetAmount: 500,
    accumulatedAmount: 0,
    currency: 'EUR',
    status: 'ACTIVE',
  };

  let stateMock: ReturnType<typeof createStateMock>;
  let router: Router;

  function createStateMock() {
    return {
      goals: signal<SavingsGoal[]>([]),
      loading: signal(false),
      error: signal<string | null>(null),
      createGoal: vi.fn<(request: CreateSavingsGoalRequest) => void>(),
    };
  }

  function renderPage(): ComponentFixture<SavingsGoalsCreatePage> {
    const fixture = TestBed.createComponent(SavingsGoalsCreatePage);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
    return fixture;
  }

  /** Sets a control value and marks the field as touched, as the user would. */
  function setInput(el: HTMLElement, selector: string, value: string): void {
    const input = el.querySelector<HTMLInputElement>(selector);
    if (!input) {
      throw new Error(`input not found: ${selector}`);
    }
    input.value = value;
    input.dispatchEvent(new Event('input'));
    input.dispatchEvent(new Event('blur'));
  }

  /** Triggers the ngSubmit handler bypassing the (disabled) submit button. */
  function submitForm(el: HTMLElement): void {
    const form = el.querySelector<HTMLFormElement>('form');
    if (!form) {
      throw new Error('form not found');
    }
    form.dispatchEvent(new Event('submit'));
  }

  function fillValidForm(el: HTMLElement): void {
    setInput(el, '#goal-name', 'Bicicleta nueva');
    setInput(el, '#goal-target-amount', '500');
    setInput(el, '#goal-currency', 'EUR');
  }

  beforeEach(() => {
    stateMock = createStateMock();
    TestBed.configureTestingModule({
      imports: [SavingsGoalsCreatePage],
      providers: [provideRouter([]), { provide: SavingsGoalsStateService, useValue: stateMock }],
    });
  });

  it('renders the creation form with its fields and actions', () => {
    const el = renderPage().nativeElement as HTMLElement;

    expect(el.querySelector('.create-goal__title')?.textContent).toContain('Nueva meta de ahorro');
    expect(el.querySelector('#goal-name')).toBeTruthy();
    expect(el.querySelector('#goal-target-amount')).toBeTruthy();
    expect(el.querySelector('#goal-currency')).toBeTruthy();
    expect(el.textContent).toContain('Cancelar');
    expect(el.textContent).toContain('Crear meta');
  });

  it('keeps the submit button disabled while the form is invalid', () => {
    const el = renderPage().nativeElement as HTMLElement;

    const submitButton = el.querySelector<HTMLButtonElement>('.create-goal__actions .button--primary');
    expect(submitButton?.disabled).toBe(true);
  });

  it('shows the required-field messages for touched empty fields', () => {
    const fixture = renderPage();
    const el = fixture.nativeElement as HTMLElement;

    setInput(el, '#goal-name', '');
    setInput(el, '#goal-target-amount', '');
    setInput(el, '#goal-currency', '');
    fixture.detectChanges();

    expect(el.textContent).toContain('El nombre es obligatorio.');
    expect(el.textContent).toContain('El monto objetivo es obligatorio.');
    expect(el.textContent).toContain('La moneda es obligatoria.');
  });

  it('rejects a non-positive target amount', () => {
    const fixture = renderPage();
    const el = fixture.nativeElement as HTMLElement;

    fillValidForm(el);
    setInput(el, '#goal-target-amount', '0');
    submitForm(el);
    fixture.detectChanges();

    expect(el.textContent).toContain('El monto objetivo debe ser mayor que 0.');
    expect(stateMock.createGoal).not.toHaveBeenCalled();
  });

  it('rejects a currency that does not match the 3-letter format', () => {
    const fixture = renderPage();
    const el = fixture.nativeElement as HTMLElement;

    fillValidForm(el);
    setInput(el, '#goal-currency', 'EU');
    submitForm(el);
    fixture.detectChanges();

    expect(el.textContent).toContain('La moneda debe tener formato de 3 letras.');
    expect(stateMock.createGoal).not.toHaveBeenCalled();
  });

  it('does not call the state service when the submit is invalid', () => {
    const fixture = renderPage();
    const el = fixture.nativeElement as HTMLElement;

    setInput(el, '#goal-name', 'Bicicleta nueva');
    submitForm(el);

    expect(stateMock.createGoal).not.toHaveBeenCalled();
  });

  it('calls createGoal with the form payload on a valid submit', () => {
    const fixture = renderPage();
    const el = fixture.nativeElement as HTMLElement;

    fillValidForm(el);
    submitForm(el);

    expect(stateMock.createGoal).toHaveBeenCalledWith({
      name: 'Bicicleta nueva',
      targetAmount: 500,
      currency: 'EUR',
    });
  });

  it('shows the sending state while the creation is in flight', () => {
    const fixture = renderPage();
    const el = fixture.nativeElement as HTMLElement;

    stateMock.createGoal.mockImplementation(() => stateMock.loading.set(true));
    fillValidForm(el);
    submitForm(el);
    fixture.detectChanges();

    const submitButton = el.querySelector<HTMLButtonElement>('.create-goal__actions .button--primary');
    expect(submitButton?.disabled).toBe(true);
    expect(submitButton?.textContent).toContain('Creando…');

    stateMock.loading.set(false);
  });

  it('shows the backend rejection message and stays on the form on error', async () => {
    const fixture = renderPage();
    const el = fixture.nativeElement as HTMLElement;

    stateMock.createGoal.mockImplementation(() => {
      stateMock.loading.set(false);
      stateMock.error.set('No se pudo crear la meta de ahorro. Inténtalo de nuevo.');
    });
    fillValidForm(el);
    submitForm(el);
    fixture.detectChanges();

    expect(el.querySelector('[role="alert"]')?.textContent).toContain(
      'No se pudo crear la meta de ahorro.',
    );
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('navigates back to the dashboard when Cancelar is clicked', () => {
    const fixture = renderPage();
    const el = fixture.nativeElement as HTMLElement;

    const cancelButton = el.querySelector<HTMLButtonElement>('.create-goal__actions .button--ghost');
    cancelButton?.click();

    expect(router.navigate).toHaveBeenCalledWith(['/savings-goals']);
  });

  it('navigates back to the dashboard once the created goal is in state', async () => {
    const fixture = renderPage();
    const el = fixture.nativeElement as HTMLElement;

    stateMock.createGoal.mockImplementation(() => {
      stateMock.loading.set(false);
      stateMock.goals.set([createdGoal]);
    });
    fillValidForm(el);
    submitForm(el);

    await fixture.whenStable();
    fixture.detectChanges();

    expect(router.navigate).toHaveBeenCalledWith(['/savings-goals']);
  });
});