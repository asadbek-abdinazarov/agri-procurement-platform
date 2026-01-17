package com.agriprocurement.procurement.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProcurementRepository extends JpaRepository<Procurement, String> {

    List<Procurement> findByBuyerId(String buyerId);

    List<Procurement> findByStatus(Procurement.ProcurementStatus status);

    @Query("SELECT p FROM Procurement p WHERE p.status = :status AND p.deadline < :deadline")
    List<Procurement> findByStatusAndDeadlineBefore(
        @Param("status") Procurement.ProcurementStatus status,
        @Param("deadline") LocalDateTime deadline
    );

    @Query("SELECT p FROM Procurement p LEFT JOIN FETCH p.bids WHERE p.id = :id")
    Optional<Procurement> findByIdWithBids(@Param("id") String id);

    @Query("SELECT COUNT(b) FROM Procurement p JOIN p.bids b WHERE p.id = :procurementId AND b.status = 'SUBMITTED'")
    long countSubmittedBids(@Param("procurementId") String procurementId);

    @Query("SELECT p FROM Procurement p WHERE p.status IN ('PUBLISHED', 'BIDDING_OPEN') ORDER BY p.deadline ASC")
    List<Procurement> findActiveProcurements();
}
