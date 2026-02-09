package com.agrifood.supplier.repository;

import com.agrifood.supplier.domain.Supplier;
import com.agrifood.supplier.domain.SupplierStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * PostgreSQL repository for supplier data
 */
@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    Optional<Supplier> findByEmail(String email);
    List<Supplier> findByStatus(SupplierStatus status);
    List<Supplier> findByRegion(String region);
}
