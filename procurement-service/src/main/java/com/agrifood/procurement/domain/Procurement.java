package com.agrifood.procurement.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Procurement Aggregate Root - Buyer's Product Requirement
 * 
 * DDD BOUNDED CONTEXT: Procurement Management
 * 
 * WHY SEPARATE DOMAIN?
 * - Ubiquitous Language: "Procurement", "Tender", "Bid", "Deadline"
 * - Business Logic: Budget validation, deadline management, tender rules
 * - Lifecycle: DRAFT → PUBLISHED → BIDDING → AWARDED → CLOSED
 * - Autonomy: Procurement exists independently before contract creation
 * 
 * DOMAIN INVARIANTS:
 * - Budget must be positive
 * - Deadline must be in future
 * - Published procurement cannot be modified
 * - One procurement can only result in one contract
 * 
 * EVENTUAL CONSISTENCY:
 * - bidCount updated via events from supplier service
 * - Contract creation triggered via ProcurementCreatedEvent
 * 
 * @see Contract - Created after procurement is awarded
 * @see ProcurementCreatedEvent - Domain event for cross-service communication
 */
@Entity
@Table(name = "procurements")
public class Procurement {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String procurementNumber;

    @Column(nullable = false)
    private Long buyerId;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private String productCategory;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(nullable = false)
    private String unit;

    @Column(nullable = false)
    private BigDecimal budget;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcurementStatus status;

    @Column(nullable = false)
    private LocalDateTime deadline;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime publishedAt;

    // Eventual consistency: may be updated later via events
    private Integer bidCount = 0;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public ProcurementStatus getStatus() {
        return status;
    }

    public void setStatus(ProcurementStatus status) {
        this.status = status;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Integer getBidCount() {
        return bidCount;
    }

    public void setBidCount(Integer bidCount) {
        this.bidCount = bidCount;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = ProcurementStatus.DRAFT;
        }
    }
}
