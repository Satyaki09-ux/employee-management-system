package com.app.repository;

import com.app.model.Sale;
import com.app.model.Employee;
import com.app.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {
    Optional<Sale> findBySaleId(String saleId);

    List<Sale> findByCreatedById(Long createdById);
    List<Sale> findByCustomer(Customer customer);
    List<Sale> findBySaleDate(LocalDate saleDate);
    List<Sale> findBySaleDateBetween(LocalDate startDate, LocalDate endDate);
    List<Sale> findBySaleStatus(String saleStatus);
    List<Sale> findByPaymentStatus(String paymentStatus);
    List<Sale> findByCreatedByIdAndSaleDateBetween(Long createdById, LocalDate startDate, LocalDate endDate);

    // Hierarchy-based finder methods
    List<Sale> findByPromoterId(Long promoterId);
    List<Sale> findByZonalHeadId(Long zonalHeadId);
    List<Sale> findByClusterHeadId(Long clusterHeadId);
    List<Sale> findByAsmId(Long asmId);
}

