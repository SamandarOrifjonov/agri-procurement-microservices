package com.agrifood.contract.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Contract Aggregate Root - Agreement Between Buyer and Supplier
 * 
 * DDD BOUNDED CONTEXT: Contract Management
 * 
 * WHY SEPARATE DOMAIN?
 * - Ubiquitous Language: "Contract", "Agreement", "Delivery", "Payment"
 * - Business Logic: Contract terms, delivery tracking, payment milestones
 * - Lifecycle: DRAFT → SIGNED → IN_PROGRESS → COMPLETED → CANCELLED
 * - Autonomy: Contract orchestrates relationship between Procurement and Supplier
 * 
 * DOMAIN INVARIANTS:
 * - One procurement can only have one active contract
 * - Amount and quantity must match procurement requirements
 * - Signed contracts cannot be deleted
 * - Delivery tracking only available in IN_PROGRESS state
 * 
 * SAGA PATTERN:
 * - sagaId tracks distributed transaction
 * - sagaStatus enables compensation on failure
 * - Ensures eventual consistency across services
 * 
 * EVENTUAL CONSISTENCY:
 * - deliveryStatus updated via events from delivery service
 * - Triggers rating updates in supplier service on completion
 * 
 * @see Procurement - Source of contract requirements
 * @see Supplier - Contract participant
 * @see ContractCreationSaga - Distributed transaction orchestration
 */
@Entity
@Table(name = "contracts")
public class Contract {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String contractNumber;

    @Column(nullable = false)
    private Long procurementId;

    @Column(nullable = false)
    private Long buyerId;

    @Column(nullable = false)
    private Long supplierId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContractStatus status;

    // Saga tracking for reliability
    @Column(nullable = false)
    private String sagaId;

    @Enumerated(EnumType.STRING)
    private com.agrifood.common.saga.SagaStatus sagaStatus;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime signedAt;

    private LocalDateTime completedAt;

    // Eventual consistency: may be updated later
    private String deliveryStatus;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContractNumber() {
        return contractNumber;
    }

    public void setContractNumber(String contractNumber) {
        this.contractNumber = contractNumber;
    }

    public Long getProcurementId() {
        return procurementId;
    }

    public void setProcurementId(Long procurementId) {
        this.procurementId = procurementId;
    }

    public Long getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(Long buyerId) {
        this.buyerId = buyerId;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public ContractStatus getStatus() {
        return status;
    }

    public void setStatus(ContractStatus status) {
        this.status = status;
    }

    public String getSagaId() {
        return sagaId;
    }

    public void setSagaId(String sagaId) {
        this.sagaId = sagaId;
    }

    public com.agrifood.common.saga.SagaStatus getSagaStatus() {
        return sagaStatus;
    }

    public void setSagaStatus(com.agrifood.common.saga.SagaStatus sagaStatus) {
        this.sagaStatus = sagaStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getSignedAt() {
        return signedAt;
    }

    public void setSignedAt(LocalDateTime signedAt) {
        this.signedAt = signedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(String deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = ContractStatus.DRAFT;
        }
    }
}
