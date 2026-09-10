import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { SavingsGoal } from '../models/savings-goal.model';
import { AddContributionRequest, CreateSavingsGoalRequest } from '../models/savings-goal-request.model';
import { SavingsGoalsApiService } from './savings-goals-api.service';

describe('SavingsGoalsApiService', () => {
  let service: SavingsGoalsApiService;
  let httpTesting: HttpTestingController;

  const goal: SavingsGoal = {
    id: 1,
    userId: 1,
    name: 'New Bike',
    targetAmount: 1000,
    accumulatedAmount: 250,
    currency: 'EUR',
    status: 'ACTIVE',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(SavingsGoalsApiService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('fetches the savings goals of a user with the demo auth header', async () => {
    const goals$ = firstValueFrom(service.getSavingsGoals(1));

    const req = httpTesting.expectOne('/api/v1/savings-goals');
    expect(req.request.method).toBe('GET');
    expect(req.request.headers.get('X-User-Id')).toBe('1');
    req.flush([goal]);

    await expect(goals$).resolves.toEqual([goal]);
  });

  it('creates a savings goal with the provided payload', async () => {
    const request: CreateSavingsGoalRequest = {
      name: 'New Bike',
      targetAmount: 1000,
      currency: 'EUR',
    };
    const created: SavingsGoal = { ...goal, id: 2, accumulatedAmount: 0 };

    const created$ = firstValueFrom(service.createSavingsGoal(request));

    const req = httpTesting.expectOne('/api/v1/savings-goals');
    expect(req.request.method).toBe('POST');
    expect(req.request.headers.get('X-User-Id')).toBe('1');
    expect(req.request.body).toEqual(request);
    req.flush(created);

    await expect(created$).resolves.toEqual(created);
  });

  it('adds a contribution to a goal at the goal-specific endpoint', async () => {
    const request: AddContributionRequest = { amount: 100, currency: 'EUR' };

    const updated$ = firstValueFrom(service.addContribution(1, request));

    const req = httpTesting.expectOne('/api/v1/savings-goals/1/contributions');
    expect(req.request.method).toBe('POST');
    expect(req.request.headers.get('X-User-Id')).toBe('1');
    expect(req.request.body).toEqual(request);
    req.flush(goal);

    await expect(updated$).resolves.toEqual(goal);
  });
});