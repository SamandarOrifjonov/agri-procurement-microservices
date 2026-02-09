package com.agrifood.common.security;

/**
 * Role-based access control
 * Ensures secure access to system resources
 * Adaptable to regulatory requirements
 */
public enum Role {
    BUYER,      // Can create procurements
    SUPPLIER,   // Can submit bids and manage products
    ADMIN,      // Full system access
    AUDITOR     // Read-only access for compliance
}
