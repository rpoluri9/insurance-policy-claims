package com.insurance.claims.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default publisher: events are logged and kept in memory. This is what runs
 * out of the box — no broker, no cloud account, no credentials needed.
 * A notification service would subscribe to these events in a real setup.
 */
public class LocalEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LocalEventPublisher.class);

    private final List<DomainEvent> events = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void publish(DomainEvent event) {
        events.add(event);
        log.info("EVENT (local publisher): type={} entity={}:{} details={}",
                event.getType(), event.getEntityType(), event.getEntityId(), event.getDetails());
    }

    public List<DomainEvent> getEvents() {
        return List.copyOf(events);
    }
}
