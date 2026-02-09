# Saga Failure Scenarios - Complete Flow

## 🎯 Saga Pattern Overview

Saga = Distributed Transaction = Sequence of Local Transactions + Compensation

```
Success: Step1 → Step2 → Step3 → Step4 ✅
Failure: Step1 → Step2 → ❌ → Compensate(Step2) → Compensate(Step1)
```

---

## 🔴 Failure Scenarios

### Scenario 1: Validation Failure (Early Failure)
```
Timeline:
1. ✅ Start saga
2. ❌ Validate contract (amount = 0)
3. 🔙 No compensation needed
4. ❌ Saga failed

Result: No side effects, clean failure
```

**Code Flow:**
```java
// ContractCreationSaga.java
public boolean createContract(Contract contract) {
    String sagaId = UUID.randomUUID().toString();
    contract.setSagaId(sagaId);
    
    List<SagaStep> steps = Arrays.asList(
        new ValidateContractStep(contract),  // ❌ Fails here
        new ReserveSupplierCapacityStep(contract),
        new CreateContractRecordStep(contract, contractRepository),
        new NotifyPartiesStep(contract)
    );
    
    return orchestrator.executeSaga(steps);  // Returns false
}

// SagaOrchestrator.java
public boolean executeSaga(List<SagaStep> steps) {
    status = SagaStatus.IN_PROGRESS;
    executedSteps.clear();
    
    for (SagaStep step : steps) {
        boolean success = step.execute();
        
        if (!success) {
            System.out.println("❌ Step failed: " + step.getStepName());
            compensate();  // No steps executed yet, nothing to compensate
            return false;
        }
        
        executedSteps.add(step);
    }
    
    return true;
}
```

**Output:**
```
🔄 Executing saga step: Validate Contract
❌ Step failed: Validate Contract
🔙 Starting compensation...
✅ Compensation completed
```

---

### Scenario 2: Supplier Capacity Failure (Mid-Saga)
```
Timeline:
1. ✅ Validate contract
2. ❌ Reserve supplier capacity (supplier busy)
3. 🔙 Compensate: Validate contract (no-op)
4. ❌ Saga failed

Result: No database changes, clean rollback
```

**Code Flow:**
```java
// ReserveSupplierCapacityStep.java
@Override
public boolean execute() {
    // Check supplier capacity (simulated)
    boolean hasCapacity = checkSupplierCapacity(contract.getSupplierId());
    
    if (!hasCapacity) {
        System.out.println("❌ Supplier has no capacity");
        return false;  // ❌ Failure
    }
    
    reserved = true;
    return true;
}

// Compensation triggered automatically
@Override
public void compensate() {
    if (reserved) {
        System.out.println("🔙 Releasing supplier capacity");
        releaseSupplierCapacity(contract.getSupplierId());
        reserved = false;
    }
}
```

**Output:**
```
🔄 Executing saga step: Validate Contract
✅ Step completed: Validate Contract
🔄 Executing saga step: Reserve Supplier Capacity
❌ Supplier has no capacity
❌ Step failed: Reserve Supplier Capacity
🔙 Starting compensation...
🔙 Compensating: Validate Contract
🔙 Validation step - no compensation needed
✅ Compensation completed
```

---

### Scenario 3: Database Failure (Critical Failure)
```
Timeline:
1. ✅ Validate contract
2. ✅ Reserve supplier capacity
3. ❌ Create contract record (DB connection lost)
4. 🔙 Compensate: Release supplier capacity
5. 🔙 Compensate: Validate (no-op)
6. ❌ Saga failed

Result: Supplier capacity released, no orphan data
```

**Code Flow:**
```java
// CreateContractRecordStep.java
@Override
public boolean execute() {
    try {
        Contract saved = repository.save(contract);  // ❌ Throws exception
        createdContractId = saved.getId();
        return true;
    } catch (DataAccessException e) {
        System.out.println("❌ Database error: " + e.getMessage());
        return false;
    }
}

@Override
public void compensate() {
    if (createdContractId != null) {
        System.out.println("🔙 Deleting contract record: " + createdContractId);
        repository.deleteById(createdContractId);
    }
}
```

**Output:**
```
🔄 Executing saga step: Validate Contract
✅ Step completed: Validate Contract
🔄 Executing saga step: Reserve Supplier Capacity
📦 Reserving supplier capacity for: 123
✅ Step completed: Reserve Supplier Capacity
🔄 Executing saga step: Create Contract Record
❌ Database error: Connection timeout
❌ Step failed: Create Contract Record
🔙 Starting compensation...
🔙 Compensating: Reserve Supplier Capacity
🔙 Releasing supplier capacity for: 123
🔙 Compensating: Validate Contract
🔙 Validation step - no compensation needed
✅ Compensation completed
```

---

### Scenario 4: Notification Failure (Late Failure)
```
Timeline:
1. ✅ Validate contract
2. ✅ Reserve supplier capacity
3. ✅ Create contract record (ID: 456)
4. ❌ Notify parties (email service down)
5. 🔙 Compensate: Send cancellation emails
6. 🔙 Compensate: Delete contract record (ID: 456)
7. 🔙 Compensate: Release supplier capacity
8. 🔙 Compensate: Validate (no-op)
9. ❌ Saga failed

Result: Contract deleted, capacity released, consistent state
```

**Code Flow:**
```java
// NotifyPartiesStep.java
@Override
public boolean execute() {
    try {
        emailService.sendContractCreatedEmail(contract.getBuyerId());
        emailService.sendContractCreatedEmail(contract.getSupplierId());
        notified = true;
        return true;
    } catch (EmailServiceException e) {
        System.out.println("❌ Email service unavailable");
        return false;  // ❌ Failure
    }
}

@Override
public void compensate() {
    if (notified) {
        System.out.println("🔙 Sending cancellation notifications");
        emailService.sendContractCancelledEmail(contract.getBuyerId());
        emailService.sendContractCancelledEmail(contract.getSupplierId());
    }
}
```

**Output:**
```
🔄 Executing saga step: Validate Contract
✅ Step completed: Validate Contract
🔄 Executing saga step: Reserve Supplier Capacity
📦 Reserving supplier capacity for: 123
✅ Step completed: Reserve Supplier Capacity
🔄 Executing saga step: Create Contract Record
💾 Contract record created: CNT-2025-001
✅ Step completed: Create Contract Record
🔄 Executing saga step: Notify Parties
❌ Email service unavailable
❌ Step failed: Notify Parties
🔙 Starting compensation...
🔙 Compensating: Notify Parties
🔙 Sending cancellation notifications
🔙 Compensating: Create Contract Record
🔙 Deleting contract record: 456
🔙 Compensating: Reserve Supplier Capacity
🔙 Releasing supplier capacity for: 123
🔙 Compensating: Validate Contract
🔙 Validation step - no compensation needed
✅ Compensation completed
```

---

## 🔥 Exception During Compensation

### Scenario 5: Compensation Failure (Worst Case)
```
Timeline:
1. ✅ Validate contract
2. ✅ Reserve supplier capacity
3. ✅ Create contract record (ID: 789)
4. ❌ Notify parties
5. 🔙 Compensate: Notify (success)
6. 🔙 Compensate: Delete contract ❌ (DB down)
7. ⚠️ Continue compensation despite error
8. 🔙 Compensate: Release capacity (success)
9. ⚠️ Saga compensated with errors

Result: Partial compensation, requires manual intervention
```

**Code Flow:**
```java
// SagaOrchestrator.java
private void compensate() {
    status = SagaStatus.COMPENSATING;
    System.out.println("🔙 Starting compensation...");
    
    for (int i = executedSteps.size() - 1; i >= 0; i--) {
        SagaStep step = executedSteps.get(i);
        try {
            System.out.println("🔙 Compensating: " + step.getStepName());
            step.compensate();
        } catch (Exception e) {
            // ⚠️ Log but continue compensating other steps
            System.out.println("⚠️ Compensation failed for: " + step.getStepName());
            System.out.println("⚠️ Error: " + e.getMessage());
            
            // Alert operations team
            alertOps("Compensation failed", step.getStepName(), e);
            
            // Continue with other compensations
        }
    }
    
    status = SagaStatus.COMPENSATED;
    System.out.println("✅ Compensation completed");
}
```

**Output:**
```
🔄 Executing saga step: Validate Contract
✅ Step completed: Validate Contract
🔄 Executing saga step: Reserve Supplier Capacity
✅ Step completed: Reserve Supplier Capacity
🔄 Executing saga step: Create Contract Record
✅ Step completed: Create Contract Record
🔄 Executing saga step: Notify Parties
❌ Step failed: Notify Parties
🔙 Starting compensation...
🔙 Compensating: Notify Parties
🔙 Sending cancellation notifications
🔙 Compensating: Create Contract Record
⚠️ Compensation failed for: Create Contract Record
⚠️ Error: Database connection timeout
🚨 ALERT: Manual intervention required for contract ID: 789
🔙 Compensating: Reserve Supplier Capacity
🔙 Releasing supplier capacity for: 123
🔙 Compensating: Validate Contract
✅ Compensation completed
```

---

## 📊 Saga State Transitions

```
┌──────────┐
│ STARTED  │
└────┬─────┘
     │
     ▼
┌──────────────┐     Success     ┌───────────┐
│ IN_PROGRESS  │────────────────▶│ COMPLETED │
└──────┬───────┘                 └───────────┘
       │
       │ Failure
       ▼
┌──────────────┐                 ┌──────────────┐
│ COMPENSATING │────────────────▶│ COMPENSATED  │
└──────────────┘                 └──────────────┘
       │
       │ Compensation Error
       ▼
┌──────────────┐
│ FAILED       │ ⚠️ Requires manual intervention
└──────────────┘
```

---

## 🛠️ Enhanced Saga Implementation

### Add Retry Logic
```java
public class RetryableSagaStep implements SagaStep {
    private final SagaStep delegate;
    private final int maxRetries;
    
    @Override
    public boolean execute() {
        int attempts = 0;
        while (attempts < maxRetries) {
            try {
                return delegate.execute();
            } catch (TransientException e) {
                attempts++;
                System.out.println("🔄 Retry attempt " + attempts);
                Thread.sleep(1000 * attempts);  // Exponential backoff
            }
        }
        return false;
    }
}
```

### Add Idempotency
```java
public class IdempotentCreateContractStep implements SagaStep {
    
    @Override
    public boolean execute() {
        // Check if already executed
        if (repository.existsBySagaId(contract.getSagaId())) {
            System.out.println("⚠️ Contract already created, skipping");
            return true;  // Idempotent
        }
        
        // Create contract
        Contract saved = repository.save(contract);
        return true;
    }
}
```

### Add Timeout
```java
public class TimeoutSagaStep implements SagaStep {
    private final SagaStep delegate;
    private final Duration timeout;
    
    @Override
    public boolean execute() {
        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(
            () -> delegate.execute()
        );
        
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            System.out.println("⏱️ Step timeout: " + delegate.getStepName());
            return false;
        }
    }
}
```

---

## 📈 Monitoring & Alerting

### Metrics to Track
```java
// Saga execution metrics
sagaCounter.increment("started");
sagaCounter.increment("completed");
sagaCounter.increment("failed");
sagaCounter.increment("compensated");

// Step-level metrics
stepTimer.record("ValidateContract", duration);
stepFailureCounter.increment("ReserveCapacity");

// Compensation metrics
compensationCounter.increment("success");
compensationCounter.increment("partial_failure");  // ⚠️ Alert!
```

### Alerts
```
🚨 CRITICAL: Saga compensation failed
   - Saga ID: abc-123
   - Failed Step: Create Contract Record
   - Contract ID: 789
   - Action: Manual cleanup required

🚨 WARNING: High saga failure rate
   - Service: contract-service
   - Failure Rate: 15% (threshold: 5%)
   - Action: Investigate root cause
```

---

## 🎯 Best Practices

### 1. Design Compensatable Operations
```java
// ✅ Good - can be compensated
createRecord() → deleteRecord()
reserveCapacity() → releaseCapacity()
sendEmail() → sendCancellationEmail()

// ❌ Bad - cannot be compensated
sendSMS() → ??? (SMS already sent!)
printDocument() → ??? (paper already printed!)
```

### 2. Make Steps Idempotent
```java
// ✅ Good - safe to retry
if (!exists(sagaId)) {
    create(sagaId);
}

// ❌ Bad - creates duplicates
create(sagaId);
```

### 3. Order Steps by Risk
```
Low Risk First:
1. Validate (no side effects)
2. Reserve (easy to compensate)
3. Create record (harder to compensate)
4. Notify (hardest to compensate)
```

### 4. Log Everything
```java
log.info("Saga started: {}", sagaId);
log.info("Step executing: {}", stepName);
log.error("Step failed: {}, reason: {}", stepName, error);
log.warn("Compensation started: {}", sagaId);
log.error("Compensation failed: {}, manual intervention required", stepName);
```

---

## 🎓 Summary

| Scenario | Steps Executed | Compensation | Result |
|----------|----------------|--------------|--------|
| Validation Failure | 0 | None | Clean failure |
| Capacity Failure | 1 | 1 step | Clean rollback |
| Database Failure | 2 | 2 steps | Clean rollback |
| Notification Failure | 3 | 3 steps | Clean rollback |
| Compensation Failure | 3 | Partial | Manual intervention |

**Key Takeaway**: Saga pattern ensures eventual consistency even when distributed operations fail!
