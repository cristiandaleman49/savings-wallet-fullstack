package com.example.savingswallet.domain.event;

import java.time.Instant;

/**
 * Marker for domain events raised by aggregates. Events are transport
 * independent: application and infrastructure layers decide how they are
 * published (in-process, SSE, etc.).
 */
public interface DomainEvent {

    Instant occurredAt();
}