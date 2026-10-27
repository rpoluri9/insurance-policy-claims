package com.insurance.claims.events;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

/**
 * Optional external publisher: POSTs each event as JSON to a configured HTTP
 * endpoint (a webhook, a messaging bridge, or a cloud pub/sub relay). Only
 * wired in when messaging.enabled=true; failures are logged, never fatal to
 * the business operation that produced the event.
 */
public class HttpEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(HttpEventPublisher.class);

    private final String endpoint;
    private final RestTemplate restTemplate;

    public HttpEventPublisher(String endpoint) {
        this.endpoint = endpoint;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public void publish(DomainEvent event) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> payload = Map.of(
                    "eventId", event.getEventId(),
                    "type", event.getType(),
                    "entityType", event.getEntityType(),
                    "entityId", event.getEntityId(),
                    "timestamp", event.getTimestamp().toString(),
                    "details", event.getDetails());
            restTemplate.postForObject(endpoint, new HttpEntity<>(payload, headers), String.class);
            log.info("EVENT (http publisher): type={} sent to {}", event.getType(), endpoint);
        } catch (Exception ex) {
            log.warn("EVENT delivery failed for type={}: {}", event.getType(), ex.getMessage());
        }
    }
}
