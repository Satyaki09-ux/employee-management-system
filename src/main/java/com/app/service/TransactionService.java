package com.app.service;

import com.app.model.Payment;
import com.app.model.Sale;
import com.app.model.Employee;
import com.app.model.SalesTarget;
import com.app.model.Product;
import com.app.repository.PaymentRepository;
import com.app.repository.SaleRepository;
import com.app.repository.SalesTargetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TransactionService {
    
    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private SalesTargetRepository salesTargetRepository;

    @Autowired
    private EmployeeService employeeService;
    
    /**
     * Generates a unique invoice number in format: INV-YYYYMMDD-XXXXXX
     * where XXXXXX is a 6-digit sequential number
     */
    public String generateInvoiceNumber() {
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String baseInvoiceNumber = "INV-" + datePrefix + "-";
        
        // Find the highest invoice number for today
        String todayPrefix = "INV-" + datePrefix + "-";
        List<Payment> todayPayments = paymentRepository.findAll().stream()
                .filter(p -> p.getInvoiceNumber() != null && p.getInvoiceNumber().startsWith(todayPrefix))
                .toList();
        
        int maxSequence = 0;
        for (Payment payment : todayPayments) {
            try {
                String sequenceStr = payment.getInvoiceNumber().substring(todayPrefix.length());
                int sequence = Integer.parseInt(sequenceStr);
                if (sequence > maxSequence) {
                    maxSequence = sequence;
                }
            } catch (NumberFormatException e) {
                // Skip invalid invoice numbers
            }
        }
        
        // Generate next sequence number (6 digits, zero-padded)
        int nextSequence = maxSequence + 1;
        String invoiceNumber = baseInvoiceNumber + String.format("%06d", nextSequence);
        
        // Ensure uniqueness (in case of race conditions)
        while (paymentRepository.findByInvoiceNumber(invoiceNumber) != null) {
            nextSequence++;
            invoiceNumber = baseInvoiceNumber + String.format("%06d", nextSequence);
        }
        
        return invoiceNumber;
    }
    
    /**
     * Gets all transactions (payments) - for admin
     */
    public List<Payment> getAllTransactions() {
        return paymentRepository.findAll();
    }
    
    /**
     * Gets transactions for a specific employee and all employees in their hierarchy
     */
    public List<Payment> getTransactionsForEmployee(Employee employee) {
        if (employee == null) {
            return List.of();
        }
        
        // Get all employee IDs in the hierarchy (including the employee themselves)
        List<Long> employeeIds = employeeService.getAllReportingEmployeeIds(employee);
        employeeIds.add(employee.getId());
        
        // Get all sales made by these employees
        return paymentRepository.findBySaleCreatedByIdIn(employeeIds);
    }
    
    /**
     * Gets a transaction by ID
     */
    public Optional<Payment> getTransactionById(Long id) {
        return paymentRepository.findById(id);
    }
    
    /**
     * Gets a transaction by invoice number
     */
    public Optional<Payment> getTransactionByInvoiceNumber(String invoiceNumber) {
        Payment payment = paymentRepository.findByInvoiceNumber(invoiceNumber);
        return Optional.ofNullable(payment);
    }
    
    /**
     * Gets all transactions for a specific sale
     */
    public List<Payment> getTransactionsBySale(Long saleId) {
        return paymentRepository.findBySaleId(saleId);
    }
    
    /**
     * Verifies if an employee has access to a transaction
     */
    public boolean hasAccessToTransaction(Employee employee, Payment transaction) {
        if (employee == null || transaction == null || transaction.getSale() == null) {
            return false;
        }
        
        // Employee has access if:
        // 1. They made the sale, OR
        // 2. The sale was made by someone in their hierarchy
        Long saleEmployeeId = transaction.getSale().getCreatedById();
        
        if (saleEmployeeId.equals(employee.getId())) {
            return true;
        }
        
        List<Long> hierarchyEmployeeIds = employeeService.getAllReportingEmployeeIds(employee);
        return hierarchyEmployeeIds.contains(saleEmployeeId);
    }

    /**
     * Saves a transaction (payment)
     */
    public Payment saveTransaction(Payment payment) {
        return paymentRepository.save(payment);
    }

    /**
     * Handle sale completion check after transaction settlement
     */
    public void calculateCommissionAfterSettlement(Payment payment) {
        // Payment settlement logic is now handled by SaleService.updatePaymentStatus()
        // which is called when payments are added or updated
    }
}
