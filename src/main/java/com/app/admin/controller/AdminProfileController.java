package com.app.admin.controller;

import com.app.model.Admin;
import com.app.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminProfileController {
    
    @Autowired
    private AdminService adminService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    private Admin getCurrentAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return adminService.getAdminByUsername(username)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
    }
    
    @GetMapping("/profile")
    public String showProfile(Model model) {
        Admin admin = getCurrentAdmin();
        model.addAttribute("admin", admin);
        model.addAttribute("currentAdmin", admin);
        return "admin/profile";
    }
    
    @PostMapping("/profile/update")
    public String updateProfile(@ModelAttribute Admin admin,
                               RedirectAttributes redirectAttributes) {
        try {
            Admin currentAdmin = getCurrentAdmin();
            
            // Ensure we're updating the current admin's profile
            if (!currentAdmin.getId().equals(admin.getId())) {
                redirectAttributes.addFlashAttribute("error", "Unauthorized access");
                return "redirect:/admin/profile";
            }
            
            // Preserve password if not changed
            if (admin.getPassword() == null || admin.getPassword().isEmpty()) {
                admin.setPassword(currentAdmin.getPassword());
            } else {
                // Password is being changed, encode it
                admin.setPassword(passwordEncoder.encode(admin.getPassword()));
            }
            
            // Preserve other important fields
            admin.setCreatedAt(currentAdmin.getCreatedAt());
            admin.setStatus(currentAdmin.getStatus());
            admin.setRole(currentAdmin.getRole());
            
            adminService.saveAdmin(admin);
            redirectAttributes.addFlashAttribute("message", "Profile updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating profile: " + e.getMessage());
        }
        
        return "redirect:/admin/profile";
    }
    
    @GetMapping("/reset-password")
    public String showResetPasswordForm(Model model) {
        Admin admin = getCurrentAdmin();
        model.addAttribute("admin", admin);
        model.addAttribute("currentAdmin", admin);
        return "admin/reset-password";
    }
    
    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String currentPassword,
                               @RequestParam String newPassword,
                               @RequestParam String confirmPassword,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        Admin admin = getCurrentAdmin();
        
        // Validate passwords match
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "New password and confirm password do not match");
            model.addAttribute("admin", admin);
            model.addAttribute("currentAdmin", admin);
            return "admin/reset-password";
        }
        
        // Validate password length
        if (newPassword.length() < 6) {
            model.addAttribute("error", "New password must be at least 6 characters long");
            model.addAttribute("admin", admin);
            model.addAttribute("currentAdmin", admin);
            return "admin/reset-password";
        }
        
        // Verify current password
        if (!passwordEncoder.matches(currentPassword, admin.getPassword())) {
            model.addAttribute("error", "Current password is incorrect");
            model.addAttribute("admin", admin);
            model.addAttribute("currentAdmin", admin);
            return "admin/reset-password";
        }
        
        // Reset password
        admin.setPassword(passwordEncoder.encode(newPassword));
        adminService.saveAdmin(admin);
        
        // Logout the user after password reset for security
        redirectAttributes.addFlashAttribute("message", "Password has been reset successfully. Please login again with your new password.");
        return "redirect:/logout";
    }
}
