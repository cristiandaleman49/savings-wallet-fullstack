import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'savings-goals' },
  {
    path: 'savings-goals',
    loadComponent: () =>
      import('./features/savings-goals/pages/savings-goals-dashboard-page').then(
        (m) => m.SavingsGoalsDashboardPage,
      ),
  },
];
