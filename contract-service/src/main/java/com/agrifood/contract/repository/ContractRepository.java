package com.agrifood.contract.repository;

import com.agrifood.contract.domain.Contract;
import com.agrifood.contract.domain.ContractStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * PostgreSQL repository for contract data
 */
@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {
    Optional<Contract> findByContractNumber(String contractNumber);
    List<Contract> findByBuyerId(Long buyerId);
    List<Contract> findBySupplierId(Long supplierId);
    List<Contract> findByStatus(ContractStatus status);
    Optional<Contract> findBySagaId(String sagaId);
}
