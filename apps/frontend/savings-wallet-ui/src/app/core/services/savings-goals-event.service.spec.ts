import { TestBed } from '@angular/core/testing';
import { GoalCompletedEvent } from '../models/goal-completed-event.model';
import { SavingsGoalsEventService } from './savings-goals-event.service';

/**
 * Deterministic in-memory EventSource stand-in. Records the registered
 * `goal-completed` listener so tests can emit payloads synchronously.
 */
class FakeEventSource {
  static readonly CONNECTING = 0;
  static readonly OPEN = 1;
  static readonly CLOSED = 2;

  readyState = FakeEventSource.CONNECTING;
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

  removeEventListener(type: string, _listener: EventListener): void {
    this.listeners.delete(type);
  }

  listenerCount(type: string): number {
    return this.listeners.get(type)?.length ?? 0;
  }

  close(): void {
    this.readyState = FakeEventSource.CLOSED;
  }

  emit(type: string, data: string): void {
    const event = { data } as unknown as MessageEvent<string>;
    for (const listener of this.listeners.get(type) ?? []) {
      listener(event);
    }
  }

  emitError(): void {
    this.onerror?.({} as unknown as Event);
  }

  static readonly instances: FakeEventSource[] = [];
  static reset(): void {
    FakeEventSource.instances.length = 0;
  }
}

describe('SavingsGoalsEventService', () => {
  let service: SavingsGoalsEventService;

  const validPayload: GoalCompletedEvent = {
    goalId: 1,
    goalName: 'Bicicleta nueva',
    targetAmount: 1000,
    completedAt: '2026-09-10T10:00:00Z',
  };

  beforeAll(() => {
    vi.stubGlobal('EventSource', FakeEventSource);
  });

  afterAll(() => {
    vi.unstubAllGlobals();
    FakeEventSource.reset();
  });

  beforeEach(() => {
    FakeEventSource.reset();
    TestBed.configureTestingModule({
      providers: [SavingsGoalsEventService],
    });
    service = TestBed.inject(SavingsGoalsEventService);
  });

  function lastInstance(): FakeEventSource {
    return FakeEventSource.instances[FakeEventSource.instances.length - 1];
  }

  it('connect() creates an EventSource with the correct URL and demo userId', () => {
    service.connect();

    expect(FakeEventSource.instances).toHaveLength(1);
    expect(lastInstance().url).toBe('/api/v1/savings-goals/events?userId=1');
  });

  it('connect() uses the provided userId', () => {
    service.connect(42);

    expect(lastInstance().url).toBe('/api/v1/savings-goals/events?userId=42');
  });

  it('connect() registers a listener for the `goal-completed` event', () => {
    service.connect();

    expect(lastInstance().listenerCount('goal-completed')).toBe(1);
  });

  it('parses and emits the goal-completed payload through goalCompleted$', () => {
    const received: GoalCompletedEvent[] = [];
    service.goalCompleted$.subscribe((event) => received.push(event));

    service.connect();
    lastInstance().emit('goal-completed', JSON.stringify(validPayload));

    expect(received).toEqual([validPayload]);
  });

  it('disconnect() removes the listener so late events are not emitted', () => {
    const received: GoalCompletedEvent[] = [];
    service.goalCompleted$.subscribe((event) => received.push(event));

    service.connect();
    const instance = lastInstance();
    service.disconnect();

    instance.emit('goal-completed', JSON.stringify(validPayload));

    expect(received).toEqual([]);
  });

  it('disconnect() closes the EventSource', () => {
    service.connect();
    const instance = lastInstance();

    service.disconnect();

    expect(instance.readyState).toBe(FakeEventSource.CLOSED);
  });

  it('repeated connect() does not create duplicate connections', () => {
    service.connect();
    service.connect();

    expect(FakeEventSource.instances).toHaveLength(1);
  });

  it('handles an EventSource error without breaking the stream', () => {
    const received: GoalCompletedEvent[] = [];
    service.goalCompleted$.subscribe((event) => received.push(event));

    service.connect();
    lastInstance().emitError();

    expect(() => lastInstance().emit('goal-completed', JSON.stringify(validPayload))).not.toThrow();
    expect(received).toEqual([validPayload]);
  });

  it('can reconnect after disconnect()', () => {
    const received: GoalCompletedEvent[] = [];
    service.goalCompleted$.subscribe((event) => received.push(event));

    service.connect();
    service.disconnect();
    expect(FakeEventSource.instances).toHaveLength(1);

    service.connect();

    expect(FakeEventSource.instances).toHaveLength(2);
    const newInstance = lastInstance();
    expect(newInstance.url).toBe('/api/v1/savings-goals/events?userId=1');
    newInstance.emit('goal-completed', JSON.stringify(validPayload));

    expect(received).toEqual([validPayload]);
  });

  it('ignores malformed JSON payloads without breaking the stream', () => {
    const received: GoalCompletedEvent[] = [];
    service.goalCompleted$.subscribe((event) => received.push(event));

    service.connect();
    lastInstance().emit('goal-completed', '{not valid json');
    lastInstance().emit('goal-completed', JSON.stringify(validPayload));

    expect(received).toEqual([validPayload]);
  });
});