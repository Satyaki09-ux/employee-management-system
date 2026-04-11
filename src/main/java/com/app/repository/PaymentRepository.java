package com.app.repository;

import com.app.model.Payment;
import com.app.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findBySale(Sale sale);
    List<Payment> findBySaleId(Long saleId);
    Payment findByInvoiceNumber(String invoiceNumber);
    
    /**
     * Find payment by transaction ID (for duplicate validation)
     * Only checks non-null and non-empty transaction IDs
     */
    Payment findByTransactionId(String transactionId);
    
    @Query("SELECT p FROM Payment p JOIN p.sale s WHERE s.createdById = :employeeId")
    List<Payment> findBySaleCreatedById(@Param("employeeId") Long employeeId);

    @Query("SELECT p FROM Payment p JOIN p.sale s WHERE s.createdById IN :employeeIds")
    List<Payment> findBySaleCreatedByIdIn(@Param("employeeIds") List<Long> employeeIds);
}
