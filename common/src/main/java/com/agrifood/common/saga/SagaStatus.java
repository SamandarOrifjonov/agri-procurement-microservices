package com.agrifood.common.saga;

/**
 * Saga pattern status tracking
 * Ensures reliability in distributed transactions
 */
public enum SagaStatus {
    STARTED,        // Saga initiated
    IN_PROGRESS,    // Steps being executed
    COMPLETED,      // All steps successful
    COMPENSATING,   // Rolling back due to failure
    COMPENSATED,    // Rollback completed
    FAILED          // Saga failed permanently
}
