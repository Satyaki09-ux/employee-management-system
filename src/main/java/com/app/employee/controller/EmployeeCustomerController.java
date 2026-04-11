package com.app.employee.controller;

import com.app.config.AuditHelper;
import com.app.model.Customer;
import com.app.model.Employee;
import com.app.service.CustomerService;
import com.app.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/employee/customers")
public class EmployeeCustomerController {

    @Autowired
    private CustomerService customerService;
    
    @Autowired
    private EmployeeService employeeService;

    private Employee getCurrentEmployee() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return employeeService.getEmployeeByUsername(username)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
    }

    @GetMapping
    public String listCustomers(Model model, @RequestParam(required = false) String search) {
        Employee currentEmployee = getCurrentEmployee();
        List<Customer> customers;
        
        if (search != null && !search.isEmpty()) {
            // Get all customers for the employee and filter by search
            List<Customer> allCustomers = customerService.getCustomersForEmployee(currentEmployee);
            customers = allCustomers.stream()
                    .filter(c -> c.getContactPerson().toLowerCase().contains(search.toLowerCase()) ||
                            c.getEmail().toLowerCase().contains(search.toLowerCase()) ||
                            (c.getPhone() != null && c.getPhone().contains(search)))
                    .toList();
        } else {
            customers = customerService.getCustomersForEmployee(currentEmployee);
        }
        
        model.addAttribute("customers", customers);
        model.addAttribute("search", search);
        model.addAttribute("currentEmployee", currentEmployee);
        return "employee/customers/list";
    }

    @GetMapping("/new")
    public String showCustomerForm(Model model) {
        Employee currentEmployee = getCurrentEmployee();
        Customer customer = new Customer();
        model.addAttribute("customer", customer);
        model.addAttribute("currentEmployee", currentEmployee);
        return "employee/customers/form";
    }

    @GetMapping("/view/{id}")
    public String viewCustomer(@PathVariable Long id, Model model) {
        Employee currentEmployee = getCurrentEmployee();
        Customer customer = customerService.getCustomerById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid customer ID: " + id));
        
        // Verify the customer is accessible to this employee
        List<Customer> accessibleCustomers = customerService.getCustomersForEmployee(currentEmployee);
        if (!accessibleCustomers.contains(customer)) {
            throw new RuntimeException("You do not have access to this customer");
        }
        
        // Load employee information for hierarchy display
        model.addAttribute("customer", customer);
        model.addAttribute("currentEmployee", currentEmployee);
        if (customer.getPromoterId() != null) {
            employeeService.getEmployeeById(customer.getPromoterId())
                    .ifPresent(emp -> model.addAttribute("promoter", emp));
        }
        if (customer.getZonalHeadId() != null) {
            employeeService.getEmployeeById(customer.getZonalHeadId())
                    .ifPresent(emp -> model.addAttribute("zonalHead", emp));
        }
        if (customer.getClusterHeadId() != null) {
            employeeService.getEmployeeById(customer.getClusterHeadId())
                    .ifPresent(emp -> model.addAttribute("clusterHead", emp));
        }
        if (customer.getAreaSalesManagerId() != null) {
            employeeService.getEmployeeById(customer.getAreaSalesManagerId())
                    .ifPresent(emp -> model.addAttribute("areaSalesManager", emp));
        }

        // Check if current employee has direct reports (is a manager)
        List<Employee> directReports = employeeService.getDirectReports(currentEmployee);
        if (!directReports.isEmpty()) {
            model.addAttribute("directReports", directReports);
            model.addAttribute("isManager", true);
        } else {
            model.addAttribute("isManager", false);
        }

        return "employee/customers/view";
    }

    @PostMapping("/assign/{customerId}")
    public String assignCustomer(@PathVariable Long customerId,
                                @RequestParam Long employeeId,
                                RedirectAttributes redirectAttributes) {
        try {
            Employee currentEmployee = getCurrentEmployee();

            // Verify the customer exists and is accessible
            Customer customer = customerService.getCustomerById(customerId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid customer ID: " + customerId));

            List<Customer> accessibleCustomers = customerService.getCustomersForEmployee(currentEmployee);
            if (!accessibleCustomers.contains(customer)) {
                redirectAttributes.addFlashAttribute("error", "You do not have access to this customer");
                return "redirect:/employee/customers/view/" + customerId;
            }

            // Verify the target employee is a direct report
            Employee targetEmployee = employeeService.getEmployeeById(employeeId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid employee ID: " + employeeId));

            List<Employee> directReports = employeeService.getDirectReports(currentEmployee);
            if (!directReports.contains(targetEmployee)) {
                redirectAttributes.addFlashAttribute("error", "You can only assign customers to your direct reports");
                return "redirect:/employee/customers/view/" + customerId;
            }

            // Assign the customer to the target employee using the hierarchy assignment logic
            customerService.assignCustomerToEmployee(customer, targetEmployee, currentEmployee);

            redirectAttributes.addFlashAttribute("message",
                "Customer successfully assigned to " + targetEmployee.getFullName());

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error assigning customer: " + e.getMessage());
        }

        return "redirect:/employee/customers/view/" + customerId;
    }

    @GetMapping("/edit/{id}")
    public String editCustomer(@PathVariable Long id, Model model) {
        Employee currentEmployee = getCurrentEmployee();
        Customer customer = customerService.getCustomerById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid customer ID: " + id));
        
        // Verify the customer is accessible to this employee
        List<Customer> accessibleCustomers = customerService.getCustomersForEmployee(currentEmployee);
        if (!accessibleCustomers.contains(customer)) {
            throw new RuntimeException("You do not have access to this customer");
        }
        
        model.addAttribute("customer", customer);
        model.addAttribute("currentEmployee", currentEmployee);
        return "employee/customers/form";
    }

    @PostMapping("/save")
    public String saveCustomer(@Valid @ModelAttribute Customer customer,
                              BindingResult result,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        Employee currentEmployee = getCurrentEmployee();
        
        if (result.hasErrors()) {
            model.addAttribute("currentEmployee", currentEmployee);
            return "employee/customers/form";
        }
        
        // Check for duplicate email+phone combination
        if (customer.getId() == null) {
            if (customerService.emailAndPhoneExists(customer.getEmail(), customer.getPhone())) {
                result.rejectValue("email", "error.customer", "A customer with this email and phone number combination already exists");
                result.rejectValue("phone", "error.customer", "A customer with this email and phone number combination already exists");
                model.addAttribute("currentEmployee", currentEmployee);
                return "employee/customers/form";
            }
        } else {
            if (customerService.emailAndPhoneExistsForOtherCustomer(customer.getEmail(), customer.getPhone(), customer.getId())) {
                result.rejectValue("email", "error.customer", "A customer with this email and phone number combination already exists");
                result.rejectValue("phone", "error.customer", "A customer with this email and phone number combination already exists");
                model.addAttribute("currentEmployee", currentEmployee);
                return "employee/customers/form";
            }
            
            // Verify the customer is accessible to this employee
            List<Customer> accessibleCustomers = customerService.getCustomersForEmployee(currentEmployee);
            Customer existingCustomer = customerService.getCustomerById(customer.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid customer ID"));
            if (!accessibleCustomers.contains(existingCustomer)) {
                throw new RuntimeException("You do not have access to this customer");
            }
        }
        
        // Save customer with hierarchy fields populated based on current employee
        String audit = AuditHelper.currentUserAuditString();
        if (customer.getId() == null) {
            // New customer - populate hierarchy fields
            customer.setCreatedBy(audit);
            customerService.saveCustomer(customer, currentEmployee);
        } else {
            // Existing customer - preserve hierarchy fields and audit info
            customerService.getCustomerById(customer.getId())
                    .ifPresent(existing -> customer.setCreatedBy(existing.getCreatedBy()));
            customer.setLastUpdatedBy(audit);
            customerService.saveCustomer(customer);
        }
        redirectAttributes.addFlashAttribute("message", "Customer saved successfully!");
        return "redirect:/employee/customers";
    }

    // Delete functionality removed - customers cannot be deleted from employee portal
    // @GetMapping("/delete/{id}")
    // public String deleteCustomer(@PathVariable Long id, RedirectAttributes redirectAttributes) {
    //     Employee currentEmployee = getCurrentEmployee();
    //     Customer customer = customerService.getCustomerById(id)
    //             .orElseThrow(() -> new IllegalArgumentException("Invalid customer ID: " + id));
    //     
    //     // Verify the customer is accessible to this employee
    //     List<Customer> accessibleCustomers = customerService.getCustomersForEmployee(currentEmployee);
    //     if (!accessibleCustomers.contains(customer)) {
    //         throw new RuntimeException("You do not have access to this customer");
    //     }
    //     
    //     customerService.deleteCustomer(id);
    //     redirectAttributes.addFlashAttribute("message", "Customer deleted successfully!");
    //     return "redirect:/employee/customers";
    // }
}
