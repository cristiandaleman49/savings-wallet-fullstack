import { HttpClient, HttpHeaders } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { SavingsGoal } from '../models/savings-goal.model';
import { AddContributionRequest, CreateSavingsGoalRequest } from '../models/savings-goal-request.model';

const API_BASE_URL = '/api/v1/savings-goals';

/**
 * Demo authentication for the MVP. The header name and demo user id are
 * centralized here so the whole flow can be replaced by real authentication
 * (e.g. an HTTP interceptor backed by a token/auth service) without touching
 * individual API methods.
 */
const AUTH_USER_HEADER = 'X-User-Id';
const DEMO_USER_ID = 1;

/**
 * Encapsulates all HTTP communication for the savings-goals REST API.
 *
 * Pure transport layer: no UI state, no business rules, no derived calculations.
 * Type-safety of responses is a compile-time contract only; the backend remains
 * the source of truth for validation.
 */
@Injectable({
  providedIn: 'root',
})
export class SavingsGoalsApiService {
  private readonly http = inject(HttpClient);

  /** Returns all savings goals owned by the given user. */
  getSavingsGoals(userId: number): Observable<SavingsGoal[]> {
    return this.http.get<SavingsGoal[]>(API_BASE_URL, {
      headers: this.buildHeaders(userId),
    });
  }

  /** Creates a new savings goal and returns the persisted entity. */
  createSavingsGoal(request: CreateSavingsGoalRequest): Observable<SavingsGoal> {
    return this.http.post<SavingsGoal>(API_BASE_URL, request, {
      headers: this.buildHeaders(),
    });
  }

  /** Adds a contribution to a goal and returns the updated entity. */
  addContribution(goalId: number, request: AddContributionRequest): Observable<SavingsGoal> {
    return this.http.post<SavingsGoal>(`${API_BASE_URL}/${goalId}/contributions`, request, {
      headers: this.buildHeaders(),
    });
  }

  /**
   * Builds the request headers identifying the acting user. For the MVP every
   * request defaults to the demo user id; this is the single place to update when
   * real authentication replaces the demo header.
   */
  private buildHeaders(userId: number = DEMO_USER_ID): HttpHeaders {
    return new HttpHeaders({ [AUTH_USER_HEADER]: String(userId) });
  }
}