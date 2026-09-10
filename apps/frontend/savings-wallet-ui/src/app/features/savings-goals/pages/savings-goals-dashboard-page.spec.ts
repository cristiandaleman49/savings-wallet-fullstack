import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable, Subject, of, throwError } from 'rxjs';
import { SavingsGoal } from '../../../core/models/savings-goal.model';
import { AddContributionRequest, CreateSavingsGoalRequest } from '../../../core/models/savings-goal-request.model';
import { SavingsGoalsApiService } from '../../../core/services/savings-goals-api.service';
import { SavingsGoalsDashboardPage } from './savings-goals-dashboard-page';

describe('SavingsGoalsDashboardPage', () => {
  const activeGoal: SavingsGoal = {
    id: 1,
    userId: 1,
    name: 'New Bike',
    targetAmount: 1000,
    accumulatedAmount: 250,
    currency: 'EUR',
    status: 'ACTIVE',
  };

  const completedGoal: SavingsGoal = {
    id: 2,
    userId: 1,
    name: 'Vacation',
    targetAmount: 2000,
    accumulatedAmount: 2000,
    currency: 'EUR',
    status: 'COMPLETED',
  };

  let apiServiceMock: ReturnType<typeof createApiServiceMock>;

  function createApiServiceMock() {
    return {
      getSavingsGoals: vi.fn<(userId: number) => Observable<SavingsGoal[]>>(),
      createSavingsGoal: vi.fn<(request: CreateSavingsGoalRequest) => Observable<SavingsGoal>>(),
      addContribution: vi.fn<(goalId: number, request: AddContributionRequest) => Observable<SavingsGoal>>(),
    };
  }

  /** Renders the page; the constructor triggers the initial load. */
  function renderPage(): ComponentFixture<SavingsGoalsDashboardPage> {
    const fixture = TestBed.createComponent(SavingsGoalsDashboardPage);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(() => {
    apiServiceMock = createApiServiceMock();
    TestBed.configureTestingModule({
      providers: [{ provide: SavingsGoalsApiService, useValue: apiServiceMock }],
    });
  });

  it('renders one card per loaded goal', () => {
    apiServiceMock.getSavingsGoals.mockReturnValue(of([activeGoal, completedGoal]));
    const el = renderPage().nativeElement as HTMLElement;

    expect(el.querySelectorAll('.goal-card').length).toBe(2);
    expect(el.textContent).toContain('New Bike');
    expect(el.textContent).toContain('Vacation');
  });

  it('shows the loading state while the goals request is in flight', () => {
    const inFlightGoals = new Subject<SavingsGoal[]>();
    apiServiceMock.getSavingsGoals.mockReturnValue(inFlightGoals.asObservable());
    const fixture = renderPage();
    let el = fixture.nativeElement as HTMLElement;

    expect(el.querySelector('[role="status"]')?.textContent).toContain('Cargando metas de ahorro');
    expect(el.querySelector('.goal-card')).toBeNull();

    inFlightGoals.next([activeGoal]);
    inFlightGoals.complete();
    fixture.detectChanges();
    el = fixture.nativeElement as HTMLElement;

    expect(el.querySelector('[role="status"]')).toBeNull();
    expect(el.querySelectorAll('.goal-card').length).toBe(1);
  });

  it('shows the empty state when the user has no goals', () => {
    apiServiceMock.getSavingsGoals.mockReturnValue(of([]));
    const el = renderPage().nativeElement as HTMLElement;

    expect(el.textContent).toContain('Aún no tienes metas de ahorro.');
    expect(el.querySelector('.goal-card')).toBeNull();
  });

  it('shows a clear error state when loading fails', () => {
    apiServiceMock.getSavingsGoals.mockReturnValue(throwError(() => new Error('network down')));
    const el = renderPage().nativeElement as HTMLElement;

    expect(el.querySelector('[role="alert"]')?.textContent).toContain(
      'No se pudieron cargar tus metas de ahorro.',
    );
    expect(el.querySelector('.goal-card')).toBeNull();
  });

  it('allows retrying the load from the error state', () => {
    apiServiceMock.getSavingsGoals.mockReturnValue(throwError(() => new Error('network down')));
    const fixture = renderPage();
    expect(apiServiceMock.getSavingsGoals).toHaveBeenCalledTimes(1);

    const retryButton = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>(
      '.dashboard__alert .button',
    );
    retryButton?.click();

    expect(apiServiceMock.getSavingsGoals).toHaveBeenCalledTimes(2);
  });

  it('offers the Contribute action only for ACTIVE goals', () => {
    apiServiceMock.getSavingsGoals.mockReturnValue(of([activeGoal, completedGoal]));
    const el = renderPage().nativeElement as HTMLElement;

    expect(el.querySelectorAll('.goal-card__contribute').length).toBe(1);
  });

  it('gives completed goals a distinctive presentation', () => {
    apiServiceMock.getSavingsGoals.mockReturnValue(of([activeGoal, completedGoal]));
    const el = renderPage().nativeElement as HTMLElement;

    const completedCard = el.querySelector('.goal-card--completed');
    expect(completedCard).toBeTruthy();
    expect(completedCard?.textContent).toContain('Completada');
    expect(el.querySelectorAll('.goal-card:not(.goal-card--completed)').length).toBe(1);
  });

  it('offers a New goal entry-point button', () => {
    apiServiceMock.getSavingsGoals.mockReturnValue(of([]));
    const el = renderPage().nativeElement as HTMLElement;

    expect(el.querySelector('.dashboard__header .button')?.textContent).toContain('Nueva meta');
  });
});