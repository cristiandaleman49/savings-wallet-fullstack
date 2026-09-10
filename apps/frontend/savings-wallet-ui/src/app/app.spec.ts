import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { App } from './app';
import { routes } from './app.routes';

describe('App', () => {
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  /** Flushes the goals request triggered by the dashboard load. */
  function flushGoalsRequest(): void {
    const req = httpTesting.expectOne('/api/v1/savings-goals');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  }

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('renders the savings goals dashboard through the router at /savings-goals', async () => {
    const router = TestBed.inject(Router);
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    await router.navigate(['/savings-goals']);
    flushGoalsRequest();
    await fixture.whenStable();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(router.url).toBe('/savings-goals');
    expect(compiled.querySelector('app-savings-goals-dashboard .dashboard')).toBeTruthy();
    expect(compiled.querySelector('.dashboard__title')?.textContent).toContain('Metas de ahorro');
    expect(compiled.querySelector('.dashboard__header .button')?.textContent).toContain('Nueva meta');
  });

  it('redirects the empty path to /savings-goals and renders the dashboard', async () => {
    const router = TestBed.inject(Router);
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    await router.navigate(['/']);
    flushGoalsRequest();
    await fixture.whenStable();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(router.url).toBe('/savings-goals');
    expect(compiled.querySelector('app-savings-goals-dashboard .dashboard')).toBeTruthy();
  });

  it('does not render scaffold content anymore', async () => {
    const router = TestBed.inject(Router);
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    await router.navigate(['/savings-goals']);
    flushGoalsRequest();
    await fixture.whenStable();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).not.toContain('Hello, savings-wallet-ui');
  });
});
