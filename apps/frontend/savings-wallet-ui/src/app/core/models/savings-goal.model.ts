/**
 * Status lifecycle of a savings goal. The backend aggregate is the source of
 * truth for transitions (e.g. ACTIVE -> COMPLETED); the frontend only mirrors it.
 */
export type SavingsGoalStatus = 'ACTIVE' | 'COMPLETED';

/**
 * Persistent savings-goal entity as returned by the REST API.
 *
 * Money fields map the backend BigDecimal to JSON `number`. This model is a pure
 * API contract: it contains no derived state (e.g. `progress` is deliberately
 * absent because it is derived as accumulatedAmount / targetAmount by the UI) and
 * performs no monetary arithmetic.
 */
export interface SavingsGoal {
  id: number;
  userId: number;
  name: string;
  targetAmount: number;
  accumulatedAmount: number;
  currency: string;
  status: SavingsGoalStatus;
}