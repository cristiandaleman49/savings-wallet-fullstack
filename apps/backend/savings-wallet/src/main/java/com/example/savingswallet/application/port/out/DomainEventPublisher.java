package com.example.savingswallet.application.port.out;

import com.example.savingswallet.domain.event.DomainEvent;

/**
 * Outbound port for publishing domain events.
 *
 * <p>The application and domain depend on this abstraction only; concrete
 * implementations (e.g. an in-process SSE broadcaster) live in the
 * adapters/infrastructure layer.
 */
public interface DomainEventPublisher {

    void publish(DomainEvent event);
}