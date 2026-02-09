package com.agrifood.contract.domain;

/**
 * Contract lifecycle status
 * Configurable for regulatory compliance
 */
public enum ContractStatus {
    DRAFT,          // Initial creation
    PENDING,        // Awaiting signatures
    SIGNED,         // Both parties signed
    ACTIVE,         // In execution
    COMPLETED,      // Successfully fulfilled
    TERMINATED,     // Ended early
    DISPUTED        // Under dispute resolution
}
