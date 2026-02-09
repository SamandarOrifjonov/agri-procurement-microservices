package com.agrifood.supplier.domain;

/**
 * Supplier status - configurable for regulatory requirements
 */
public enum SupplierStatus {
    ACTIVE,         // Can participate in procurements
    SUSPENDED,      // Temporarily blocked
    BLACKLISTED,    // Permanently blocked
    PENDING_REVIEW  // Under evaluation
}
