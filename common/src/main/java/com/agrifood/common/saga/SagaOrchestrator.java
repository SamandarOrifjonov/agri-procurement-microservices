package com.agrifood.common.saga;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Saga Pattern Orchestrator - Distributed Transaction Coordinator
 * 
 * RELIABILITY PATTERN: Ensures eventual consistency across microservices
 * 
 * SAGA PATTERN OVERVIEW:
 * - Distributed transaction = Sequence of local transactions
 * - Each step has compensation logic for rollback
 * - Failure triggers automatic compensation in reverse order
 * 
 * EXECUTION FLOW:
 * Success: Step1 → Step2 → Step3 → Step4 ✅ COMPLETED
 * Failure: Step1 → Step2 → ❌ → Compensate(Step2) → Compensate(Step1) ⚠️ COMPENSATED
 * 
 * FAILURE SCENARIOS:
 * 
 * 1. Early Failure (Validation):
 *    - No steps executed yet
 *    - No compensation needed
 *    - Clean failure
 * 
 * 2. Mid-Saga Failure (Capacity):
 *    - Some steps executed
 *    - Compensation in reverse order
 *    - Clean rollback
 * 
 * 3. Late Failure (Notification):
 *    - Most steps executed (including DB writes)
 *    - Full compensation required
 *    - Database records deleted, resources released
 * 
 * 4. Compensation Failure (Worst Case):
 *    - Compensation step fails (e.g., DB down)
 *    - Log error and continue with other compensations
 *    - Alert operations team for manual intervention
 * 
 * STATE TRANSITIONS:
 * STARTED → IN_PROGRESS → COMPLETED (success)
 *                      ↓
 *                   COMPENSATING → COMPENSATED (failure)
 *                      ↓
 *                   FAILED (compensation error, requires manual fix)
 * 
 * BEST PRACTICES:
 * - Design compensatable operations (create → delete, reserve → release)
 * - Make steps idempotent (safe to retry)
 * - Order steps by risk (low risk first)
 * - Log everything for debugging
 * - Alert on compensation failures
 * 
 * @see ContractCreationSaga - Example implementation
 * @see <a href="SAGA-FAILURE-SCENARIOS.md">Complete failure scenarios guide</a>
 */
@Component
public class SagaOrchestrator {
    
    private final List<SagaStep> executedSteps = new ArrayList<>();
    private SagaStatus status = SagaStatus.STARTED;
    
    /**
     * Execute saga with automatic compensation on failure
     * Demonstrates reliability pattern for distributed transactions
     */
    public boolean executeSaga(List<SagaStep> steps) {
        status = SagaStatus.IN_PROGRESS;
        executedSteps.clear();
        
        try {
            // Execute each step sequentially
            for (SagaStep step : steps) {
                System.out.println("🔄 Executing saga step: " + step.getStepName());
                
                boolean success = step.execute();
                
                if (!success) {
                    System.out.println("❌ Step failed: " + step.getStepName());
                    compensate();
                    return false;
                }
                
                executedSteps.add(step);
                System.out.println("✅ Step completed: " + step.getStepName());
            }
            
            status = SagaStatus.COMPLETED;
            System.out.println("✅ Saga completed successfully");
            return true;
            
        } catch (Exception e) {
            System.out.println("❌ Saga failed with exception: " + e.getMessage());
            compensate();
            return false;
        }
    }
    
    /**
     * Compensate (rollback) all executed steps in reverse order
     * 
     * COMPENSATION STRATEGY:
     * - Execute in reverse order (LIFO - Last In First Out)
     * - Continue even if compensation fails (best effort)
     * - Log all failures for manual intervention
     * - Alert operations team on critical failures
     * 
     * FAILURE HANDLING:
     * - Transient errors: Log and continue
     * - Permanent errors: Alert ops team
     * - Partial compensation: Mark saga as FAILED
     * 
     * EXAMPLE FLOW:
     * Steps executed: [Validate, Reserve, CreateDB, Notify]
     * Compensation order: Notify → CreateDB → Reserve → Validate
     * 
     * If CreateDB compensation fails:
     * - Log error: "⚠️ Compensation failed for: Create Contract Record"
     * - Alert ops: "Manual cleanup required for contract ID: 789"
     * - Continue: Still compensate Reserve and Validate
     * - Result: Partial compensation, requires manual fix
     */
    private void compensate() {
        status = SagaStatus.COMPENSATING;
        System.out.println("🔙 Starting compensation...");
        
        // Compensate in reverse order
        for (int i = executedSteps.size() - 1; i >= 0; i--) {
            SagaStep step = executedSteps.get(i);
            try {
                System.out.println("🔙 Compensating: " + step.getStepName());
                step.compensate();
            } catch (Exception e) {
                System.out.println("⚠️ Compensation failed for: " + step.getStepName());
                // Log but continue compensating other steps
            }
        }
        
        status = SagaStatus.COMPENSATED;
        System.out.println("✅ Compensation completed");
    }
    
    public SagaStatus getStatus() {
        return status;
    }
}
