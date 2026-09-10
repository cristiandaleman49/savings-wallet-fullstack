import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SavingsGoal } from '../../../core/models/savings-goal.model';
import { SavingsGoalCard } from './savings-goal-card';

describe('SavingsGoalCard', () => {
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

  function renderCard(goal: SavingsGoal): ComponentFixture<SavingsGoalCard> {
    const fixture = TestBed.createComponent(SavingsGoalCard);
    fixture.componentRef.setInput('goal', goal);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [SavingsGoalCard],
    });
  });

  it('renders the data of an active goal', () => {
    const el = renderCard(activeGoal).nativeElement as HTMLElement;

    expect(el.querySelector('.goal-card__name')?.textContent).toContain('New Bike');
    expect(el.querySelector('.goal-card__accumulated')?.textContent).toContain('250.00');
    expect(el.querySelector('.goal-card__target')?.textContent).toContain('1,000.00');
    expect(el.querySelector('[data-status="ACTIVE"]')?.textContent).toContain('Activa');
    expect(el.querySelector('.goal-card__contribute')).toBeTruthy();
  });

  it('computes and renders the progress percentage', () => {
    const el = renderCard(activeGoal).nativeElement as HTMLElement;

    const progressbar = el.querySelector('[role="progressbar"]');
    expect(progressbar?.getAttribute('aria-valuenow')).toBe('25');
    expect(el.querySelector('.goal-card__progress-label')?.textContent).toContain('25%');
    expect(el.querySelector<HTMLElement>('.goal-card__progress-fill')?.style.width).toBe('25%');
  });

  it('caps the progress at 100% when accumulated exceeds the target', () => {
    const el = renderCard({ ...activeGoal, accumulatedAmount: 1250 }).nativeElement as HTMLElement;

    expect(el.querySelector('[role="progressbar"]')?.getAttribute('aria-valuenow')).toBe('100');
  });

  it('renders 0% progress when the target is not positive', () => {
    const el = renderCard({ ...activeGoal, targetAmount: 0, accumulatedAmount: 50 }).nativeElement as HTMLElement;

    expect(el.querySelector('[role="progressbar"]')?.getAttribute('aria-valuenow')).toBe('0');
  });

  it('gives completed goals a distinctive presentation without a contribute button', () => {
    const el = renderCard(completedGoal).nativeElement as HTMLElement;

    const card = el.querySelector('.goal-card');
    expect(card?.classList.contains('goal-card--completed')).toBe(true);
    expect(el.querySelector('[data-status="COMPLETED"]')?.textContent).toContain('Completada');
    expect(el.querySelector('.goal-card__contribute')).toBeNull();
  });

  it('emits the goal id when the contribute button is clicked', () => {
    const fixture = renderCard(activeGoal);
    const emittedGoalIds: number[] = [];
    fixture.componentInstance.contribute.subscribe((goalId) => emittedGoalIds.push(goalId));

    const button = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>(
      '.goal-card__contribute',
    );
    button?.click();

    expect(emittedGoalIds).toEqual([activeGoal.id]);
  });
});