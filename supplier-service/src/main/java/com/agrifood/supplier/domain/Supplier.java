package com.agrifood.supplier.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Supplier Aggregate Root - Agricultural Product Provider
 * 
 * DDD BOUNDED CONTEXT: Supplier Management
 * 
 * WHY SEPARATE DOMAIN?
 * - Ubiquitous Language: "Supplier", "Rating", "Capacity", "Certification"
 * - Business Logic: Supplier verification, rating calculation, capacity management
 * - Lifecycle: PENDING → ACTIVE → SUSPENDED → BLACKLISTED
 * - Autonomy: Supplier exists independently, participates in multiple contracts
 * 
 * DOMAIN INVARIANTS:
 * - Email must be unique
 * - Rating must be between 0-5
 * - Suspended suppliers cannot accept new contracts
 * - Rating only changes through completed contracts
 * 
 * EVENTUAL CONSISTENCY:
 * - rating updated via ContractCompletedEvent from contract service
 * - completedContracts incremented via events
 * 
 * @see Contract - Supplier participates in contracts
 */
@Entity
@Table(name = "suppliers")
public class Supplier {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    @Column(nullable = false)
    private String region;

    private String address;

    @Enumerated(EnumType.STRING)
    private SupplierStatus status;

    // Eventual consistency: rating updated via events from contract service
    private Double rating = 0.0;

    private Integer completedContracts = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public SupplierStatus getStatus() {
        return status;
    }

    public void setStatus(SupplierStatus status) {
        this.status = status;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Integer getCompletedContracts() {
        return completedContracts;
    }

    public void setCompletedContracts(Integer completedContracts) {
        this.completedContracts = completedContracts;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = SupplierStatus.ACTIVE;
        }
    }
}
