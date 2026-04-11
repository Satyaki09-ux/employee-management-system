package com.app.employee.controller;

import com.app.model.Sale;
import com.app.model.Employee;
import com.app.model.Customer;
import com.app.model.Product;
import com.app.model.Payment;
import com.app.service.SaleService;
import com.app.service.CustomerService;
import com.app.service.ProductService;
import com.app.service.EmployeeService;
import com.app.service.CommissionService;
import com.app.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/employee/sales")
public class EmployeeSaleController {

    @Autowired
    private SaleService saleService;
    
    @Autowired
    private CustomerService customerService;
    
    @Autowired
    private ProductService productService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private CommissionService commissionService;

    @Autowired
    private PaymentRepository paymentRepository;

    private Employee getCurrentEmployee() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return employeeService.getEmployeeByUsername(username)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
    }

    @GetMapping
    public String listSales(Model model, @RequestParam(required = false) String search) {
        Employee currentEmployee = getCurrentEmployee();
        List<Sale> sales = saleService.getSalesForEmployee(currentEmployee);
        
        if (search != null && !search.isEmpty()) {
            sales = sales.stream()
                    .filter(s -> (s.getCustomer() != null && s.getCustomer().getContactPerson().toLowerCase().contains(search.toLowerCase())) ||
                            (s.getProduct() != null && s.getProduct().getName().toLowerCase().contains(search.toLowerCase())) ||
                            (s.getProductName() != null && s.getProductName().toLowerCase().contains(search.toLowerCase())))
                    .toList();
        }
        
        // Load payment information for each sale to avoid lazy loading issues
        // This ensures payments are loaded before the view tries to access them
        for (Sale sale : sales) {
            try {
                List<Payment> payments = saleService.getPaymentsBySale(sale.getId());
                sale.setPayments(payments);
            } catch (Exception e) {
                // If payments can't be loaded, set empty list
                sale.setPayments(java.util.Collections.emptyList());
            }
        }
        
        model.addAttribute("sales", sales);
        model.addAttribute("search", search);
        model.addAttribute("currentEmployee", currentEmployee);
        return "employee/sales/list";
    }

    @GetMapping("/new")
    public String showSaleForm(Model model) {
        Employee currentEmployee = getCurrentEmployee();
        Sale sale = new Sale();
        sale.setCreatedById(currentEmployee.getId());
        sale.setSaleDate(LocalDate.now());
        
        // Get accessible customers
        List<Customer> customers = customerService.getCustomersForEmployee(currentEmployee);
        List<Product> products = productService.getActiveProducts();
        
        model.addAttribute("sale", sale);
        model.addAttribute("customers", customers);
        model.addAttribute("products", products);
        model.addAttribute("currentEmployee", currentEmployee);
        return "employee/sales/form";
    }

    @GetMapping("/view/{id}")
    public String viewSale(@PathVariable Long id, Model model) {
        Employee currentEmployee = getCurrentEmployee();
        Sale sale = saleService.getSaleById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid sale ID: " + id));

        // Verify the current employee has access to this sale based on hierarchy
        if (!canEmployeeAccessSale(currentEmployee, sale)) {
            throw new RuntimeException("You do not have access to this sale");
        }
        
        List<Payment> payments = saleService.getPaymentsBySale(id);
        BigDecimal totalPaid = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Get commission information for this sale
        List<com.app.model.Commission> commissions = commissionService.getCommissionRepository().findBySale(sale);

        // Get the employee who created this sale
        Employee createdByEmployee = employeeService.getEmployeeById(sale.getCreatedById())
                .orElse(null);

        // Check if current user can approve commissions for this sale
        boolean canApproveCommissions = commissionService.canShowApproveButtonForSale(sale, currentEmployee);

        model.addAttribute("sale", sale);
        model.addAttribute("payments", payments);
        model.addAttribute("commissions", commissions);
        model.addAttribute("createdByEmployee", createdByEmployee);
        model.addAttribute("totalPaid", totalPaid);
        model.addAttribute("remainingAmount", sale.getTotalAmount().subtract(totalPaid));
        model.addAttribute("currentEmployee", currentEmployee);
        model.addAttribute("canApproveCommissions", canApproveCommissions);
        return "employee/sales/view";
    }

    @PostMapping("/save")
    public String saveSale(@ModelAttribute Sale sale,
                          @RequestParam(required = false) Long customerId,
                          @RequestParam(required = false) Long productId,
                          @RequestParam(required = false) String paymentStatus,
                          @RequestParam(required = false) BigDecimal paymentAmount,
                          @RequestParam(required = false) String transactionMode,
                          @RequestParam(required = false) String transactionId,
                          @RequestParam(required = false) String paymentNotes,
                          @RequestParam(required = false) java.time.LocalDate dueDate,
                          BindingResult result,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        Employee currentEmployee = getCurrentEmployee();
        
        // Set created by
        sale.setCreatedById(currentEmployee.getId());

        // Set hierarchy fields (promoter, zonal head, cluster head, ASM)
        employeeService.setSaleHierarchyFields(sale, currentEmployee);

        // Set customer
        if (customerId != null) {
            Customer customer = customerService.getCustomerById(customerId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid customer ID"));
            
            // Verify customer is accessible
            List<Customer> accessibleCustomers = customerService.getCustomersForEmployee(currentEmployee);
            if (!accessibleCustomers.contains(customer)) {
                result.rejectValue("customer", "error.sale", "You do not have access to this customer");
                List<Customer> customers = customerService.getCustomersForEmployee(currentEmployee);
                List<Product> products = productService.getActiveProducts();
                model.addAttribute("customers", customers);
                model.addAttribute("products", products);
                model.addAttribute("currentEmployee", currentEmployee);
                return "employee/sales/form";
            }
            sale.setCustomer(customer);
        } else {
            result.rejectValue("customer", "error.sale", "Customer is required");
        }
        
        // Set product if provided
        if (productId != null) {
            Product product = productService.getProductById(productId).orElse(null);
            sale.setProduct(product);
            if (product != null) {
                sale.setProductName(product.getName());
            }
        }
        
        // Validate sale object
        if (result.hasErrors() || sale.getCustomer() == null || sale.getCreatedById() == null) {
            List<Customer> customers = customerService.getCustomersForEmployee(currentEmployee);
            List<Product> products = productService.getActiveProducts();
            model.addAttribute("customers", customers);
            model.addAttribute("products", products);
            model.addAttribute("currentEmployee", currentEmployee);
            return "employee/sales/form";
        }
        
        // Set initial payment status
        if (paymentStatus != null && ("COMPLETED".equals(paymentStatus) || "PARTIAL".equals(paymentStatus) || "PENDING".equals(paymentStatus))) {
            sale.setPaymentStatus(paymentStatus);
        } else {
            sale.setPaymentStatus("PENDING");
        }

        // Set initial sale status
        sale.setSaleStatus("IN_PROGRESS");
        
        // Set due date if provided
        if (dueDate != null && !"COMPLETED".equals(paymentStatus)) {
            sale.setDueDate(dueDate);
        }
        
        // Save the sale first
        Sale savedSale = saleService.saveSale(sale);
        
        // Add payment if provided
        if (paymentAmount != null && paymentAmount.compareTo(BigDecimal.ZERO) > 0 && transactionMode != null && !transactionMode.isEmpty()) {
            // Validate transaction ID uniqueness if provided
            if (transactionId != null && !transactionId.trim().isEmpty()) {
                Payment existingPayment = paymentRepository.findByTransactionId(transactionId.trim());
                if (existingPayment != null) {
                    redirectAttributes.addFlashAttribute("error", "Sale saved but payment could not be added: Duplicate transaction reference. This transaction ID has already been used for another payment.");
                    return "redirect:/employee/sales";
                }
            }
            
            try {
                saleService.addPayment(savedSale.getId(), paymentAmount, transactionMode, transactionId, paymentNotes, dueDate);
            } catch (IllegalArgumentException e) {
                redirectAttributes.addFlashAttribute("error", "Sale saved but payment could not be added: " + e.getMessage());
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Sale saved but payment could not be added: " + e.getMessage());
            }
        }
        
        redirectAttributes.addFlashAttribute("message", "Sale saved successfully!");
        return "redirect:/employee/sales";
    }

    @GetMapping("/{id}/payments/add")
    public String showAddPaymentForm(@PathVariable Long id, Model model) {
        Employee currentEmployee = getCurrentEmployee();
        Sale sale = saleService.getSaleById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid sale ID: " + id));

        // Verify the current employee has access to this sale based on hierarchy
        if (!canEmployeeAccessSale(currentEmployee, sale)) {
            throw new RuntimeException("You do not have access to this sale");
        }
        
        List<Payment> payments = saleService.getPaymentsBySale(id);
        BigDecimal totalPaid = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remaining = sale.getTotalAmount().subtract(totalPaid);
        
        model.addAttribute("sale", sale);
        model.addAttribute("totalPaid", totalPaid);
        model.addAttribute("remainingAmount", remaining);
        model.addAttribute("existingPayments", payments); // Add payment history for display
        model.addAttribute("currentEmployee", currentEmployee);
        return "employee/sales/add-payment";
    }

    @PostMapping("/{id}/payments/add")
    public String addPayment(@PathVariable Long id,
                            @RequestParam BigDecimal amount,
                            @RequestParam String transactionMode,
                            @RequestParam(required = false) String transactionId,
                            @RequestParam(required = false) String notes,
                            @RequestParam(required = false) java.time.LocalDate dueDate,
                            RedirectAttributes redirectAttributes) {
        Employee currentEmployee = getCurrentEmployee();
        Sale sale = saleService.getSaleById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid sale ID: " + id));

        // Verify the current employee has access to this sale based on hierarchy
        if (!canEmployeeAccessSale(currentEmployee, sale)) {
            throw new RuntimeException("You do not have access to this sale");
        }
        
        // Check if payment amount exceeds remaining amount
        List<Payment> existingPayments = saleService.getPaymentsBySale(id);
        BigDecimal totalPaid = existingPayments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remaining = sale.getTotalAmount().subtract(totalPaid);
        
        if (amount.compareTo(remaining) > 0) {
            redirectAttributes.addFlashAttribute("error", "Payment amount cannot exceed remaining amount: ₹" + remaining);
            return "redirect:/employee/sales/" + id + "/payments/add";
        }
        
        // Validate transaction mode
        if (transactionMode == null || transactionMode.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Transaction mode is required");
            return "redirect:/employee/sales/" + id + "/payments/add";
        }
        
        // Validate transaction ID uniqueness if provided
        if (transactionId != null && !transactionId.trim().isEmpty()) {
            Payment existingPayment = paymentRepository.findByTransactionId(transactionId.trim());
            if (existingPayment != null) {
                redirectAttributes.addFlashAttribute("error", "Duplicate transaction reference. This transaction ID has already been used for another payment.");
                return "redirect:/employee/sales/" + id + "/payments/add";
            }
        }
        
        try {
            saleService.addPayment(id, amount, transactionMode, transactionId, notes, dueDate);
            redirectAttributes.addFlashAttribute("message", "Payment added successfully!");
            return "redirect:/employee/sales/view/" + id;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/employee/sales/" + id + "/payments/add";
        }
    }

    @PostMapping("/approve-commission/{commissionId}")
    public String approveCommission(@PathVariable Long commissionId,
                                   @RequestParam Long saleId,
                                   RedirectAttributes redirectAttributes) {
        Employee currentEmployee = getCurrentEmployee();

        try {
            commissionService.approveCommission(commissionId, currentEmployee);
            redirectAttributes.addFlashAttribute("message", "Commission approved successfully!");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to approve commission: " + e.getMessage());
        }

        return "redirect:/employee/sales/view/" + saleId;
    }

    /**
     * Helper method to check if an employee can access a specific sale based on hierarchy.
     */
    private boolean canEmployeeAccessSale(Employee employee, Sale sale) {
        if (employee == null || sale == null) {
            return false;
        }

        // Check if the sale is in the employee's accessible sales list
        List<Sale> accessibleSales = saleService.getSalesForEmployee(employee);
        return accessibleSales.stream().anyMatch(s -> s.getId().equals(sale.getId()));
    }
}
