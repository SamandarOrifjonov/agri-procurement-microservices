package com.agrifood.common.saga;

/**
 * Saga pattern step definition
 * Each step has compensation logic for reliability
 */
public interface SagaStep {
    
    /**
     * Execute the step
     * Returns true if successful
     */
    boolean execute();
    
    /**
     * Compensate (rollback) the step
     * Called when saga fails to maintain consistency
     */
    void compensate();
    
    /**
     * Step name for logging and tracking
     */
    String getStepName();
}
