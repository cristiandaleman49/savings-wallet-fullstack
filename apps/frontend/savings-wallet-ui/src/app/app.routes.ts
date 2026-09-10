import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'savings-goals' },
  {
    path: 'savings-goals/new',
    loadComponent: () =>
      import('./features/savings-goals/pages/savings-goals-create-page').then(
        (m) => m.SavingsGoalsCreatePage,
      ),
  },
  {
    path: 'savings-goals/:goalId/contribute',
    loadComponent: () =>
      import('./features/savings-goals/pages/savings-goal-contribute-page').then(
        (m) => m.SavingsGoalContributePage,
      ),
  },
  {
    path: 'savings-goals',
    loadComponent: () =>
      import('./features/savings-goals/pages/savings-goals-dashboard-page').then(
        (m) => m.SavingsGoalsDashboardPage,
      ),
  },
];
