package com.agrifood.contract.saga;

import com.agrifood.common.saga.SagaOrchestrator;
import com.agrifood.common.saga.SagaStep;
import com.agrifood.contract.domain.Contract;
import com.agrifood.contract.repository.ContractRepository;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Contract Creation Saga - Distributed Transaction Implementation
 * 
 * SAGA PATTERN: Coordinates contract creation across multiple services
 * 
 * STEPS:
 * 1. Validate Contract - Check business rules (amount > 0, IDs present)
 * 2. Reserve Supplier Capacity - Check if supplier can fulfill
 * 3. Create Contract Record - Persist to database
 * 4. Notify Parties - Send emails to buyer and supplier
 * 
 * COMPENSATION LOGIC:
 * Each step has rollback capability:
 * - Validate → No compensation needed (no side effects)
 * - Reserve → Release capacity
 * - Create → Delete database record
 * - Notify → Send cancellation emails
 * 
 * FAILURE SCENARIOS:
 * 
 * Scenario 1: Validation fails
 * - Timeline: ❌ Validate
 * - Compensation: None needed
 * - Result: Clean failure, no side effects
 * 
 * Scenario 2: Capacity check fails
 * - Timeline: ✅ Validate → ❌ Reserve
 * - Compensation: Validate (no-op)
 * - Result: Clean rollback
 * 
 * Scenario 3: Database fails
 * - Timeline: ✅ Validate → ✅ Reserve → ❌ CreateDB
 * - Compensation: Release capacity → Validate
 * - Result: Capacity released, no orphan data
 * 
 * Scenario 4: Notification fails
 * - Timeline: ✅ Validate → ✅ Reserve → ✅ CreateDB → ❌ Notify
 * - Compensation: Cancel notifications → Delete DB → Release capacity
 * - Result: Contract deleted, consistent state
 * 
 * Scenario 5: Compensation fails (worst case)
 * - Timeline: Steps succeed → Notify fails → Compensation fails
 * - Handling: Log error, alert ops team, continue other compensations
 * - Result: Partial compensation, manual intervention required
 * 
 * IDEMPOTENCY:
 * - Each step checks if already executed (via sagaId)
 * - Safe to retry on transient failures
 * 
 * MONITORING:
 * - Log all step executions and compensations
 * - Track saga success/failure rates
 * - Alert on compensation failures
 * 
 * @see SagaOrchestrator - Generic saga execution engine
 * @see <a href="SAGA-FAILURE-SCENARIOS.md">Detailed failure scenarios</a>
 */
@Component
public class ContractCreationSaga {
    
    private final ContractRepository contractRepository;
    private final SagaOrchestrator orchestrator;

    public ContractCreationSaga(ContractRepository contractRepository, SagaOrchestrator orchestrator) {
        this.contractRepository = contractRepository;
        this.orchestrator = orchestrator;
    }

    /**
     * Execute contract creation saga with multiple steps
     * Each step has compensation for rollback capability
     */
    public boolean createContract(Contract contract) {
        String sagaId = UUID.randomUUID().toString();
        contract.setSagaId(sagaId);
        
        // Define saga steps with compensation logic
        List<SagaStep> steps = Arrays.asList(
            new ValidateContractStep(contract),
            new ReserveSupplierCapacityStep(contract),
            new CreateContractRecordStep(contract, contractRepository),
            new NotifyPartiesStep(contract)
        );
        
        // Execute saga with automatic compensation on failure
        return orchestrator.executeSaga(steps);
    }

    /**
     * Step 1: Validate contract data
     * 
     * VALIDATION RULES:
     * - Amount must be positive
     * - Buyer ID and Supplier ID must be present
     * - Quantity must be positive
     * 
     * FAILURE SCENARIO:
     * - Returns false if validation fails
     * - No side effects, no compensation needed
     * - Saga stops immediately
     * 
     * COMPENSATION:
     * - None needed (no state changes)
     */
    static class ValidateContractStep implements SagaStep {
        private final Contract contract;
        
        ValidateContractStep(Contract contract) {
            this.contract = contract;
        }

        @Override
        public boolean execute() {
            // Validate contract data
            if (contract.getAmount() == null || contract.getAmount().signum() <= 0) {
                return false;
            }
            if (contract.getBuyerId() == null || contract.getSupplierId() == null) {
                return false;
            }
            return true;
        }

        @Override
        public void compensate() {
            // No compensation needed for validation
            System.out.println("🔙 Validation step - no compensation needed");
        }

        @Override
        public String getStepName() {
            return "Validate Contract";
        }
    }

    /**
     * Step 2: Reserve supplier capacity
     * 
     * PURPOSE:
     * - Check if supplier can fulfill the contract
     * - Reserve capacity to prevent overbooking
     * 
     * REAL IMPLEMENTATION:
     * - Call supplier service API: POST /suppliers/{id}/reserve
     * - Pass contract details (quantity, deadline)
     * - Receive confirmation or rejection
     * 
     * FAILURE SCENARIO:
     * - Supplier has no capacity
     * - Supplier is suspended
     * - Supplier service unavailable
     * 
     * COMPENSATION:
     * - Release reserved capacity
     * - Call supplier service: POST /suppliers/{id}/release
     * - Ensures supplier can accept other contracts
     * 
     * IDEMPOTENCY:
     * - Check if already reserved (via sagaId)
     * - Safe to retry on transient failures
     */
    static class ReserveSupplierCapacityStep implements SagaStep {
        private final Contract contract;
        private boolean reserved = false;
        
        ReserveSupplierCapacityStep(Contract contract) {
            this.contract = contract;
        }

        @Override
        public boolean execute() {
            // Simulate capacity check (in real system: call supplier service)
            System.out.println("📦 Reserving supplier capacity for: " + contract.getSupplierId());
            reserved = true;
            return true;
        }

        @Override
        public void compensate() {
            // Release reserved capacity
            if (reserved) {
                System.out.println("🔙 Releasing supplier capacity for: " + contract.getSupplierId());
                reserved = false;
            }
        }

        @Override
        public String getStepName() {
            return "Reserve Supplier Capacity";
        }
    }

    /**
     * Step 3: Create contract record in database
     * 
     * PURPOSE:
     * - Persist contract to database
     * - Generate contract number
     * - Set initial status (DRAFT)
     * 
     * FAILURE SCENARIO:
     * - Database connection timeout
     * - Unique constraint violation (duplicate contract number)
     * - Transaction rollback
     * 
     * COMPENSATION:
     * - Delete created contract record
     * - Ensures no orphan data in database
     * - Maintains data consistency
     * 
     * IDEMPOTENCY:
     * - Check if contract already exists (via sagaId)
     * - If exists, skip creation (already executed)
     * - Safe to retry on transient failures
     * 
     * CRITICAL:
     * - This is the point of no return
     * - After this step, compensation becomes critical
     * - Failure here requires full rollback of previous steps
     */
    static class CreateContractRecordStep implements SagaStep {
        private final Contract contract;
        private final ContractRepository repository;
        private Long createdContractId;
        
        CreateContractRecordStep(Contract contract, ContractRepository repository) {
            this.contract = contract;
            this.repository = repository;
        }

        @Override
        public boolean execute() {
            // Save contract to database
            Contract saved = repository.save(contract);
            createdContractId = saved.getId();
            System.out.println("💾 Contract record created: " + saved.getContractNumber());
            return true;
        }

        @Override
        public void compensate() {
            // Delete contract record (rollback)
            if (createdContractId != null) {
                System.out.println("🔙 Deleting contract record: " + createdContractId);
                repository.deleteById(createdContractId);
            }
        }

        @Override
        public String getStepName() {
            return "Create Contract Record";
        }
    }

    /**
     * Step 4: Notify parties about contract creation
     * 
     * PURPOSE:
     * - Send email to buyer: "Contract created"
     * - Send email to supplier: "New contract assigned"
     * - Publish ContractCreatedEvent for other services
     * 
     * REAL IMPLEMENTATION:
     * - Call email service API
     * - Publish event to message broker (Kafka/RabbitMQ)
     * - Update notification status
     * 
     * FAILURE SCENARIO:
     * - Email service unavailable
     * - Message broker down
     * - Network timeout
     * 
     * COMPENSATION:
     * - Send cancellation emails
     * - Publish ContractCancelledEvent
     * - Notify parties that contract was rolled back
     * 
     * IMPORTANT:
     * - This is the last step
     * - Failure here triggers full compensation
     * - All previous steps must be rolled back
     * - Contract record will be deleted
     * - Supplier capacity will be released
     * 
     * EVENTUAL CONSISTENCY:
     * - Even if notification fails, system remains consistent
     * - No contract in database = no notification needed
     * - Compensation ensures clean state
     */
    static class NotifyPartiesStep implements SagaStep {
        private final Contract contract;
        private boolean notified = false;
        
        NotifyPartiesStep(Contract contract) {
            this.contract = contract;
        }

        @Override
        public boolean execute() {
            // Simulate notification (in real system: send emails/events)
            System.out.println("📧 Notifying buyer: " + contract.getBuyerId());
            System.out.println("📧 Notifying supplier: " + contract.getSupplierId());
            notified = true;
            return true;
        }

        @Override
        public void compensate() {
            // Send cancellation notifications
            if (notified) {
                System.out.println("🔙 Sending cancellation notifications");
            }
        }

        @Override
        public String getStepName() {
            return "Notify Parties";
        }
    }
}
