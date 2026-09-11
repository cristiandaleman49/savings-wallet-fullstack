import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { GoalCompletedEvent } from './core/models/goal-completed-event.model';
import { App } from './app';
import { routes } from './app.routes';

/**
 * Minimal EventSource stand-in for the jsdom environment, which provides no
 * native EventSource. It records `goal-completed` listeners so App-level tests
 * can emit SSE payloads deterministically.
 */
class FakeEventSource {
  static readonly CONNECTING = 0;
  static readonly OPEN = 1;
  static readonly CLOSED = 2;

  static readonly instances: FakeEventSource[] = [];

  readonly readyState = FakeEventSource.CONNECTING;
  onerror: ((event: Event) => void) | null = null;
  private readonly listeners = new Map<string, EventListener[]>();

  constructor(public readonly url: string) {
    FakeEventSource.instances.push(this);
  }

  addEventListener(type: string, listener: EventListener): void {
    const current = this.listeners.get(type) ?? [];
    current.push(listener);
    this.listeners.set(type, current);
  }

  removeEventListener(type: string): void {
    this.listeners.delete(type);
  }

  close(): void {}

  emit(type: string, data: string): void {
    const event = { data } as unknown as MessageEvent<string>;
    for (const listener of this.listeners.get(type) ?? []) {
      listener(event);
    }
  }
}

describe('App', () => {
  let httpTesting: HttpTestingController;

  beforeAll(() => {
    vi.stubGlobal('EventSource', FakeEventSource);
  });

  afterAll(() => {
    vi.unstubAllGlobals();
  });

  beforeEach(async () => {
    FakeEventSource.instances.length = 0;
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

  it('opens exactly one shared SSE connection for the app lifetime', () => {
    TestBed.createComponent(App);

    expect(FakeEventSource.instances).toHaveLength(1);
    expect(FakeEventSource.instances[0].url).toBe('/api/v1/savings-goals/events?userId=1');
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

  // Celebration dialog driven by the App-owned SSE stream: an event arriving
  // while the contribution page is active must still open the dialog.
  const completedEvent: GoalCompletedEvent = {
    goalId: 1,
    goalName: 'Bicicleta nueva',
    targetAmount: 1000,
    completedAt: '2026-09-10T10:00:00Z',
  };

  it('shows the completion dialog when a goal-completed event arrives', async () => {
    const router = TestBed.inject(Router);
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    await router.navigate(['/savings-goals']);
    flushGoalsRequest();
    await fixture.whenStable();

    FakeEventSource.instances[0].emit('goal-completed', JSON.stringify(completedEvent));
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const dialog = compiled.querySelector('app-goal-completed-dialog');
    expect(dialog).toBeTruthy();
    expect(dialog?.textContent).toContain('¡Meta completada!');
    expect(dialog?.textContent).toContain('Bicicleta nueva');
    expect(dialog?.textContent).toContain('1,000');
  });

  it('closes the completion dialog when Continuar is clicked', async () => {
    const router = TestBed.inject(Router);
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    await router.navigate(['/savings-goals']);
    flushGoalsRequest();
    await fixture.whenStable();

    FakeEventSource.instances[0].emit('goal-completed', JSON.stringify(completedEvent));
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    compiled.querySelector<HTMLButtonElement>('app-goal-completed-dialog .button')?.click();
    fixture.detectChanges();

    expect(compiled.querySelector('app-goal-completed-dialog')).toBeNull();
  });
});
