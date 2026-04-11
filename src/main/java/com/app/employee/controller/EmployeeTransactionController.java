package com.app.employee.controller;

import com.app.model.Payment;
import com.app.model.Employee;
import com.app.service.TransactionService;
import com.app.service.EmployeeService;
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
@RequestMapping("/employee/transactions")
public class EmployeeTransactionController {
    
    @Autowired
    private TransactionService transactionService;
    
    @Autowired
    private EmployeeService employeeService;
    
    private Employee getCurrentEmployee() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return employeeService.getEmployeeByUsername(username)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
    }
    
    @GetMapping
    public String listTransactions(Model model,
                                   @RequestParam(required = false) String search,
                                   @RequestParam(required = false) String invoiceNumber) {
        Employee currentEmployee = getCurrentEmployee();

        // Get transactions for current employee and hierarchy
        List<Payment> transactions = transactionService.getTransactionsForEmployee(currentEmployee);
        
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
        model.addAttribute("currentEmployee", currentEmployee);
        return "employee/transactions/list";
    }
    
    @GetMapping("/view/{id}")
    public String viewTransaction(@PathVariable Long id, Model model) {
        Employee currentEmployee = getCurrentEmployee();
        Optional<Payment> transactionOpt = transactionService.getTransactionById(id);
        
        if (transactionOpt.isEmpty()) {
            return "redirect:/employee/transactions?error=Transaction not found";
        }
        
        Payment transaction = transactionOpt.get();

        // Verify access
        if (!transactionService.hasAccessToTransaction(currentEmployee, transaction)) {
            return "redirect:/employee/transactions?error=You do not have access to this transaction";
        }

        // Get the employee who created the sale
        Employee createdByEmployee = null;
        if (transaction.getSale() != null) {
            createdByEmployee = employeeService.getEmployeeById(transaction.getSale().getCreatedById())
                    .orElse(null);
        }

        model.addAttribute("transaction", transaction);
        model.addAttribute("createdByEmployee", createdByEmployee);
        model.addAttribute("currentEmployee", currentEmployee);
        return "employee/transactions/view";
    }
    
    @GetMapping("/invoice/{id}")
    public String viewInvoice(@PathVariable Long id, Model model) {
        Employee currentEmployee = getCurrentEmployee();
        Optional<Payment> transactionOpt = transactionService.getTransactionById(id);
        
        if (transactionOpt.isEmpty()) {
            return "redirect:/employee/transactions?error=Transaction not found";
        }
        
        Payment transaction = transactionOpt.get();
        
        // Verify access
        if (!transactionService.hasAccessToTransaction(currentEmployee, transaction)) {
            return "redirect:/employee/transactions?error=You do not have access to this transaction";
        }

        // Get the employee who created the sale
        Employee createdByEmployee = null;
        if (transaction.getSale() != null) {
            createdByEmployee = employeeService.getEmployeeById(transaction.getSale().getCreatedById())
                    .orElse(null);
        }

        model.addAttribute("transaction", transaction);
        model.addAttribute("createdByEmployee", createdByEmployee);
        model.addAttribute("currentEmployee", currentEmployee);
        return "employee/transactions/invoice";
    }
}
