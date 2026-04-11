package com.app.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, length = 10)
    private String productId;
    
    @NotBlank(message = "Product name is required")
    @Column(nullable = false, unique = true)
    private String name;
    
    @Column(length = 1000)
    private String description;
    
    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Unit price must be greater than 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;
    
    @NotNull(message = "Commission type is required")
    @Column(nullable = false)
    private String commissionType; // FIXED, PERCENTAGE
    
    @NotNull(message = "Commission value is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Commission value must be greater than or equal to 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal commissionValue;
    
    @Column(nullable = false)
    private String status = "ACTIVE"; // ACTIVE, INACTIVE
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    /** Audit: who created this product (e.g. "ADMIN:admin1" or "EMPLOYEE:PGABC123") */
    @Column(name = "created_by")
    private String createdBy;
    
    /** Audit: who last updated this product */
    @Column(name = "last_updated_by")
    private String lastUpdatedBy;
    
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
    
    public String getProductId() {
        return productId;
    }
    
    public void setProductId(String productId) {
        this.productId = productId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
    
    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
    
    public String getCommissionType() {
        return commissionType;
    }
    
    public void setCommissionType(String commissionType) {
        this.commissionType = commissionType;
    }
    
    public BigDecimal getCommissionValue() {
        return commissionValue;
    }
    
    public void setCommissionValue(BigDecimal commissionValue) {
        this.commissionValue = commissionValue;
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
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public String getLastUpdatedBy() {
        return lastUpdatedBy;
    }
    
    public void setLastUpdatedBy(String lastUpdatedBy) {
        this.lastUpdatedBy = lastUpdatedBy;
    }
    
    /**
     * Calculates the commission amount for a given sale amount.
     * If commission type is FIXED, returns commissionValue.
     * If commission type is PERCENTAGE, returns (saleAmount * commissionValue / 100).
     */
    public BigDecimal calculateCommission(BigDecimal saleAmount) {
        if (commissionType == null || commissionValue == null || saleAmount == null) {
            return BigDecimal.ZERO;
        }
        if ("FIXED".equals(commissionType)) {
            return commissionValue;
        } else if ("PERCENTAGE".equals(commissionType)) {
            return saleAmount.multiply(commissionValue).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Returns a display string for the commission, e.g. "₹50.00" or "5%"
     */
    public String getCommissionDisplay() {
        if (commissionType == null || commissionValue == null) {
            return "—";
        }
        if ("FIXED".equals(commissionType)) {
            return "₹" + commissionValue.setScale(2, java.math.RoundingMode.HALF_UP);
        } else if ("PERCENTAGE".equals(commissionType)) {
            return commissionValue.setScale(2, java.math.RoundingMode.HALF_UP) + "%";
        }
        return "—";
    }
}
