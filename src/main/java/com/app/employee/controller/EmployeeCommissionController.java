package com.app.employee.controller;

import com.app.model.Commission;
import com.app.model.Employee;
import com.app.service.CommissionService;
import com.app.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/employee/commissions")
public class EmployeeCommissionController {

    @Autowired
    private CommissionService commissionService;

    @Autowired
    private EmployeeService employeeService;

    private Employee getCurrentEmployee() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return employeeService.getEmployeeByUsername(username)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
    }

    @GetMapping
    public String listCommissions(Model model) {
        Employee currentEmployee = getCurrentEmployee();

        List<Commission> commissions = commissionService.getCommissionsByEmployee(currentEmployee);
        BigDecimal totalApproved = commissionService.getTotalApprovedCommissionForEmployee(currentEmployee);

        model.addAttribute("commissions", commissions);
        model.addAttribute("totalApproved", totalApproved);
        model.addAttribute("currentEmployee", currentEmployee);

        return "employee/commissions/list";
    }

    @GetMapping("/{id}")
    public String viewCommission(@PathVariable Long id, Model model) {
        Employee currentEmployee = getCurrentEmployee();
        Commission commission = commissionService.getCommissionById(id);

        // Verify commission belongs to current employee
        if (!commission.getEmployee().getId().equals(currentEmployee.getId())) {
            throw new RuntimeException("You do not have access to this commission");
        }

        model.addAttribute("commission", commission);
        model.addAttribute("currentEmployee", currentEmployee);

        return "employee/commissions/view";
    }
}