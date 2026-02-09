package com.agrifood.procurement.domain;

/**
 * Procurement lifecycle status
 * Configurable enum for regulatory adaptation
 */
public enum ProcurementStatus {
    DRAFT,          // Initial state
    PUBLISHED,      // Open for bids
    EVALUATION,     // Reviewing bids
    AWARDED,        // Winner selected
    CONTRACTED,     // Contract signed
    COMPLETED,      // Fulfilled
    CANCELLED       // Terminated
}
