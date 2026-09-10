import { TestBed } from '@angular/core/testing';
import { Observable, Subject, of, throwError } from 'rxjs';
import { SavingsGoal } from '../../../core/models/savings-goal.model';
import { AddContributionRequest, CreateSavingsGoalRequest } from '../../../core/models/savings-goal-request.model';
import { SavingsGoalsApiService } from '../../../core/services/savings-goals-api.service';
import { SavingsGoalsStateService } from './savings-goals-state.service';

describe('SavingsGoalsStateService', () => {
  const existingGoal: SavingsGoal = {
    id: 1,
    userId: 1,
    name: 'New Bike',
    targetAmount: 1000,
    accumulatedAmount: 250,
    currency: 'EUR',
    status: 'ACTIVE',
  };

  const otherGoal: SavingsGoal = {
    id: 2,
    userId: 1,
    name: 'Vacation',
    targetAmount: 2000,
    accumulatedAmount: 500,
    currency: 'EUR',
    status: 'ACTIVE',
  };

  const createdGoal: SavingsGoal = {
    id: 3,
    userId: 1,
    name: 'Emergency Fund',
    targetAmount: 500,
    accumulatedAmount: 0,
    currency: 'EUR',
    status: 'ACTIVE',
  };

  const updatedGoal: SavingsGoal = { ...existingGoal, accumulatedAmount: 350 };

  let stateService: SavingsGoalsStateService;
  let apiServiceMock: ReturnType<typeof createApiServiceMock>;

  function createApiServiceMock() {
    return {
      getSavingsGoals: vi.fn<(userId: number) => Observable<SavingsGoal[]>>(),
      createSavingsGoal: vi.fn<(request: CreateSavingsGoalRequest) => Observable<SavingsGoal>>(),
      addContribution: vi.fn<(goalId: number, request: AddContributionRequest) => Observable<SavingsGoal>>(),
    };
  }

  /** Populates the state through a successful load, as the UI would do. */
  function seedGoals(goals: SavingsGoal[]): void {
    apiServiceMock.getSavingsGoals.mockReturnValue(of(goals));
    stateService.loadGoals();
  }

  beforeEach(() => {
    apiServiceMock = createApiServiceMock();
    TestBed.configureTestingModule({
      providers: [{ provide: SavingsGoalsApiService, useValue: apiServiceMock }],
    });
    stateService = TestBed.inject(SavingsGoalsStateService);
  });

  it('loads the goals of the demo user when no user id is provided', () => {
    apiServiceMock.getSavingsGoals.mockReturnValue(of([existingGoal]));

    stateService.loadGoals();

    expect(apiServiceMock.getSavingsGoals).toHaveBeenCalledWith(1);
    expect(stateService.goals()).toEqual([existingGoal]);
    expect(stateService.loading()).toBe(false);
    expect(stateService.error()).toBeNull();
  });

  it('keeps loading true while a load is in flight and false once it settles', () => {
    const inFlightGoals = new Subject<SavingsGoal[]>();
    apiServiceMock.getSavingsGoals.mockReturnValue(inFlightGoals.asObservable());

    stateService.loadGoals();
    expect(stateService.loading()).toBe(true);

    inFlightGoals.next([existingGoal]);
    inFlightGoals.complete();

    expect(stateService.loading()).toBe(false);
    expect(stateService.goals()).toEqual([existingGoal]);
  });

  it('sets an error and preserves the previously loaded goals when loading fails', () => {
    seedGoals([existingGoal]);
    expect(stateService.error()).toBeNull();

    apiServiceMock.getSavingsGoals.mockReturnValueOnce(throwError(() => new Error('network down')));
    stateService.loadGoals();

    expect(stateService.error()).toBe('Could not load your savings goals. Please try again.');
    expect(stateService.loading()).toBe(false);
    expect(stateService.goals()).toEqual([existingGoal]);
  });

  it('appends the created goal to the current goals', () => {
    seedGoals([existingGoal]);
    const request: CreateSavingsGoalRequest = {
      name: 'Emergency Fund',
      targetAmount: 500,
      currency: 'EUR',
    };
    apiServiceMock.createSavingsGoal.mockReturnValueOnce(of(createdGoal));

    stateService.createGoal(request);

    expect(apiServiceMock.createSavingsGoal).toHaveBeenCalledWith(request);
    expect(stateService.goals()).toEqual([existingGoal, createdGoal]);
    expect(stateService.loading()).toBe(false);
    expect(stateService.error()).toBeNull();
  });

  it('sets an error and keeps the goals unchanged when creating a goal fails', () => {
    seedGoals([existingGoal]);
    const request: CreateSavingsGoalRequest = {
      name: 'Emergency Fund',
      targetAmount: 500,
      currency: 'EUR',
    };
    apiServiceMock.createSavingsGoal.mockReturnValueOnce(throwError(() => new Error('validation failed')));

    stateService.createGoal(request);

    expect(stateService.error()).toBe('Could not create the savings goal. Please try again.');
    expect(stateService.loading()).toBe(false);
    expect(stateService.goals()).toEqual([existingGoal]);
  });

  it('replaces the contributed goal with the updated version returned by the API', () => {
    seedGoals([existingGoal, otherGoal]);
    const request: AddContributionRequest = { amount: 100, currency: 'EUR' };
    apiServiceMock.addContribution.mockReturnValueOnce(of(updatedGoal));

    stateService.addContribution(existingGoal.id, request);

    expect(apiServiceMock.addContribution).toHaveBeenCalledWith(existingGoal.id, request);
    expect(stateService.goals()).toEqual([updatedGoal, otherGoal]);
    expect(stateService.loading()).toBe(false);
    expect(stateService.error()).toBeNull();
  });

  it('sets an error and keeps the goals unchanged when adding a contribution fails', () => {
    seedGoals([existingGoal, otherGoal]);
    const request: AddContributionRequest = { amount: 100, currency: 'EUR' };
    apiServiceMock.addContribution.mockReturnValueOnce(throwError(() => new Error('insufficient funds')));

    stateService.addContribution(existingGoal.id, request);

    expect(stateService.error()).toBe('Could not add the contribution. Please try again.');
    expect(stateService.loading()).toBe(false);
    expect(stateService.goals()).toEqual([existingGoal, otherGoal]);
  });

  it('passes an explicit user id through to the API service', () => {
    apiServiceMock.getSavingsGoals.mockReturnValue(of([]));

    stateService.loadGoals(42);

    expect(apiServiceMock.getSavingsGoals).toHaveBeenCalledWith(42);
    expect(stateService.goals()).toEqual([]);
  });
});