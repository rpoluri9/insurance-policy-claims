package com.insurance.claims.events;

/**
 * Publishes domain events to downstream consumers. The interface is the
 * contract; which implementation is wired in is a configuration choice, so
 * the services never care whether events go to a log, an in-memory list,
 * or a real messaging system.
 */
public interface EventPublisher {
    void publish(DomainEvent event);
}
