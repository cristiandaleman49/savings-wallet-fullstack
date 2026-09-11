import { ComponentFixture, TestBed } from '@angular/core/testing';
import { GoalCompletedEvent } from '../../../core/models/goal-completed-event.model';
import { GoalCompletedDialog } from './goal-completed-dialog';

describe('GoalCompletedDialog', () => {
  const completed: GoalCompletedEvent = {
    goalId: 1,
    goalName: 'Awakening',
    targetAmount: 1000000,
    completedAt: '2026-09-10T10:00:00Z',
  };

  function render(event: GoalCompletedEvent = completed): ComponentFixture<GoalCompletedDialog> {
    const fixture = TestBed.createComponent(GoalCompletedDialog);
    fixture.componentRef.setInput('completedEvent', event);
    fixture.detectChanges();
    return fixture;
  }

  it('renders an accessible dialog with the goal name and target amount', () => {
    const el = render().nativeElement as HTMLElement;
    const dialog = el.querySelector('[role="dialog"]');

    expect(dialog).toBeTruthy();
    expect(dialog?.getAttribute('aria-modal')).toBe('true');
    expect(el.textContent).toContain('¡Meta completada!');
    expect(el.textContent).toContain('Awakening');
    expect(el.textContent).toContain('1,000,000');
  });

  it('emits closed when Continuar is clicked', () => {
    const fixture = render();
    let closedCount = 0;
    fixture.componentInstance.closed.subscribe(() => closedCount++);

    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('.button')?.click();

    expect(closedCount).toBe(1);
  });
});
