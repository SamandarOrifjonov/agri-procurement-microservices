package com.agrifood.common.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Domain Event: Procurement Created
 * 
 * EVENT VERSIONING STRATEGY:
 * 
 * Current Version: v1
 * 
 * CHANGELOG:
 * - v1 (2025-01): Initial version with basic procurement fields
 * 
 * FUTURE VERSIONS (Planned):
 * - v2 (2026-03): Add organicCertified field (new regulation requirement)
 * - v3 (2027-06): Add multi-currency support (currency field)
 * 
 * BACKWARD COMPATIBILITY:
 * - New fields must be optional (nullable) to support v1 consumers
 * - Old consumers ignore unknown fields (forward compatible)
 * - New consumers handle old events via default values (backward compatible)
 * 
 * VERSIONING PATTERN:
 * 1. Version field identifies event schema
 * 2. Consumers check version and handle accordingly
 * 3. Upcasting converts old events to new format
 * 4. Default values ensure backward compatibility
 * 
 * EXAMPLE MIGRATION (v1 → v2):
 * <pre>
 * // v1 event (old)
 * {
 *   "version": "v1",
 *   "procurementId": 123,
 *   "budget": 10000
 * }
 * 
 * // v2 event (new)
 * {
 *   "version": "v2",
 *   "procurementId": 123,
 *   "budget": 10000,
 *   "organicCertified": false,  // new field with default
 *   "currency": "UZS"            // new field with default
 * }
 * 
 * // Consumer handles both:
 * if ("v1".equals(event.getVersion())) {
 *     // Use defaults for missing fields
 *     boolean organic = false;
 *     String currency = "UZS";
 * }
 * </pre>
 * 
 * @see <a href="EVENT-VERSIONING-STRATEGY.md">Complete versioning guide</a>
 */
public class ProcurementCreatedEvent {
    
    /**
     * Event schema version for backward compatibility
     * 
     * CRITICAL: Never remove this field!
     * Consumers rely on version to handle different schemas
     */
    private final String version = "v1";
    
    private Long procurementId;
    private String procurementNumber;
    private Long buyerId;
    private String productCategory;
    private BigDecimal quantity;
    private BigDecimal budget;
    private LocalDateTime deadline;
    private LocalDateTime eventTimestamp;
    
    public ProcurementCreatedEvent() {
        this.eventTimestamp = LocalDateTime.now();
    }

    public ProcurementCreatedEvent(Long procurementId, String procurementNumber, Long buyerId, 
                                   String productCategory, BigDecimal quantity, BigDecimal budget, 
                                   LocalDateTime deadline) {
        this.procurementId = procurementId;
        this.procurementNumber = procurementNumber;
        this.buyerId = buyerId;
        this.productCategory = productCategory;
        this.quantity = quantity;
        this.budget = budget;
        this.deadline = deadline;
        this.eventTimestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getVersion() {
        return version;
    }

    public Long getProcurementId() {
        return procurementId;
    }

    public void setProcurementId(Long procurementId) {
        this.procurementId = procurementId;
    }

    public String getProcurementNumber() {
        return procurementNumber;
    }

    public void setProcurementNumber(String procurementNumber) {
        this.procurementNumber = procurementNumber;
    }

    public Long getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(Long buyerId) {
        this.buyerId = buyerId;
    }

    public String getProductCategory() {
        return productCategory;
    }

    public void setProductCategory(String productCategory) {
        this.productCategory = productCategory;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public LocalDateTime getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(LocalDateTime eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }
}
