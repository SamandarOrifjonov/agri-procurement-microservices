package com.agrifood.common.event;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Simple in-memory event publisher for demonstration
 * Enables loose coupling between services through async event processing
 * In production: replace with Kafka/RabbitMQ
 */
@Component
public class EventPublisher {
    
    // In-memory event store (for demo purposes)
    private final List<Object> eventStore = new ArrayList<>();
    
    /**
     * Asynchronous event publishing
     * Supports high concurrency and multi-user scenarios
     */
    @Async
    public CompletableFuture<Void> publishAsync(Object event) {
        return CompletableFuture.runAsync(() -> {
            eventStore.add(event);
            System.out.println("📢 Event published asynchronously: " + event.getClass().getSimpleName());
        });
    }
    
    /**
     * Synchronous event publishing (for critical operations)
     */
    public void publish(Object event) {
        eventStore.add(event);
        System.out.println("📢 Event published: " + event.getClass().getSimpleName());
    }
    
    public List<Object> getEvents() {
        return new ArrayList<>(eventStore);
    }
    
    public void clearEvents() {
        eventStore.clear();
    }
}
