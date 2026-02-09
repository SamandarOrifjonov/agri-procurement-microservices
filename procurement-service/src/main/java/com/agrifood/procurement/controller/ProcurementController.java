package com.agrifood.procurement.controller;

import com.agrifood.common.security.Role;
import com.agrifood.procurement.domain.Procurement;
import com.agrifood.procurement.domain.ProcurementStatus;
import com.agrifood.procurement.service.ProcurementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Procurement REST API
 * Demonstrates microservices architecture with independent service
 */
@RestController
@RequestMapping("/api/procurements")
public class ProcurementController {
    
    private final ProcurementService service;

    public ProcurementController(ProcurementService service) {
        this.service = service;
    }

    /**
     * Create new procurement requirement
     * Role-based access control applied
     */
    @PostMapping
    public ResponseEntity<Procurement> create(
            @RequestBody Procurement procurement,
            @RequestHeader(value = "X-User-Roles", defaultValue = "BUYER") String rolesHeader) {
        
        List<Role> roles = parseRoles(rolesHeader);
        Procurement created = service.createProcurement(procurement, roles);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Publish procurement (make visible to suppliers)
     */
    @PostMapping("/{id}/publish")
    public ResponseEntity<Procurement> publish(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Roles", defaultValue = "BUYER") String rolesHeader) {
        
        List<Role> roles = parseRoles(rolesHeader);
        Procurement published = service.publishProcurement(id, roles);
        return ResponseEntity.ok(published);
    }

    /**
     * Async endpoint demonstrating concurrent processing capability
     */
    @GetMapping("/status/{status}/async")
    public CompletableFuture<ResponseEntity<List<Procurement>>> findByStatusAsync(
            @PathVariable ProcurementStatus status) {
        
        return service.findByStatusAsync(status)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Procurement> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping
    public ResponseEntity<List<Procurement>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/buyer/{buyerId}")
    public ResponseEntity<List<Procurement>> findByBuyer(@PathVariable Long buyerId) {
        return ResponseEntity.ok(service.findByBuyer(buyerId));
    }

    // Helper method to parse roles from header
    private List<Role> parseRoles(String rolesHeader) {
        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .map(Role::valueOf)
                .toList();
    }
}
