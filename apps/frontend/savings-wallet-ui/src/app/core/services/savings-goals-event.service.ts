import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { DEMO_USER_ID } from './savings-goals-api.service';
import { GoalCompletedEvent } from '../models/goal-completed-event.model';

const EVENTS_ENDPOINT = '/api/v1/savings-goals/events';
const GOAL_COMPLETED_EVENT_TYPE = 'goal-completed';

/**
 * Dedicated client for the savings-goals Server-Sent Events (SSE) stream.
 *
 * Responsibility: open an SSE connection, listen for `goal-completed` events,
 * parse them type-safe and expose them reactively. It contains no business
 * logic and does not decide what happens when a goal completes.
 *
 * REST commands stay in SavingsGoalsApiService; application state stays in
 * SavingsGoalsStateService. This service is purely server-to-client realtime.
 */
@Injectable({
  providedIn: 'root',
})
export class SavingsGoalsEventService {
  private eventSource: EventSource | null = null;
  private readonly goalCompletedSubject = new Subject<GoalCompletedEvent>();

  /** Stream of `goal-completed` events received from the backend. */
  readonly goalCompleted$: Observable<GoalCompletedEvent> = this.goalCompletedSubject.asObservable();

  /**
   * Opens the SSE connection for the given user. Reuses the existing
   * connection if one is already open or connecting, avoiding duplicates.
   */
  connect(userId: number = DEMO_USER_ID): void {
    if (this.eventSource && this.eventSource.readyState !== EventSource.CLOSED) {
      return;
    }

    const eventSource = new EventSource(`${EVENTS_ENDPOINT}?userId=${userId}`);

    eventSource.addEventListener(GOAL_COMPLETED_EVENT_TYPE, this.handleGoalCompleted);
    eventSource.onerror = () => {
      // EventSource reconnects natively; just log. Do not break the stream.
      console.error('[SavingsGoalsEventService] SSE connection error');
    };

    this.eventSource = eventSource;
  }

  /**
   * Closes the SSE connection and releases the reference so the service can
   * reconnect later. The observable stays alive for future subscriptions.
   */
  disconnect(): void {
    if (this.eventSource) {
      this.eventSource.removeEventListener(GOAL_COMPLETED_EVENT_TYPE, this.handleGoalCompleted);
      this.eventSource.close();
      this.eventSource = null;
    }
  }

  private handleGoalCompleted = (event: Event): void => {
    const payload = parseGoalCompletedEvent((event as MessageEvent<string>).data);
    if (payload === null) {
      // Ignore malformed payloads; never break the stream or throw.
      console.error('[SavingsGoalsEventService] malformed goal-completed payload');
      return;
    }
    this.goalCompletedSubject.next(payload);
  };
}

/**
 * Parses and validates the raw SSE `data` field into a GoalCompletedEvent.
 * Returns null when the payload is not valid JSON or lacks the expected shape.
 */
function parseGoalCompletedEvent(data: unknown): GoalCompletedEvent | null {
  if (typeof data !== 'string') {
    return null;
  }
  let parsed: unknown;
  try {
    // JSON.parse returns `any` by definition; it is captured here as
    // `unknown` so every use below is narrowed before access.
    parsed = JSON.parse(data) as unknown;
  } catch {
    return null;
  }
  if (typeof parsed !== 'object' || parsed === null) {
    return null;
  }
  const candidate = parsed as Record<string, unknown>;
  const goalId = candidate['goalId'];
  const goalName = candidate['goalName'];
  const targetAmount = candidate['targetAmount'];
  const completedAt = candidate['completedAt'];
  if (
    typeof goalId !== 'number' ||
    typeof goalName !== 'string' ||
    typeof targetAmount !== 'number' ||
    typeof completedAt !== 'string'
  ) {
    return null;
  }
  return { goalId, goalName, targetAmount, completedAt };
}