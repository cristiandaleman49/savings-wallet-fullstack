/**
 * Request payloads for the savings-goal REST API.
 *
 * Money amounts follow the backend BigDecimal JSON serialization (plain number).
 * Backend validation remains the source of truth; these contracts only mirror the
 * wire format.
 */
export interface CreateSavingsGoalRequest {
  name: string;
  targetAmount: number;
  currency: string;
}

export interface AddContributionRequest {
  amount: number;
  currency: string;
}