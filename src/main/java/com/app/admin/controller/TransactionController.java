package com.app.admin.controller;

import com.app.model.Admin;
import com.app.model.Employee;
import com.app.model.Payment;
import com.app.service.AdminService;
import com.app.service.EmployeeService;
import com.app.service.TransactionService;
import com.app.service.SaleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private SaleService saleService;
    
    @GetMapping
    public String listTransactions(Model model, 
                                   @RequestParam(required = false) String search,
                                   @RequestParam(required = false) String invoiceNumber) {
        List<Payment> transactions = transactionService.getAllTransactions();
        
        // Filter by search term (customer name, product name, invoice number)
        if (search != null && !search.isEmpty()) {
            String searchLower = search.toLowerCase();
            transactions = transactions.stream()
                    .filter(t -> {
                        if (t.getSale() != null && t.getSale().getCustomer() != null) {
                            String customerName = t.getSale().getCustomer().getContactPerson().toLowerCase();
                            if (customerName.contains(searchLower)) return true;
                        }
                        if (t.getSale() != null && t.getSale().getProductName() != null) {
                            String productName = t.getSale().getProductName().toLowerCase();
                            if (productName.contains(searchLower)) return true;
                        }
                        if (t.getInvoiceNumber() != null) {
                            String invNum = t.getInvoiceNumber().toLowerCase();
                            if (invNum.contains(searchLower)) return true;
                        }
                        return false;
                    })
                    .toList();
        }
        
        // Filter by invoice number if provided
        if (invoiceNumber != null && !invoiceNumber.isEmpty()) {
            transactions = transactions.stream()
                    .filter(t -> t.getInvoiceNumber() != null && 
                               t.getInvoiceNumber().equalsIgnoreCase(invoiceNumber))
                    .toList();
        }
        
        model.addAttribute("transactions", transactions);
        model.addAttribute("search", search);
        model.addAttribute("invoiceNumber", invoiceNumber);
        return "admin/transactions/list";
    }
    
    @GetMapping("/view/{id}")
    public String viewTransaction(@PathVariable Long id, Model model) {
        Optional<Payment> transactionOpt = transactionService.getTransactionById(id);
        if (transactionOpt.isEmpty()) {
            return "redirect:/admin/transactions?error=Transaction not found";
        }
        
        Payment transaction = transactionOpt.get();

        // Get the employee who created the sale
        Employee createdByEmployee = null;
        if (transaction.getSale() != null) {
            createdByEmployee = employeeService.getEmployeeById(transaction.getSale().getCreatedById())
                    .orElse(null);
        }

        model.addAttribute("transaction", transaction);
        model.addAttribute("createdByEmployee", createdByEmployee);
        return "admin/transactions/view";
    }
    
    @GetMapping("/invoice/{id}")
    public String viewInvoice(@PathVariable Long id, Model model) {
        Optional<Payment> transactionOpt = transactionService.getTransactionById(id);
        if (transactionOpt.isEmpty()) {
            return "redirect:/admin/transactions?error=Transaction not found";
        }

        Payment transaction = transactionOpt.get();
        model.addAttribute("transaction", transaction);
        return "admin/transactions/invoice";
    }

    @PostMapping("/settle/{id}")
    public String settleTransaction(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Optional<Payment> transactionOpt = transactionService.getTransactionById(id);
            if (transactionOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Transaction not found");
                return "redirect:/admin/transactions";
            }

            Payment transaction = transactionOpt.get();

            // Get current admin who is settling
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();

            Optional<Admin> settlerOpt = adminService.getAdminByUsername(username);
            if (settlerOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Admin not found");
                return "redirect:/admin/transactions/view/" + id;
            }

            Admin settler = settlerOpt.get();

            // Generate invoice number if not already present
            if (transaction.getInvoiceNumber() == null || transaction.getInvoiceNumber().isEmpty()) {
                String invoiceNumber = transactionService.generateInvoiceNumber();
                transaction.setInvoiceNumber(invoiceNumber);
            }

            // Mark as settled and track who did it
            transaction.setSettled(true);
            transaction.setSettledBy(settler);
            transactionService.saveTransaction(transaction);

            // Update payment status and check if sale should be completed
            if (transaction.getSale() != null) {
                saleService.updatePaymentStatus(transaction.getSale());
            }

            redirectAttributes.addFlashAttribute("message", "Transaction marked as settled successfully");
            return "redirect:/admin/transactions/view/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error settling transaction: " + e.getMessage());
            return "redirect:/admin/transactions";
        }
    }
}
