package com.app.employee.controller;

import com.app.model.Employee;
import com.app.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/employee")
public class EmployeeProfileController {
    
    @Autowired
    private EmployeeService employeeService;
    
    private Employee getCurrentEmployee() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Employee employee = employeeService.getEmployeeByUsername(username)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        return employee;
    }
    
    @GetMapping("/reset-password")
    public String showResetPasswordForm(Model model) {
        Employee employee = getCurrentEmployee();
        model.addAttribute("employee", employee);
        return "employee/reset-password";
    }
    
    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String currentPassword,
                               @RequestParam String newPassword,
                               @RequestParam String confirmPassword,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        Employee employee = getCurrentEmployee();
        
        // Validate passwords match
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "New password and confirm password do not match");
            model.addAttribute("employee", employee);
            return "employee/reset-password";
        }
        
        // Validate password length
        if (newPassword.length() < 6) {
            model.addAttribute("error", "New password must be at least 6 characters long");
            model.addAttribute("employee", employee);
            return "employee/reset-password";
        }
        
        // Verify current password
        if (!employeeService.verifyPassword(employee, currentPassword)) {
            model.addAttribute("error", "Current password is incorrect");
            model.addAttribute("employee", employee);
            return "employee/reset-password";
        }
        
        // Reset password
        employeeService.resetPassword(employee.getId(), newPassword);
        
        // Logout the user after password reset for security
        redirectAttributes.addFlashAttribute("message", "Password has been reset successfully. Please login again with your new password.");
        return "redirect:/logout";
    }
    
}
