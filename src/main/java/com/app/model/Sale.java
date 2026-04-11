package com.app.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sales")
public class Sale {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique 10-char sale ID: PGES + 6 alphanumeric (e.g. PGESA1B2C3) */
    @Column(name = "sale_id", unique = true, length = 10)
    private String saleId;
    
    @NotNull(message = "Created by is required")
    @Column(name = "created_by_id", nullable = false)
    private Long createdById;

    // Employee hierarchy for commission tracking (optional)
    @Column(name = "promoter_id")
    private Long promoterId;

    @Column(name = "zonal_head_id")
    private Long zonalHeadId;

    @Column(name = "cluster_head_id")
    private Long clusterHeadId;

    @Column(name = "asm_id")
    private Long asmId;

    // Transient field for display purposes
    @Transient
    private Employee createdByEmployee;
    
    @NotNull(message = "Customer is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    
    @NotNull(message = "Sale date is required")
    @Column(nullable = false)
    private LocalDate saleDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;
    
    @Column(name = "product_name", nullable = false)
    private String productName;
    
    @Column(length = 1000)
    private String description;
    
    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private java.util.List<Payment> payments;
    
    @Column(name = "due_date")
    private java.time.LocalDate dueDate;
    
    @NotNull(message = "Quantity is required")
    @Column(nullable = false)
    private Integer quantity = 1;
    
    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Unit price must be greater than 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;
    
    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Total amount must be greater than 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(nullable = false)
    private String paymentStatus = "PENDING"; // PENDING, PARTIAL, COMPLETED

    @Column(name = "status", nullable = false)
    private String saleStatus = "IN_PROGRESS"; // IN_PROGRESS, COMPLETED, CANCELLED
    
    @Column(length = 500)
    private String notes;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    public Sale() {
        // Ensure default values are set when object is created
        if (this.saleStatus == null || this.saleStatus.isEmpty()) {
            this.saleStatus = "IN_PROGRESS";
        }
        if (this.paymentStatus == null || this.paymentStatus.isEmpty()) {
            this.paymentStatus = "PENDING";
        }
    }

    @PrePersist
    protected void onPersist() {
        if (unitPrice != null && quantity != null) {
            totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
        // Ensure productName is set
        if ((productName == null || productName.isEmpty()) && product != null && product.getName() != null) {
            productName = product.getName();
        }
        // Ensure saleStatus is set
        if (saleStatus == null || saleStatus.isEmpty()) {
            saleStatus = "IN_PROGRESS";
        }
        // Ensure paymentStatus is set
        if (paymentStatus == null || paymentStatus.isEmpty()) {
            paymentStatus = "PENDING";
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (unitPrice != null && quantity != null) {
            totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
        // Ensure productName is set
        if ((productName == null || productName.isEmpty()) && product != null && product.getName() != null) {
            productName = product.getName();
        }
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }

    public String getSaleId() {
        return saleId;
    }

    public void setSaleId(String saleId) {
        this.saleId = saleId;
    }
    
    public Long getCreatedById() {
        return createdById;
    }

    public void setCreatedById(Long createdById) {
        this.createdById = createdById;
    }

    public Long getPromoterId() {
        return promoterId;
    }

    public void setPromoterId(Long promoterId) {
        this.promoterId = promoterId;
    }

    public Long getZonalHeadId() {
        return zonalHeadId;
    }

    public void setZonalHeadId(Long zonalHeadId) {
        this.zonalHeadId = zonalHeadId;
    }

    public Long getClusterHeadId() {
        return clusterHeadId;
    }

    public void setClusterHeadId(Long clusterHeadId) {
        this.clusterHeadId = clusterHeadId;
    }

    public Long getAsmId() {
        return asmId;
    }

    public void setAsmId(Long asmId) {
        this.asmId = asmId;
    }

    public Employee getCreatedByEmployee() {
        return createdByEmployee;
    }

    public void setCreatedByEmployee(Employee createdByEmployee) {
        this.createdByEmployee = createdByEmployee;
    }
    
    public Customer getCustomer() {
        return customer;
    }
    
    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
    
    public LocalDate getSaleDate() {
        return saleDate;
    }
    
    public void setSaleDate(LocalDate saleDate) {
        this.saleDate = saleDate;
    }
    
    public Product getProduct() {
        return product;
    }
    
    public void setProduct(Product product) {
        this.product = product;
    }
    
    public String getProductName() {
        if (productName != null && !productName.isEmpty()) {
            return productName;
        }
        return product != null ? product.getName() : null;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public java.util.List<Payment> getPayments() {
        return payments;
    }
    
    public void setPayments(java.util.List<Payment> payments) {
        this.payments = payments;
    }
    
    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getSaleStatus() {
        return saleStatus;
    }

    public void setSaleStatus(String saleStatus) {
        this.saleStatus = saleStatus;
    }
    
    /**
     * Calculates the total amount paid so far
     */
    public BigDecimal getPaidAmount() {
        try {
            if (payments == null || payments.isEmpty()) {
                return BigDecimal.ZERO;
            }
            return payments.stream()
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (Exception e) {
            // Handle lazy loading exceptions
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * Calculates the remaining amount to be paid
     */
    public BigDecimal getRemainingAmount() {
        try {
            BigDecimal paid = getPaidAmount();
            if (totalAmount == null) {
                return BigDecimal.ZERO;
            }
            return totalAmount.subtract(paid);
        } catch (Exception e) {
            // Handle lazy loading exceptions
            return totalAmount != null ? totalAmount : BigDecimal.ZERO;
        }
    }
    
    /**
     * Checks if the sale is fully paid
     */
    public boolean isFullyPaid() {
        return getRemainingAmount().compareTo(BigDecimal.ZERO) <= 0;
    }
    
    public java.time.LocalDate getDueDate() {
        return dueDate;
    }
    
    public void setDueDate(java.time.LocalDate dueDate) {
        this.dueDate = dueDate;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
        if (this.unitPrice != null && this.quantity != null) {
            this.totalAmount = this.unitPrice.multiply(BigDecimal.valueOf(this.quantity));
        }
    }
    
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
    
    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        if (this.unitPrice != null && this.quantity != null) {
            this.totalAmount = this.unitPrice.multiply(BigDecimal.valueOf(this.quantity));
        }
    }
    
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    // Legacy method for backward compatibility - maps to saleStatus
    public String getStatus() {
        return saleStatus;
    }

    // Legacy method for backward compatibility - maps to saleStatus
    public void setStatus(String status) {
        this.saleStatus = status;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
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

