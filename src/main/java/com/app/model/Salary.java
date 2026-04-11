package com.app.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "salaries")
public class Salary {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "released_by")
    private com.app.model.Admin releasedBy; // Admin who released the salary
    
    @NotNull(message = "Start date is required")
    @Column(nullable = false)
    private LocalDate startDate; // Salary period start date
    
    @NotNull(message = "End date is required")
    @Column(nullable = false)
    private LocalDate endDate; // Salary period end date
    
    @NotNull(message = "Salary month is required")
    @Column(name = "salary_month", nullable = false)
    private LocalDate salaryMonth; // The month for which salary is paid (first day of the month)
    
    @NotNull(message = "Basic salary is required")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal basicSalary;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal allowances; // HRA, Transport, etc.
    
    @Column(precision = 10, scale = 2)
    private BigDecimal deductions; // Tax, PF, etc.
    
    @Column(precision = 10, scale = 2)
    private BigDecimal bonus;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal overtime;
    
    @NotNull(message = "Net salary is required")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal netSalary;
    
    @Column(length = 500)
    private String payslipPath; // File path for generated payslip
    
    @Column(nullable = false)
    private String status = "PENDING"; // PENDING, PROCESSED, PAID
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @Column
    private LocalDateTime paidAt;
    
    @Column(name = "transaction_reference", length = 100)
    private String transactionReference; // NEFT/UPI/IMPS reference
    
    @Column(name = "transaction_type", length = 20)
    private String transactionType; // NEFT, UPI, IMPS
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Employee getEmployee() {
        return employee;
    }
    
    public void setEmployee(Employee employee) {
        this.employee = employee;
    }
    
    public com.app.model.Admin getReleasedBy() {
        return releasedBy;
    }
    
    public void setReleasedBy(com.app.model.Admin releasedBy) {
        this.releasedBy = releasedBy;
    }
    
    public LocalDate getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
    
    public LocalDate getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
    
    public LocalDate getSalaryMonth() {
        return salaryMonth;
    }
    
    public void setSalaryMonth(LocalDate salaryMonth) {
        this.salaryMonth = salaryMonth;
    }
    
    public BigDecimal getBasicSalary() {
        return basicSalary;
    }
    
    public void setBasicSalary(BigDecimal basicSalary) {
        this.basicSalary = basicSalary;
    }
    
    public BigDecimal getAllowances() {
        return allowances;
    }
    
    public void setAllowances(BigDecimal allowances) {
        this.allowances = allowances;
    }
    
    public BigDecimal getDeductions() {
        return deductions;
    }
    
    public void setDeductions(BigDecimal deductions) {
        this.deductions = deductions;
    }
    
    public BigDecimal getBonus() {
        return bonus;
    }
    
    public void setBonus(BigDecimal bonus) {
        this.bonus = bonus;
    }
    
    public BigDecimal getOvertime() {
        return overtime;
    }
    
    public void setOvertime(BigDecimal overtime) {
        this.overtime = overtime;
    }
    
    public BigDecimal getNetSalary() {
        return netSalary;
    }
    
    public void setNetSalary(BigDecimal netSalary) {
        this.netSalary = netSalary;
    }
    
    public String getPayslipPath() {
        return payslipPath;
    }
    
    public void setPayslipPath(String payslipPath) {
        this.payslipPath = payslipPath;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public LocalDateTime getPaidAt() {
        return paidAt;
    }
    
    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }
    
    public String getTransactionReference() {
        return transactionReference;
    }
    
    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }
    
    public String getTransactionType() {
        return transactionType;
    }
    
    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }
}
