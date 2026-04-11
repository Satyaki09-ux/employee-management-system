package com.app.service;

import com.app.model.Commission;
import com.app.model.Employee;
import com.app.model.Sale;
import com.app.model.SalesTarget;
import com.app.model.Admin;
import com.app.repository.CommissionRepository;
import com.app.repository.SalesTargetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CommissionService {

    @Autowired
    private CommissionRepository commissionRepository;

    @Autowired
    private SalesTargetRepository salesTargetRepository;

    @Autowired
    private EmployeeService employeeService;

    public CommissionRepository getCommissionRepository() {
        return commissionRepository;
    }

    /**
     * Create a commission entry for a completed sale
     */
    @Transactional
    public Commission createCommissionForSale(Sale sale) {
        // Calculate commission amount (you may want to implement custom logic here)
        BigDecimal commissionAmount = calculateCommissionAmount(sale);

        Commission commission = new Commission();
        commission.setSale(sale);

        // Commission recipient is the employee who created the sale
        Employee creator = employeeService.getEmployeeById(sale.getCreatedById())
                .orElseThrow(() -> new RuntimeException("Sale creator not found"));
        commission.setEmployee(creator);

        commission.setAmount(commissionAmount);
        commission.setStatus("PENDING_APPROVAL");

        return commissionRepository.save(commission);
    }

    /**
     * Calculate commission amount for a sale
     * Uses the product's commission settings (FIXED or PERCENTAGE)
     */
    private BigDecimal calculateCommissionAmount(Sale sale) {
        // If sale has a product, use the product's commission calculation
        if (sale.getProduct() != null) {
            return sale.getProduct().calculateCommission(sale.getTotalAmount());
        }
        
        // Fallback: if no product, use 5% of total sale amount
        // This should rarely happen, but provides a default
        return sale.getTotalAmount().multiply(new BigDecimal("0.05"));
    }

    /**
     * Approve a commission by the reporting manager or above
     */
    @Transactional
    public Commission approveCommission(Long commissionId, Employee approver) {
        Commission commission = commissionRepository.findById(commissionId)
                .orElseThrow(() -> new IllegalArgumentException("Commission not found"));

        if (!commission.getStatus().equals("PENDING_APPROVAL")) {
            throw new IllegalStateException("Commission is not in pending approval status");
        }

        // Verify that the approver is not an area sales manager
        // Cluster Heads, Zonal Heads, and Promoters can approve any commissions
        if ("AREA_SALES_MANAGER".equals(approver.getHierarchyLevel())) {
            throw new IllegalArgumentException("Area Sales Managers cannot approve commissions");
        }

        commission.setStatus("APPROVED");
        commission.setApprovedBy(approver);
        commission.setApprovedAt(LocalDateTime.now());

        Commission approvedCommission = commissionRepository.save(commission);

        // Update sales target with accumulated commission
        updateSalesTargetWithCommission(commission);

        return approvedCommission;
    }

    /**
     * Reject a commission
     */
    @Transactional
    public Commission rejectCommission(Long commissionId, Employee rejector) {
        Commission commission = commissionRepository.findById(commissionId)
                .orElseThrow(() -> new IllegalArgumentException("Commission not found"));

        if (!commission.getStatus().equals("PENDING_APPROVAL")) {
            throw new IllegalStateException("Commission is not in pending approval status");
        }

        // Verify that the rejector is not an area sales manager
        // Cluster Heads, Zonal Heads, and Promoters can reject any commissions
        if ("AREA_SALES_MANAGER".equals(rejector.getHierarchyLevel())) {
            throw new IllegalArgumentException("Area Sales Managers cannot reject commissions");
        }

        commission.setStatus("REJECTED");
        commission.setApprovedBy(rejector);
        commission.setApprovedAt(LocalDateTime.now());

        return commissionRepository.save(commission);
    }

    /**
     * Update sales target when a commission is approved.
     * - achievedAmount: add the actual sale amount (total of unit sold) to track progress toward target.
     * - commissionAmount: add the commission earned.
     * Target amount is fixed (FDP); we reduce remaining target by sale amount, not commission.
     */
    private void updateSalesTargetWithCommission(Commission commission) {
        Sale sale = commission.getSale();
        if (sale == null || sale.getTotalAmount() == null) {
            return;
        }

        List<SalesTarget> targets = salesTargetRepository
                .findByEmployeeAndProductAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqual(
                        commission.getEmployee(),
                        sale.getProduct(),
                        sale.getSaleDate(),
                        sale.getSaleDate());

        SalesTarget salesTarget = targets.stream().findFirst().orElse(null);

        if (salesTarget != null) {
            // Accumulate commission amount
            BigDecimal currentCommission = salesTarget.getCommissionAmount() != null
                    ? salesTarget.getCommissionAmount()
                    : BigDecimal.ZERO;
            salesTarget.setCommissionAmount(currentCommission.add(commission.getAmount()));

            // Achieved amount = actual sale total (unit sold), not commission; target is reduced by sale amount
            BigDecimal currentAchieved = salesTarget.getAchievedAmount() != null
                    ? salesTarget.getAchievedAmount()
                    : BigDecimal.ZERO;
            salesTarget.setAchievedAmount(currentAchieved.add(sale.getTotalAmount()));

            salesTargetRepository.save(salesTarget);
        }
    }

    /**
     * Get all commissions for an employee
     */
    public List<Commission> getCommissionsByEmployee(Employee employee) {
        return commissionRepository.findByEmployee(employee);
    }

    /**
     * Get pending commissions for a manager to approve
     */
    public List<Commission> getPendingCommissionsForManager(Employee manager) {
        return commissionRepository.findPendingCommissionsForManager(manager);
    }

    /**
     * Get total approved commission for an employee
     */
    public BigDecimal getTotalApprovedCommissionForEmployee(Employee employee) {
        BigDecimal total = commissionRepository.getTotalApprovedCommissionForEmployee(employee);
        return total != null ? total : BigDecimal.ZERO;
    }

    /**
     * Get total pending commission for an employee
     */
    public BigDecimal getTotalPendingCommissionForEmployee(Employee employee) {
        BigDecimal total = commissionRepository.getTotalPendingCommissionForEmployee(employee);
        return total != null ? total : BigDecimal.ZERO;
    }

    /**
     * Get commission by ID
     */
    public Commission getCommissionById(Long id) {
        return commissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Commission not found"));
    }
    
    /**
     * Mark commission as paid
     */
    @Transactional
    public Commission markCommissionAsPaid(Long commissionId, Admin paidBy, String paymentMethod, String transactionReference) {
        Commission commission = commissionRepository.findById(commissionId)
                .orElseThrow(() -> new IllegalArgumentException("Commission not found"));
        
        if (!"APPROVED".equals(commission.getStatus())) {
            throw new IllegalStateException("Only approved commissions can be marked as paid");
        }
        
        commission.setIsPaid(true);
        commission.setPaidBy(paidBy);
        commission.setPaidAt(LocalDateTime.now());
        commission.setPaymentMethod(paymentMethod);
        commission.setTransactionReference(transactionReference);
        
        return commissionRepository.save(commission);
    }
    
    /**
     * Mark commission as paid (backward compatibility - used in salary management)
     */
    @Transactional
    public Commission markCommissionAsPaid(Long commissionId) {
        Commission commission = commissionRepository.findById(commissionId)
                .orElseThrow(() -> new IllegalArgumentException("Commission not found"));
        
        if (!"APPROVED".equals(commission.getStatus())) {
            throw new IllegalStateException("Only approved commissions can be marked as paid");
        }
        
        commission.setIsPaid(true);
        return commissionRepository.save(commission);
    }
    
    /**
     * Get approved unpaid commissions for an employee
     */
    public List<Commission> getApprovedUnpaidCommissionsByEmployee(Employee employee) {
        return commissionRepository.findApprovedUnpaidCommissionsByEmployee(employee);
    }

    /**
     * Check if an employee can approve commissions for another employee
     * The approver must be a reporting manager or above in the hierarchy
     */
    public boolean canApproveCommission(Employee commissionEmployee, Employee approver) {
        // Get all employees that report to the approver (including indirect reports)
        List<Employee> reportingEmployees = employeeService.getAllReportingEmployees(approver);

        // Check if the commission employee is in the reporting chain
        return reportingEmployees.stream()
                .anyMatch(emp -> emp.getId().equals(commissionEmployee.getId()));
    }

    /**
     * Check if a sale was created by an eligible role (promoter, zonal head, cluster head)
     * and if the current user can approve commissions for it
     */
    public boolean canShowApproveButtonForSale(Sale sale, Employee currentUser) {
        // Check if current user is NOT an area sales manager
        // Only Cluster Heads, Zonal Heads, and Promoters can approve commissions
        if ("AREA_SALES_MANAGER".equals(currentUser.getHierarchyLevel())) {
            return false;
        }

        // Get commissions for this sale
        List<Commission> commissions = commissionRepository.findBySale(sale);

        // Check if there are any pending commissions for this sale
        // Higher-level employees (Cluster Head, Zonal Head, Promoter) can approve any commissions
        return commissions.stream()
                .anyMatch(commission -> "PENDING_APPROVAL".equals(commission.getStatus()));
    }
}