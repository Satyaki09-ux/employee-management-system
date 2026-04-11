package com.app.admin.controller;

import com.app.model.Sale;
import com.app.model.Employee;
import com.app.model.Customer;
import com.app.model.Payment;
import com.app.service.SaleService;
import com.app.service.EmployeeService;
import com.app.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin/sales")
public class SaleController {
    
    @Autowired
    private SaleService saleService;
    
    @Autowired
    private EmployeeService employeeService;
    
    @Autowired
    private CustomerService customerService;
    
    @GetMapping
    public String listSales(Model model,
                           @RequestParam(required = false) Long employeeId,
                           @RequestParam(required = false) Long customerId,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<Sale> sales;
        
        if (employeeId != null && startDate != null && endDate != null) {
            sales = saleService.getSalesByDateRange(startDate, endDate)
                    .stream()
                    .filter(s -> s.getCreatedById().equals(employeeId))
                    .toList();
            model.addAttribute("selectedEmployeeId", employeeId);
        } else if (employeeId != null) {
            sales = saleService.getSalesByEmployee(employeeId);
            model.addAttribute("selectedEmployeeId", employeeId);
        } else if (customerId != null) {
            sales = saleService.getSalesByCustomer(customerId);
            model.addAttribute("selectedCustomerId", customerId);
        } else if (startDate != null && endDate != null) {
            sales = saleService.getSalesByDateRange(startDate, endDate);
        } else {
            sales = saleService.getSalesByDateRange(LocalDate.now().minusDays(30), LocalDate.now());
        }
        
        model.addAttribute("sales", sales);
        model.addAttribute("employees", employeeService.getActiveEmployees());
        model.addAttribute("customers", customerService.getActiveCustomers());
        return "admin/sales/list";
    }
    
    @GetMapping("/new")
    public String showSaleForm(Model model) {
        Sale sale = new Sale();
        sale.setSaleDate(LocalDate.now());
        model.addAttribute("sale", sale);
        model.addAttribute("employees", employeeService.getActiveEmployees());
        model.addAttribute("customers", customerService.getActiveCustomers());
        return "admin/sales/form";
    }
    
    @PostMapping("/save")
    public String saveSale(@Valid @ModelAttribute Sale sale,
                          @RequestParam(required = false) Long employeeId,
                          @RequestParam(required = false) Long customerId,
                          BindingResult result,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (employeeId != null) {
            Employee employee = employeeService.getEmployeeById(employeeId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid employee ID: " + employeeId));
            sale.setCreatedById(employee.getId());
            // Set hierarchy fields based on employee hierarchy
            employeeService.setSaleHierarchyFields(sale, employee);
        }
        
        if (customerId != null) {
            Customer customer = customerService.getCustomerById(customerId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid customer ID: " + customerId));
            sale.setCustomer(customer);
        }
        
        if (result.hasErrors()) {
            model.addAttribute("employees", employeeService.getActiveEmployees());
            model.addAttribute("customers", customerService.getActiveCustomers());
            return "admin/sales/form";
        }
        
        saleService.saveSale(sale);
        redirectAttributes.addFlashAttribute("message", "Sale saved successfully!");
        return "redirect:/sales";
    }
    
    @GetMapping("/view/{id}")
    public String viewSale(@PathVariable Long id, Model model) {
        Sale sale = saleService.getSaleById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid sale ID: " + id));

        // Get all payments/transactions for this sale
        List<Payment> transactions = saleService.getPaymentsBySale(sale.getId());

        // Calculate payment summary
        BigDecimal paidAmount = transactions.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remainingAmount = sale.getTotalAmount().subtract(paidAmount);

        // Get the employee who created this sale
        Employee createdByEmployee = employeeService.getEmployeeById(sale.getCreatedById())
                .orElse(null);

        model.addAttribute("sale", sale);
        model.addAttribute("transactions", transactions);
        model.addAttribute("createdByEmployee", createdByEmployee);
        model.addAttribute("paidAmount", paidAmount);
        model.addAttribute("remainingAmount", remainingAmount);

        return "admin/sales/view";
    }
}
