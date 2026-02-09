package com.agrifood.common.security;

import java.util.Arrays;
import java.util.List;

/**
 * Simple access control mechanism
 * Demonstrates secure access without full authentication system
 */
public class AccessControl {
    
    /**
     * Check if user has required role
     * Ensures regulatory compliance through role-based permissions
     */
    public static boolean hasRole(List<Role> userRoles, Role... requiredRoles) {
        if (userRoles == null || userRoles.isEmpty()) {
            return false;
        }
        
        // Admin has access to everything
        if (userRoles.contains(Role.ADMIN)) {
            return true;
        }
        
        return userRoles.stream()
                .anyMatch(role -> Arrays.asList(requiredRoles).contains(role));
    }
    
    /**
     * Validate user has permission for operation
     */
    public static void requireRole(List<Role> userRoles, Role... requiredRoles) {
        if (!hasRole(userRoles, requiredRoles)) {
            throw new SecurityException("Access denied: insufficient permissions");
        }
    }
}
