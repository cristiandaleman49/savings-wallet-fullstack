/**
 * Server-to-client SSE payload emitted by the backend when a savings goal is
 * completed. Consumed by the dedicated SSE service and surfaced to the UI.
 */
export interface GoalCompletedEvent {
  goalId: number;
  goalName: string;
  targetAmount: number;
  /** ISO-8601 timestamp of completion, as serialized by the backend. */
  completedAt: string;
}