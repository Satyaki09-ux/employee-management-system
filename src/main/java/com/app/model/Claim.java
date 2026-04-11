package com.app.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "claims")
public class Claim {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private Employee assignedTo; // Reporting manager who needs to approve
    
    @NotBlank(message = "Claim type is required")
    @Column(nullable = false)
    private String claimType; // TRAVEL, MEAL, ACCOMMODATION, MEDICAL, OTHER
    
    @NotBlank(message = "Description is required")
    @Column(nullable = false, length = 1000)
    private String description;
    
    @NotNull(message = "Amount is required")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @NotNull(message = "Claim date is required")
    @Column(nullable = false)
    private LocalDate claimDate;
    
    @Column(length = 500)
    private String billAttachment; // File path or URL
    
    @Column(nullable = false)
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED
    
    @Column(length = 500)
    private String remarks;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private Employee approvedBy;
    
    @Column
    private LocalDateTime approvedAt;
    
    @Column(name = "is_paid", nullable = true)
    private Boolean isPaid = false; // Whether claim has been paid
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_by_id")
    private com.app.model.Admin paidBy; // Admin who marked claim as paid
    
    @Column(name = "paid_at")
    private LocalDateTime paidAt;
    
    @Column(name = "payment_method", length = 20)
    private String paymentMethod; // NEFT, UPI, CASH
    
    @Column(name = "transaction_reference", length = 100)
    private String transactionReference; // Transaction ID/Reference
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
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
    
    public Employee getAssignedTo() {
        return assignedTo;
    }
    
    public void setAssignedTo(Employee assignedTo) {
        this.assignedTo = assignedTo;
    }
    
    public String getClaimType() {
        return claimType;
    }
    
    public void setClaimType(String claimType) {
        this.claimType = claimType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public LocalDate getClaimDate() {
        return claimDate;
    }
    
    public void setClaimDate(LocalDate claimDate) {
        this.claimDate = claimDate;
    }
    
    public String getBillAttachment() {
        return billAttachment;
    }
    
    public void setBillAttachment(String billAttachment) {
        this.billAttachment = billAttachment;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getRemarks() {
        return remarks;
    }
    
    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
    
    public Employee getApprovedBy() {
        return approvedBy;
    }
    
    public void setApprovedBy(Employee approvedBy) {
        this.approvedBy = approvedBy;
    }
    
    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }
    
    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }
    
    public Boolean getIsPaid() {
        return isPaid != null ? isPaid : false;
    }
    
    public void setIsPaid(Boolean isPaid) {
        this.isPaid = isPaid;
    }
    
    public com.app.model.Admin getPaidBy() {
        return paidBy;
    }
    
    public void setPaidBy(com.app.model.Admin paidBy) {
        this.paidBy = paidBy;
    }
    
    public LocalDateTime getPaidAt() {
        return paidAt;
    }
    
    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }
    
    public String getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public String getTransactionReference() {
        return transactionReference;
    }
    
    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
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
}
