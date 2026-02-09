package com.agrifood.contract.service;

import com.agrifood.common.exception.ResourceNotFoundException;
import com.agrifood.common.security.AccessControl;
import com.agrifood.common.security.Role;
import com.agrifood.contract.domain.Contract;
import com.agrifood.contract.domain.ContractStatus;
import com.agrifood.contract.repository.ContractRepository;
import com.agrifood.contract.saga.ContractCreationSaga;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Contract Service with Saga pattern implementation
 * Demonstrates reliability and eventual consistency
 */
@Service
public class ContractService {
    
    private final ContractRepository repository;
    private final ContractCreationSaga saga;

    public ContractService(ContractRepository repository, ContractCreationSaga saga) {
        this.repository = repository;
        this.saga = saga;
    }

    /**
     * Create contract using Saga pattern
     * Ensures reliability through coordinated compensation
     */
    @Transactional
    public Contract createContract(Contract contract, List<Role> userRoles) {
        // Secure access control
        AccessControl.requireRole(userRoles, Role.BUYER, Role.ADMIN);
        
        // Generate contract number (configurable format)
        contract.setContractNumber("CNT-" + System.currentTimeMillis() + "-" + 
                                   UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        
        // Execute saga with automatic compensation on failure
        boolean success = saga.createContract(contract);
        
        if (!success) {
            throw new RuntimeException("Contract creation failed - saga compensated");
        }
        
        return repository.findBySagaId(contract.getSagaId())
                .orElseThrow(() -> new RuntimeException("Contract not found after saga"));
    }

    /**
     * Async method for concurrent processing
     * Supports multi-user scalability
     */
    @Async
    public CompletableFuture<List<Contract>> findByStatusAsync(ContractStatus status) {
        return CompletableFuture.supplyAsync(() -> repository.findByStatus(status));
    }

    /**
     * Sign contract - eventual consistency
     * Status may be updated later via events
     */
    @Transactional
    public Contract signContract(Long id, List<Role> userRoles) {
        AccessControl.requireRole(userRoles, Role.BUYER, Role.SUPPLIER, Role.ADMIN);
        
        Contract contract = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found"));
        
        contract.setStatus(ContractStatus.SIGNED);
        contract.setSignedAt(LocalDateTime.now());
        
        return repository.save(contract);
    }

    public Contract findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found"));
    }

    public List<Contract> findAll() {
        return repository.findAll();
    }

    public List<Contract> findByBuyer(Long buyerId) {
        return repository.findByBuyerId(buyerId);
    }

    public List<Contract> findBySupplier(Long supplierId) {
        return repository.findBySupplierId(supplierId);
    }
}
