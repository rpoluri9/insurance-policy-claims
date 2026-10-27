package com.insurance.claims.events;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A domain event: something meaningful happened to a policy or a claim
 * (created, status changed, settled). Downstream systems (notifications,
 * analytics) consume these instead of polling the database.
 */
public class DomainEvent {

    private final String eventId;
    private final String type;
    private final String entityType;
    private final String entityId;
    private final LocalDateTime timestamp;
    private final String details;

    public DomainEvent(String type, String entityType, String entityId, String details) {
        this.eventId = UUID.randomUUID().toString();
        this.type = type;
        this.entityType = entityType;
        this.entityId = entityId;
        this.timestamp = LocalDateTime.now();
        this.details = details;
    }

    public String getEventId() {
        return eventId;
    }

    public String getType() {
        return type;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getDetails() {
        return details;
    }
}
