package com.app.service;

import com.app.model.Sale;
import com.app.model.Employee;
import com.app.model.Customer;
import com.app.model.Product;
import com.app.model.Payment;
import com.app.model.Commission;
import com.app.repository.SaleRepository;
import com.app.repository.EmployeeRepository;
import com.app.repository.CustomerRepository;
import com.app.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SaleService {
    
    @Autowired
    private SaleRepository saleRepository;
    
    @Autowired
    private EmployeeRepository employeeRepository;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private TransactionService transactionService;

    @Autowired
    private CommissionService commissionService;

    /**
     * Helper method to populate employee information for sales
     */
    private void populateEmployeeInfo(List<Sale> sales) {
        for (Sale sale : sales) {
            try {
                // Populate created by employee
                Employee createdBy = employeeRepository.findById(sale.getCreatedById()).orElse(null);
                sale.setCreatedByEmployee(createdBy);
            } catch (Exception e) {
                // Ignore errors
            }
        }
    }
    
    public List<Sale> getAllSales() {
        List<Sale> sales = saleRepository.findAll();
        populateEmployeeInfo(sales);
        return sales;
    }
    
    public List<Sale> getSalesByEmployee(Long employeeId) {
        List<Sale> sales = saleRepository.findByCreatedById(employeeId);
        // Initialize payments for each sale to avoid lazy loading issues
        for (Sale sale : sales) {
            try {
                List<Payment> payments = paymentRepository.findBySaleId(sale.getId());
                sale.setPayments(payments);
            } catch (Exception e) {
                sale.setPayments(java.util.Collections.emptyList());
            }
        }
        populateEmployeeInfo(sales);
        return sales;
    }

    /**
     * Gets all sales visible to the given employee based on their hierarchy level.
     * This follows the same logic as customer filtering.
     * - Promoter: Sales where promoterId = employee.id
     * - Zonal Head: Sales where zonalHeadId = employee.id OR (clusterHeadId in reporting cluster heads OR asmId in reporting ASMs)
     * - Cluster Head: Sales where clusterHeadId = employee.id OR asmId in reporting ASMs
     * - Area Sales Manager: Sales where asmId = employee.id
     */
    public List<Sale> getSalesForEmployee(Employee employee) {
        if (employee == null || employee.getId() == null) {
            return List.of();
        }

        List<Sale> sales = getSalesForEmployeeByHierarchy(employee);

        // Initialize payments for each sale to avoid lazy loading issues
        for (Sale sale : sales) {
            try {
                List<Payment> payments = paymentRepository.findBySaleId(sale.getId());
                sale.setPayments(payments);
            } catch (Exception e) {
                sale.setPayments(java.util.Collections.emptyList());
            }
        }

        populateEmployeeInfo(sales);
        return sales;
    }

    /**
     * Gets all sales visible to the given employee based on their hierarchy level.
     */
    private List<Sale> getSalesForEmployeeByHierarchy(Employee employee) {
        if (employee == null || employee.getId() == null) {
            return List.of();
        }

        String hierarchyLevel = employee.getHierarchyLevel();
        Long employeeId = employee.getId();

        if ("PROMOTER".equals(hierarchyLevel)) {
            return saleRepository.findByPromoterId(employeeId);
        } else if ("ZONAL_HEAD".equals(hierarchyLevel)) {
            // Get sales directly assigned to this zonal head
            List<Sale> sales = new java.util.ArrayList<>(saleRepository.findByZonalHeadId(employeeId));

            // Get all cluster heads reporting to this zonal head
            List<Employee> clusterHeads = employeeRepository.findAll().stream()
                    .filter(e -> "CLUSTER_HEAD".equals(e.getHierarchyLevel())
                            && e.getReportingManager() != null
                            && e.getReportingManager().getId().equals(employeeId))
                    .toList();

            // Get sales of those cluster heads
            for (Employee clusterHead : clusterHeads) {
                sales.addAll(saleRepository.findByClusterHeadId(clusterHead.getId()));
            }

            // Get all ASMs reporting to those cluster heads
            for (Employee clusterHead : clusterHeads) {
                List<Employee> asms = employeeRepository.findAll().stream()
                        .filter(e -> "AREA_SALES_MANAGER".equals(e.getHierarchyLevel())
                                && e.getReportingManager() != null
                                && e.getReportingManager().getId().equals(clusterHead.getId()))
                        .toList();
                for (Employee asm : asms) {
                    sales.addAll(saleRepository.findByAsmId(asm.getId()));
                }
            }

            return sales.stream().distinct().toList();
        } else if ("CLUSTER_HEAD".equals(hierarchyLevel)) {
            // Get sales directly assigned to this cluster head
            List<Sale> sales = new java.util.ArrayList<>(saleRepository.findByClusterHeadId(employeeId));

            // Get all ASMs reporting to this cluster head
            List<Employee> asms = employeeRepository.findAll().stream()
                    .filter(e -> "AREA_SALES_MANAGER".equals(e.getHierarchyLevel())
                            && e.getReportingManager() != null
                            && e.getReportingManager().getId().equals(employeeId))
                    .toList();

            // Get sales of those ASMs
            for (Employee asm : asms) {
                sales.addAll(saleRepository.findByAsmId(asm.getId()));
            }

            return sales.stream().distinct().toList();
        } else if ("AREA_SALES_MANAGER".equals(hierarchyLevel)) {
            return saleRepository.findByAsmId(employeeId);
        }

        return List.of();
    }

    public List<Sale> getSalesByCustomer(Long customerId) {
        Optional<Customer> customer = customerRepository.findById(customerId);
        List<Sale> sales = customer.map(saleRepository::findByCustomer).orElse(List.of());
        populateEmployeeInfo(sales);
        return sales;
    }
    
    public List<Sale> getSalesByDateRange(LocalDate startDate, LocalDate endDate) {
        List<Sale> sales = saleRepository.findBySaleDateBetween(startDate, endDate);
        populateEmployeeInfo(sales);
        return sales;
    }
    
    public Optional<Sale> getSaleById(Long id) {
        return saleRepository.findById(id);
    }
    
    public Sale saveSale(Sale sale) {
        // Generate unique 10-char sale ID (PGES + 6 alphanumeric) for new sales
        if (sale.getId() == null && (sale.getSaleId() == null || sale.getSaleId().isEmpty())) {
            sale.setSaleId(generateSaleId());
        }

        // Calculate total amount from product or unit price
        if (sale.getProduct() != null && sale.getProduct().getUnitPrice() != null && sale.getQuantity() != null) {
            sale.setUnitPrice(sale.getProduct().getUnitPrice());
            sale.setTotalAmount(sale.getUnitPrice().multiply(BigDecimal.valueOf(sale.getQuantity())));
            // Set product name from product
            if (sale.getProduct().getName() != null) {
                sale.setProductName(sale.getProduct().getName());
            }
        } else if (sale.getUnitPrice() != null && sale.getQuantity() != null) {
            sale.setTotalAmount(sale.getUnitPrice().multiply(BigDecimal.valueOf(sale.getQuantity())));
        }
        
        // Ensure productName is set
        if ((sale.getProductName() == null || sale.getProductName().isEmpty()) && sale.getProduct() != null) {
            sale.setProductName(sale.getProduct().getName());
        }
        
        
        return saleRepository.save(sale);
    }

    /**
     * Generate unique 10-char sale ID: PGES + 6 random alphanumeric characters.
     */
    private String generateSaleId() {
        SecureRandom random = new SecureRandom();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder saleId = new StringBuilder("PGES");
        for (int i = 0; i < 6; i++) {
            saleId.append(chars.charAt(random.nextInt(chars.length())));
        }
        while (saleRepository.findBySaleId(saleId.toString()).isPresent()) {
            saleId = new StringBuilder("PGES");
            for (int i = 0; i < 6; i++) {
                saleId.append(chars.charAt(random.nextInt(chars.length())));
            }
        }
        return saleId.toString();
    }
    
    
    /**
     * Adds a payment to a sale and generates an invoice number
     */
    public Payment addPayment(Long saleId, BigDecimal amount, String transactionMode, String transactionId, String notes, java.time.LocalDate dueDate) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new IllegalArgumentException("Sale not found"));
        
        // Validate transaction ID uniqueness if provided
        if (transactionId != null && !transactionId.trim().isEmpty()) {
            Payment existingPayment = paymentRepository.findByTransactionId(transactionId.trim());
            if (existingPayment != null) {
                throw new IllegalArgumentException("Duplicate transaction reference. This transaction ID has already been used for another payment.");
            }
        }
        
        Payment payment = new Payment();
        payment.setSale(sale);
        payment.setAmount(amount);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setTransactionMode(transactionMode);
        payment.setTransactionId(transactionId != null ? transactionId.trim() : null);
        payment.setNotes(notes);

        // Note: Invoice number will be generated when payment is settled by admin
        // payment.setInvoiceNumber(invoiceNumber);
        
        Payment savedPayment = paymentRepository.save(payment);
        
        // Update sale due date if provided and sale is not fully paid
        if (dueDate != null && !sale.isFullyPaid()) {
            sale.setDueDate(dueDate);
        }
        
        // Update payment status and check if sale should be completed
        updatePaymentStatus(sale);
        
        return savedPayment;
    }
    
    
    /**
     * Gets all payments for a sale
     */
    public List<Payment> getPaymentsBySale(Long saleId) {
        return paymentRepository.findBySaleId(saleId);
    }
    
    
    public void deleteSale(Long id) {
        saleRepository.deleteById(id);
    }
    
    public BigDecimal getTotalSalesByDateRange(LocalDate startDate, LocalDate endDate) {
        List<Sale> sales = saleRepository.findBySaleDateBetween(startDate, endDate);
        return sales.stream()
                   .filter(s -> "COMPLETED".equals(s.getSaleStatus()))
                   .map(Sale::getTotalAmount)
                   .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public BigDecimal getTotalSalesByEmployee(Long employeeId, LocalDate startDate, LocalDate endDate) {
        List<Sale> sales = saleRepository.findByCreatedByIdAndSaleDateBetween(employeeId, startDate, endDate);
        return sales.stream()
                   .filter(s -> "COMPLETED".equals(s.getSaleStatus()))
                   .map(Sale::getTotalAmount)
                   .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public List<Sale> getSalesByEmployeeAndDateRange(Long employeeId, LocalDate startDate, LocalDate endDate) {
        List<Sale> sales = saleRepository.findByCreatedByIdAndSaleDateBetween(employeeId, startDate, endDate);
        populateEmployeeInfo(sales);
        return sales;
    }

    /**
     * Gets sales visible to the employee within a date range based on hierarchy.
     */
    public List<Sale> getSalesForEmployeeAndDateRange(Employee employee, LocalDate startDate, LocalDate endDate) {
        List<Sale> allSales = getSalesForEmployee(employee);
        return allSales.stream()
                .filter(sale -> sale.getSaleDate() != null &&
                        !sale.getSaleDate().isBefore(startDate) &&
                        !sale.getSaleDate().isAfter(endDate))
                .toList();
    }
    
    /**
     * Gets revenue by product for a given date range
     * Returns a map of product name to total revenue
     */
    public java.util.Map<String, BigDecimal> getRevenueByProduct(LocalDate startDate, LocalDate endDate) {
        List<Sale> sales = saleRepository.findBySaleDateBetween(startDate, endDate);
        java.util.Map<String, BigDecimal> revenueMap = new java.util.HashMap<>();
        
        for (Sale sale : sales) {
            // Only count completed sales
            if (!"COMPLETED".equals(sale.getSaleStatus())) {
                continue;
            }
            
            String productName = sale.getProductName();
            if (productName == null || productName.isEmpty()) {
                productName = sale.getProduct() != null ? sale.getProduct().getName() : "Unknown";
            }
            
            BigDecimal amount = sale.getTotalAmount();
            if (amount == null) {
                amount = BigDecimal.ZERO;
            }
            
            revenueMap.put(productName, revenueMap.getOrDefault(productName, BigDecimal.ZERO).add(amount));
        }
        
        return revenueMap;
    }

    /**
     * Check if a sale should be marked as completed
     * Sale is completed when all payments are settled and payment status is COMPLETED
     */
    @Transactional
    public void checkAndCompleteSale(Sale sale) {
        // Get all payments for this sale
        List<Payment> payments = paymentRepository.findBySale(sale);

        // Check if all payments are settled
        boolean allPaymentsSettled = payments.stream()
                .allMatch(payment -> payment.getSettled() != null && payment.getSettled());

        // Check if payment status is COMPLETED
        boolean paymentStatusCompleted = "COMPLETED".equals(sale.getPaymentStatus());

        if (allPaymentsSettled && paymentStatusCompleted && !"COMPLETED".equals(sale.getSaleStatus())) {
            // Mark sale as completed
            sale.setSaleStatus("COMPLETED");
            saleRepository.save(sale);

            // Create commission entry only if one doesn't exist
            List<Commission> existingCommissions = commissionService.getCommissionRepository().findBySale(sale);
            if (existingCommissions.isEmpty()) {
                commissionService.createCommissionForSale(sale);
            }
        }
    }

    /**
     * Update payment status based on payments
     */
    @Transactional
    public void updatePaymentStatus(Sale sale) {
        List<Payment> payments = paymentRepository.findBySale(sale);
        BigDecimal totalPaid = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPaid.compareTo(BigDecimal.ZERO) == 0) {
            sale.setPaymentStatus("PENDING");
        } else if (totalPaid.compareTo(sale.getTotalAmount()) < 0) {
            sale.setPaymentStatus("PARTIAL");
        } else {
            sale.setPaymentStatus("COMPLETED");
        }

        saleRepository.save(sale);

        // Check if sale should be completed after payment status update
        checkAndCompleteSale(sale);
    }
}

