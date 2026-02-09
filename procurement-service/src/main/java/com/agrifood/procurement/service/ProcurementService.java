package com.agrifood.procurement.service;

import com.agrifood.common.event.EventPublisher;
import com.agrifood.common.event.ProcurementCreatedEvent;
import com.agrifood.common.exception.ResourceNotFoundException;
import com.agrifood.common.security.AccessControl;
import com.agrifood.common.security.Role;
import com.agrifood.procurement.domain.Procurement;
import com.agrifood.procurement.domain.ProcurementStatus;
import com.agrifood.procurement.repository.ProcurementRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Procurement business logic
 * Demonstrates async processing and event-driven architecture
 */
@Service
public class ProcurementService {
    
    private final ProcurementRepository repository;
    private final EventPublisher eventPublisher;

    public ProcurementService(ProcurementRepository repository, EventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Create procurement with role-based access control
     * Publishes event for loose coupling with other services
     */
    @Transactional
    public Procurement createProcurement(Procurement procurement, List<Role> userRoles) {
        // Secure access: only buyers can create procurements
        AccessControl.requireRole(userRoles, Role.BUYER, Role.ADMIN);
        
        // Generate unique procurement number (adaptable format)
        procurement.setProcurementNumber("PROC-" + System.currentTimeMillis() + "-" + 
                                        UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        
        Procurement saved = repository.save(procurement);
        
        // Event-driven communication: notify other services asynchronously
        // Enables scalability through loose coupling
        ProcurementCreatedEvent event = new ProcurementCreatedEvent(
            saved.getId(),
            saved.getProcurementNumber(),
            saved.getBuyerId(),
            saved.getProductCategory(),
            saved.getQuantity(),
            saved.getBudget(),
            saved.getDeadline()
        );
        
        eventPublisher.publishAsync(event);
        
        return saved;
    }

    /**
     * Async method for concurrent processing
     * Supports multi-user scalability
     */
    @Async
    public CompletableFuture<List<Procurement>> findByStatusAsync(ProcurementStatus status) {
        return CompletableFuture.supplyAsync(() -> repository.findByStatus(status));
    }

    /**
     * Publish procurement - makes it visible to suppliers
     * Eventual consistency: bid count updated via events
     */
    @Transactional
    public Procurement publishProcurement(Long id, List<Role> userRoles) {
        AccessControl.requireRole(userRoles, Role.BUYER, Role.ADMIN);
        
        Procurement procurement = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Procurement not found"));
        
        procurement.setStatus(ProcurementStatus.PUBLISHED);
        procurement.setPublishedAt(LocalDateTime.now());
        
        return repository.save(procurement);
    }

    public Procurement findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Procurement not found"));
    }

    public List<Procurement> findAll() {
        return repository.findAll();
    }

    public List<Procurement> findByBuyer(Long buyerId) {
        return repository.findByBuyerId(buyerId);
    }
}
