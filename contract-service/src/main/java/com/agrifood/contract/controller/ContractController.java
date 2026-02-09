package com.agrifood.contract.controller;

import com.agrifood.common.security.Role;
import com.agrifood.contract.domain.Contract;
import com.agrifood.contract.domain.ContractStatus;
import com.agrifood.contract.service.ContractService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Contract REST API
 * Demonstrates microservices architecture with saga pattern
 */
@RestController
@RequestMapping("/api/contracts")
public class ContractController {
    
    private final ContractService service;

    public ContractController(ContractService service) {
        this.service = service;
    }

    /**
     * Create contract with Saga pattern
     * Ensures reliability through automatic compensation
     */
    @PostMapping
    public ResponseEntity<Contract> create(
            @RequestBody Contract contract,
            @RequestHeader(value = "X-User-Roles", defaultValue = "BUYER") String rolesHeader) {
        
        List<Role> roles = parseRoles(rolesHeader);
        Contract created = service.createContract(contract, roles);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Sign contract - role-based access control
     */
    @PostMapping("/{id}/sign")
    public ResponseEntity<Contract> sign(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Roles", defaultValue = "BUYER") String rolesHeader) {
        
        List<Role> roles = parseRoles(rolesHeader);
        Contract signed = service.signContract(id, roles);
        return ResponseEntity.ok(signed);
    }

    /**
     * Async endpoint for concurrent processing
     */
    @GetMapping("/status/{status}/async")
    public CompletableFuture<ResponseEntity<List<Contract>>> findByStatusAsync(
            @PathVariable ContractStatus status) {
        
        return service.findByStatusAsync(status)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Contract> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping
    public ResponseEntity<List<Contract>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/buyer/{buyerId}")
    public ResponseEntity<List<Contract>> findByBuyer(@PathVariable Long buyerId) {
        return ResponseEntity.ok(service.findByBuyer(buyerId));
    }

    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<List<Contract>> findBySupplier(@PathVariable Long supplierId) {
        return ResponseEntity.ok(service.findBySupplier(supplierId));
    }

    private List<Role> parseRoles(String rolesHeader) {
        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .map(Role::valueOf)
                .toList();
    }
}
