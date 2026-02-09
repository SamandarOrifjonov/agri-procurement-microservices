package com.agrifood.procurement.repository;

import com.agrifood.procurement.domain.Procurement;
import com.agrifood.procurement.domain.ProcurementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * PostgreSQL repository for procurement data
 */
@Repository
public interface ProcurementRepository extends JpaRepository<Procurement, Long> {
    Optional<Procurement> findByProcurementNumber(String procurementNumber);
    List<Procurement> findByBuyerId(Long buyerId);
    List<Procurement> findByStatus(ProcurementStatus status);
    List<Procurement> findByProductCategory(String category);
}
