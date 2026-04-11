package com.app.repository;

import com.app.model.Commission;
import com.app.model.Employee;
import com.app.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface CommissionRepository extends JpaRepository<Commission, Long> {

    List<Commission> findByEmployee(Employee employee);

    List<Commission> findByEmployeeAndStatus(Employee employee, String status);

    List<Commission> findBySale(Sale sale);

    @Query("SELECT c FROM Commission c WHERE c.employee.reportingManager = :manager AND c.status = 'PENDING_APPROVAL'")
    List<Commission> findPendingCommissionsForManager(@Param("manager") Employee manager);

    @Query("SELECT SUM(c.amount) FROM Commission c WHERE c.employee = :employee AND c.status = 'APPROVED'")
    BigDecimal getTotalApprovedCommissionForEmployee(@Param("employee") Employee employee);

    @Query("SELECT SUM(c.amount) FROM Commission c WHERE c.employee = :employee AND c.status = 'PENDING_APPROVAL'")
    BigDecimal getTotalPendingCommissionForEmployee(@Param("employee") Employee employee);

    @Query("SELECT c FROM Commission c WHERE c.employee = :employee AND c.status = 'APPROVED' ORDER BY c.approvedAt DESC")
    List<Commission> findApprovedCommissionsByEmployee(@Param("employee") Employee employee);
    
    @Query("SELECT c FROM Commission c WHERE c.employee = :employee AND c.status = 'APPROVED' AND (c.isPaid = false OR c.isPaid IS NULL) ORDER BY c.approvedAt DESC")
    List<Commission> findApprovedUnpaidCommissionsByEmployee(@Param("employee") Employee employee);
}