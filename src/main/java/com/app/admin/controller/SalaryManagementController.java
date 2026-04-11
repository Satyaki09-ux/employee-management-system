package com.app.admin.controller;

import com.app.model.Employee;
import com.app.model.Salary;
import com.app.model.Commission;
import com.app.model.Admin;
import com.app.service.EmployeeService;
import com.app.service.CommissionService;
import com.app.service.SalaryService;
import com.app.service.SaleService;
import com.app.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/salary-management")
public class SalaryManagementController {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private CommissionService commissionService;
    
    @Autowired
    private SalaryService salaryService;
    
    @Autowired
    private SaleService saleService;
    
    @Autowired
    private AdminService adminService;

    @GetMapping
    public String listEmployeesForSalary(Model model,
                                       @RequestParam(required = false) String hierarchyLevel,
                                       @RequestParam(required = false) String search) {
        List<Employee> employees = employeeService.getActiveEmployees();

        // Filter by hierarchy level if provided
        if (hierarchyLevel != null && !hierarchyLevel.isEmpty()) {
            employees = employees.stream()
                    .filter(emp -> emp.getHierarchyLevel() != null && emp.getHierarchyLevel().toString().equals(hierarchyLevel))
                    .collect(java.util.stream.Collectors.toList());
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
                    .collect(java.util.stream.Collectors.toList());
            model.addAttribute("searchQuery", search);
        }

        model.addAttribute("employees", employees);
        return "admin/salary-management/list";
    }

    @GetMapping("/employee/{id}")
    public String viewEmployeeSalary(@PathVariable Long id, Model model) {
        Employee employee = employeeService.getEmployeeById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid employee ID: " + id));

        // Get approved commissions from commission table
        List<Commission> approvedCommissions = commissionService.getApprovedUnpaidCommissionsByEmployee(employee);
        
        // Calculate total approved commission amount
        BigDecimal totalApprovedCommission = approvedCommissions.stream()
                .map(Commission::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Get total sales for display
        YearMonth currentMonth = YearMonth.now();
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();
        BigDecimal totalSales = saleService.getTotalSalesByEmployee(employee.getId(), monthStart, monthEnd);
        
        // Get salary history for this employee
        List<Salary> salaryHistory = salaryService.getSalariesByEmployee(employee);

        model.addAttribute("employee", employee);
        model.addAttribute("approvedCommissions", approvedCommissions);
        model.addAttribute("totalApprovedCommission", totalApprovedCommission);
        model.addAttribute("totalSales", totalSales);
        model.addAttribute("salaryHistory", salaryHistory);
        
        // Set default date range to current month
        model.addAttribute("defaultStartDate", monthStart);
        model.addAttribute("defaultEndDate", monthEnd);

        return "admin/salary-management/employee-salary";
    }

    @PostMapping("/mark-commission-paid/{commissionId}")
    public String markCommissionAsPaid(@PathVariable Long commissionId,
                                      @RequestParam Long employeeId,
                                      @RequestParam(required = false) String paymentMethod,
                                      @RequestParam(required = false) String transactionReference,
                                      RedirectAttributes redirectAttributes) {
        try {
            // Get current admin
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            Optional<Admin> adminOpt = adminService.getAdminByUsername(username);
            
            if (adminOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Admin not found");
                return "redirect:/admin/salary-management/employee/" + employeeId;
            }
            
            // Use default payment method if not provided
            String finalPaymentMethod = (paymentMethod != null && !paymentMethod.isEmpty()) 
                    ? paymentMethod : "NEFT";
            
            commissionService.markCommissionAsPaid(commissionId, adminOpt.get(), finalPaymentMethod, transactionReference);
            redirectAttributes.addFlashAttribute("message", "Commission marked as paid successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error marking commission as paid: " + e.getMessage());
        }
        return "redirect:/admin/salary-management/employee/" + employeeId;
    }

    @PostMapping("/release-salary/{employeeId}")
    public String releaseEmployeeSalary(@PathVariable Long employeeId,
                                      @RequestParam LocalDate startDate,
                                      @RequestParam LocalDate endDate,
                                      @RequestParam BigDecimal basicSalary,
                                      @RequestParam(required = false) BigDecimal allowances,
                                      @RequestParam(required = false) BigDecimal deductions,
                                      @RequestParam(required = false) BigDecimal bonus,
                                      @RequestParam(required = false) BigDecimal overtime,
                                      @RequestParam BigDecimal netSalary,
                                      @RequestParam String transactionType,
                                      @RequestParam(required = false) String transactionReference,
                                      RedirectAttributes redirectAttributes) {
        try {
            Employee employee = employeeService.getEmployeeById(employeeId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid employee ID: " + employeeId));

            // Get current admin who is releasing the salary
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            Optional<Admin> adminOpt = adminService.getAdminByUsername(username);
            
            if (adminOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Admin not found");
                return "redirect:/admin/salary-management/employee/" + employeeId;
            }

            // Create salary entry
            Salary salary = new Salary();
            salary.setEmployee(employee);
            salary.setStartDate(startDate);
            salary.setEndDate(endDate);
            // Set salary month to the first day of the month based on start date
            salary.setSalaryMonth(startDate.withDayOfMonth(1));
            salary.setBasicSalary(basicSalary);
            salary.setAllowances(allowances != null ? allowances : BigDecimal.ZERO);
            salary.setDeductions(deductions != null ? deductions : BigDecimal.ZERO);
            salary.setBonus(bonus != null ? bonus : BigDecimal.ZERO);
            salary.setOvertime(overtime != null ? overtime : BigDecimal.ZERO);
            salary.setNetSalary(netSalary);
            salary.setTransactionType(transactionType);
            salary.setTransactionReference(transactionReference);
            salary.setStatus("PAID");
            salary.setPaidAt(java.time.LocalDateTime.now());
            salary.setReleasedBy(adminOpt.get());
            
            salaryService.saveSalary(salary);
            
            redirectAttributes.addFlashAttribute("message",
                "Salary released successfully for " + employee.getFullName() + ". Amount: ₹" + netSalary);

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error releasing salary: " + e.getMessage());
        }

        return "redirect:/admin/salary-management/employee/" + employeeId;
    }
}
