package com.app.employee.controller;

import com.app.model.SalesTarget;
import com.app.model.Employee;
import com.app.model.Product;
import com.app.service.SalesTargetService;
import com.app.service.EmployeeService;
import com.app.service.ProductService;
import jakarta.validation.Valid;
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
import java.util.Optional;

@Controller
@RequestMapping("/employee/sales-targets")
public class EmployeeSalesTargetController {
    
    @Autowired
    private SalesTargetService salesTargetService;
    
    @Autowired
    private EmployeeService employeeService;
    
    @Autowired
    private ProductService productService;
    
    private Employee getCurrentEmployee() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return employeeService.getEmployeeByUsername(username)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
    }
    
    /**
     * Lists all direct reports for the current employee
     */
    @GetMapping
    public String listDirectReports(Model model) {
        Employee currentEmployee = getCurrentEmployee();
        
        // Get all direct reports (employees reporting to current employee)
//        List<Employee> directReports = employeeService.getAllEmployees().stream()
//                .filter(e -> e.getReportingManager() != null &&
//                           e.getReportingManager().getId().equals(currentEmployee.getId()))
//                .toList();
        List<Employee> directReports = employeeService.getAllReportingEmployees(currentEmployee);
        
        model.addAttribute("currentEmployee", currentEmployee);
        model.addAttribute("directReports", directReports);
        return "employee/sales-targets/list";
    }
    
    /**
     * Shows form to set sales target for a specific employee (direct report)
     */
    @GetMapping("/set/{employeeId}")
    public String showSetTargetForm(@PathVariable Long employeeId, Model model) {
        Employee currentEmployee = getCurrentEmployee();
        Employee targetEmployee = employeeService.getEmployeeById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid employee ID: " + employeeId));
        
        // Verify the employee is a direct report
//        if (targetEmployee.getReportingManager() == null ||
//            !targetEmployee.getReportingManager().getId().equals(currentEmployee.getId())) {
//            throw new RuntimeException("You can only set sales targets for your direct reports");
//        }

        List<Long> allowedIds = employeeService.getAllReportingEmployeeIds(currentEmployee);

        if (!allowedIds.contains(targetEmployee.getId())) {
            throw new RuntimeException("You can only set sales targets for employees in your hierarchy");
        }
        
        // Get all active products
        List<Product> products = productService.getActiveProducts();
        
        // Get current employee's sales targets for reference (max limits)
        List<SalesTarget> managerTargets = salesTargetService.getSalesTargetsByEmployee(currentEmployee);
        
        model.addAttribute("currentEmployee", currentEmployee);
        model.addAttribute("targetEmployee", targetEmployee);
        model.addAttribute("products", products);
        model.addAttribute("managerTargets", managerTargets);
        return "employee/sales-targets/form";
    }
    
    /**
     * Saves sales target for a direct report
     */
    @PostMapping("/save")
    public String saveSalesTarget(@RequestParam Long employeeId,
                                 @RequestParam Long productId,
                                 @RequestParam Integer targetUnits,
                                 @RequestParam java.time.LocalDate periodStart,
                                 @RequestParam java.time.LocalDate periodEnd,
                                 RedirectAttributes redirectAttributes) {
        Employee currentEmployee = getCurrentEmployee();
        Employee targetEmployee = employeeService.getEmployeeById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid employee ID: " + employeeId));
        
        // Verify the employee is a direct report
//        if (targetEmployee.getReportingManager() == null ||
//            !targetEmployee.getReportingManager().getId().equals(currentEmployee.getId())) {
//            redirectAttributes.addFlashAttribute("error", "You can only set sales targets for your direct reports");
//            return "redirect:/employee/sales-targets";
//        }

        List<Long> allowedIds = employeeService.getAllReportingEmployeeIds(currentEmployee);

        if (!allowedIds.contains(targetEmployee.getId())) {
            redirectAttributes.addFlashAttribute("error", "You can only set sales targets for employees in your hierarchy");
            return "redirect:/employee/sales-targets";
        }
        
        // Validate target units (minimum 0)
        if (targetUnits == null || targetUnits < 0) {
            redirectAttributes.addFlashAttribute("error", "Target units must be at least 0");
            return "redirect:/employee/sales-targets/set/" + employeeId;
        }
        
        // Get product (required for employee portal)
        if (productId == null) {
            redirectAttributes.addFlashAttribute("error", "Product is required");
            return "redirect:/employee/sales-targets/set/" + employeeId;
        }
        
        Product product = productService.getProductById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid product ID: " + productId));
        
        // Check if manager has a target for this product in the same period (for max validation)
        Optional<SalesTarget> managerTarget = salesTargetService.getSalesTargetsByEmployee(currentEmployee).stream()
                .filter(t -> t.getProduct() != null && t.getProduct().getId().equals(productId))
                .filter(t -> !t.getPeriodStart().isAfter(periodEnd) && !t.getPeriodEnd().isBefore(periodStart))
                .findFirst();
        
        // If manager has a target, validate that employee target units don't exceed manager's units
        if (managerTarget.isPresent()) {
            Integer managerUnits = managerTarget.get().getTargetUnits();
            if (managerUnits != null && targetUnits > managerUnits) {
                redirectAttributes.addFlashAttribute("error", 
                    "Target units cannot exceed your target for this product: " + managerUnits + " units");
                return "redirect:/employee/sales-targets/set/" + employeeId;
            }
        }
        
        // Check if target already exists for this employee, product, and period
        List<SalesTarget> existingTargets = salesTargetService.getSalesTargetsByEmployee(targetEmployee);
        Optional<SalesTarget> existingTarget = existingTargets.stream()
                .filter(t -> t.getProduct() != null && t.getProduct().getId().equals(productId))
                .filter(t -> !t.getPeriodStart().isAfter(periodEnd) && !t.getPeriodEnd().isBefore(periodStart))
                .findFirst();
        
        SalesTarget salesTarget;
        if (existingTarget.isPresent()) {
            // Update existing target
            salesTarget = existingTarget.get();
            salesTarget.setTargetUnits(targetUnits);
            salesTarget.setProduct(product); // Ensure product is set for calculation
            salesTarget.setPeriodStart(periodStart);
            salesTarget.setPeriodEnd(periodEnd);
        } else {
            // Create new target
            salesTarget = new SalesTarget();
            salesTarget.setEmployee(targetEmployee);
            salesTarget.setProduct(product);
            salesTarget.setTargetUnits(targetUnits);
            salesTarget.setPeriodStart(periodStart);
            salesTarget.setPeriodEnd(periodEnd);
            salesTarget.setAchievedAmount(BigDecimal.ZERO);
        }
        
        salesTargetService.saveSalesTarget(salesTarget);
        salesTargetService.updateAchievedAmount(salesTarget);
        
        redirectAttributes.addFlashAttribute("message", 
            "Sales target set successfully for " + targetEmployee.getFullName() + " - " + product.getName() + " (" + targetUnits + " units)");
        return "redirect:/employee/sales-targets/view/" + employeeId;
    }
    
    /**
     * Views sales targets for a specific direct report
     */
    @GetMapping("/view/{employeeId}")
    public String viewEmployeeTargets(@PathVariable Long employeeId, Model model) {
        Employee currentEmployee = getCurrentEmployee();
        Employee targetEmployee = employeeService.getEmployeeById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid employee ID: " + employeeId));
        
        // Verify the employee is a direct report
//        if (targetEmployee.getReportingManager() == null ||
//            !targetEmployee.getReportingManager().getId().equals(currentEmployee.getId())) {
//            throw new RuntimeException("You can only view sales targets for your direct reports");
//        }

        List<Long> allowedIds = employeeService.getAllReportingEmployeeIds(currentEmployee);

        if (!allowedIds.contains(targetEmployee.getId())) {
            throw new RuntimeException("You can only view sales targets for employees in your hierarchy");
        }
        
//        List<SalesTarget> targets = salesTargetService.getSalesTargetsByEmployee(targetEmployee);
//        List<SalesTarget> targets = salesTargetService.getSalesTargetsForEmployeeHierarchy(targetEmployee);

        List<SalesTarget> targets = salesTargetService.getSalesTargetsByEmployee(targetEmployee);
        
        model.addAttribute("currentEmployee", currentEmployee);
        model.addAttribute("targetEmployee", targetEmployee);
        model.addAttribute("targets", targets);
        return "employee/sales-targets/view";
    }
}
