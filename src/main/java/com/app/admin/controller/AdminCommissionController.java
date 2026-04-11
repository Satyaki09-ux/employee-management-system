package com.app.admin.controller;

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

import java.util.List;

@Controller
@RequestMapping("/admin/commissions")
public class AdminCommissionController {

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
    public String listAllCommissions(Model model) {
        List<Commission> allCommissions = commissionService.getCommissionRepository().findAll();
        model.addAttribute("commissions", allCommissions);
        return "admin/commissions/list";
    }

    @GetMapping("/pending")
    public String listPendingCommissions(Model model) {
        Employee currentAdmin = getCurrentEmployee();
        List<Commission> pendingCommissions = commissionService.getPendingCommissionsForManager(currentAdmin);
        model.addAttribute("commissions", pendingCommissions);
        return "admin/commissions/pending";
    }

    @PostMapping("/{id}/approve")
    public String approveCommission(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Employee currentAdmin = getCurrentEmployee();
            Commission commission = commissionService.approveCommission(id, currentAdmin);
            redirectAttributes.addFlashAttribute("message",
                "Commission of ₹" + commission.getAmount() + " approved for " + commission.getEmployee().getFullName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to approve commission: " + e.getMessage());
        }
        return "redirect:/admin/commissions/pending";
    }

    @PostMapping("/{id}/reject")
    public String rejectCommission(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Employee currentAdmin = getCurrentEmployee();
            Commission commission = commissionService.rejectCommission(id, currentAdmin);
            redirectAttributes.addFlashAttribute("message",
                "Commission rejected for " + commission.getEmployee().getFullName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to reject commission: " + e.getMessage());
        }
        return "redirect:/admin/commissions/pending";
    }

    @GetMapping("/{id}")
    public String viewCommission(@PathVariable Long id, Model model) {
        Commission commission = commissionService.getCommissionById(id);
        model.addAttribute("commission", commission);
        return "admin/commissions/view";
    }
}