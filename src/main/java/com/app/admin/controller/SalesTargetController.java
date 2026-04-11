package com.app.admin.controller;

import com.app.model.SalesTarget;
import com.app.model.Employee;
import com.app.model.Product;
import com.app.service.SalesTargetService;
import com.app.service.EmployeeService;
import com.app.service.ProductService;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/sales-targets")
public class SalesTargetController {
    
    @Autowired
    private SalesTargetService salesTargetService;
    
    @Autowired
    private EmployeeService employeeService;
    
    @Autowired
    private ProductService productService;
    
    @GetMapping
    public String listSalesTargets(Model model,
                                   @RequestParam(required = false) String hierarchyLevel,
                                   @RequestParam(required = false) String search) {
        List<Employee> employees = employeeService.getActiveEmployees();
        
        // Filter by hierarchy level if provided
        if (hierarchyLevel != null && !hierarchyLevel.isEmpty()) {
            employees = employees.stream()
                    .filter(emp -> emp.getHierarchyLevel() != null && emp.getHierarchyLevel().toString().equals(hierarchyLevel))
                    .collect(Collectors.toList());
            model.addAttribute("selectedHierarchyLevel", hierarchyLevel);
        }
        
        // Filter by search query if provided
        if (search != null && !search.isEmpty()) {
            final String searchLower = search.toLowerCase();
            employees = employees.stream()
                    .filter(emp -> 
                        (emp.getFullName() != null && emp.getFullName().toLowerCase().contains(searchLower)) ||
                        (emp.getUsername() != null && emp.getUsername().toLowerCase().contains(searchLower)) ||
                        (emp.getEmail() != null && emp.getEmail().toLowerCase().contains(searchLower))
                    )
                    .collect(Collectors.toList());
            model.addAttribute("searchQuery", search);
        }
        
        model.addAttribute("employees", employees);
        return "admin/sales-targets/list";
    }
    
    @GetMapping("/set/{id}")
    public String showSetTargetForm(@PathVariable Long id, Model model) {
        Employee targetEmployee = employeeService.getEmployeeById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid employee ID: " + id));
        
        // Set default period to current month
        LocalDate now = LocalDate.now();
        LocalDate periodStart = now.withDayOfMonth(1);
        LocalDate periodEnd = now.withDayOfMonth(now.lengthOfMonth());
        
        model.addAttribute("targetEmployee", targetEmployee);
        model.addAttribute("products", productService.getActiveProducts());
        model.addAttribute("defaultPeriodStart", periodStart);
        model.addAttribute("defaultPeriodEnd", periodEnd);
        return "admin/sales-targets/form";
    }
    
    @PostMapping("/save")
    public String saveSalesTarget(@RequestParam Long employeeId,
                                 @RequestParam Long productId,
                                 @RequestParam Integer targetUnits,
                                 @RequestParam java.time.LocalDate periodStart,
                                 @RequestParam java.time.LocalDate periodEnd,
                                 @RequestParam BigDecimal baseSalary,
                                 @RequestParam BigDecimal commissionRate,
                                 RedirectAttributes redirectAttributes) {
        // Validate target units (minimum 0)
        if (targetUnits == null || targetUnits < 0) {
            redirectAttributes.addFlashAttribute("error", "Target units must be at least 0");
            return "redirect:/admin/sales-targets/set/" + employeeId;
        }
        
        // Get employee
        Employee employee = employeeService.getEmployeeById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid employee ID: " + employeeId));
        
        // Get product (required)
        if (productId == null) {
            redirectAttributes.addFlashAttribute("error", "Product is required");
            return "redirect:/admin/sales-targets/set/" + employeeId;
        }
        
        Product product = productService.getProductById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid product ID: " + productId));
        
        // Check if target already exists for this employee, product, and period
        List<SalesTarget> existingTargets = salesTargetService.getSalesTargetsByEmployee(employee);
        Optional<SalesTarget> existingTarget = existingTargets.stream()
                .filter(t -> t.getProduct() != null && t.getProduct().getId().equals(productId))
                .filter(t -> !t.getPeriodStart().isAfter(periodEnd) && !t.getPeriodEnd().isBefore(periodStart))
                .findFirst();
        
        SalesTarget salesTarget;
        if (existingTarget.isPresent()) {
            // Update existing target
            salesTarget = existingTarget.get();
            salesTarget.setTargetUnits(targetUnits);
            salesTarget.setProduct(product);
            salesTarget.setPeriodStart(periodStart);
            salesTarget.setPeriodEnd(periodEnd);
            salesTarget.setBaseSalary(baseSalary);
            salesTarget.setCommissionRate(commissionRate);
        } else {
            // Create new target
            salesTarget = new SalesTarget();
            salesTarget.setEmployee(employee);
            salesTarget.setProduct(product);
            salesTarget.setTargetUnits(targetUnits);
            salesTarget.setPeriodStart(periodStart);
            salesTarget.setPeriodEnd(periodEnd);
            salesTarget.setBaseSalary(baseSalary);
            salesTarget.setCommissionRate(commissionRate);
            salesTarget.setAchievedAmount(BigDecimal.ZERO);
        }
        
        salesTargetService.saveSalesTarget(salesTarget);
        salesTargetService.updateAchievedAmount(salesTarget);
        
        redirectAttributes.addFlashAttribute("message", 
            "Sales target set successfully for " + employee.getFullName() + " - " + product.getName() + " (" + targetUnits + " units)");
        return "redirect:/admin/sales-targets/view/" + employeeId;
    }
    
    @GetMapping("/delete/{id}")
    public String deleteSalesTarget(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        salesTargetService.deleteSalesTarget(id);
        redirectAttributes.addFlashAttribute("message", "Sales target deleted successfully!");
        return "redirect:/admin/sales-targets";
    }
    
    
    @GetMapping("/view/{id}")
    public String viewEmployeeTargets(@PathVariable Long id, Model model) {
        Employee targetEmployee = employeeService.getEmployeeById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid employee ID: " + id));
        
        List<SalesTarget> targets = salesTargetService.getSalesTargetsByEmployee(targetEmployee);
        
        model.addAttribute("targetEmployee", targetEmployee);
        model.addAttribute("targets", targets);
        return "admin/sales-targets/view";
    }
}
