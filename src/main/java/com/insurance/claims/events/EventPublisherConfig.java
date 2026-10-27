package com.insurance.claims.events;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Chooses the event publisher from configuration. Local by default; the HTTP
 * publisher only when messaging.enabled=true and an endpoint is provided.
 */
@Configuration
public class EventPublisherConfig {

    @Bean
    public EventPublisher eventPublisher(
            @Value("${messaging.enabled:false}") boolean enabled,
            @Value("${messaging.endpoint:}") String endpoint) {
        if (enabled && endpoint != null && !endpoint.isBlank()) {
            return new HttpEventPublisher(endpoint);
        }
        return new LocalEventPublisher();
    }
}
