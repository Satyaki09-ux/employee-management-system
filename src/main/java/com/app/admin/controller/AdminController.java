package com.app.admin.controller;

import com.app.model.Admin;
import com.app.service.AdminService;
import com.app.util.AdminPermissions;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/admin/admins")
public class AdminController {
    
    @Autowired
    private AdminService adminService;
    
    @GetMapping
    public String listAdmins(Model model, @RequestParam(required = false) String role) {
        List<Admin> admins;
        if (role != null && !role.isEmpty()) {
            admins = adminService.getAdminsByRole(role);
        } else {
            admins = adminService.getAllAdmins();
        }
        model.addAttribute("admins", admins);
        model.addAttribute("selectedRole", role);
        return "admin/admins/list";
    }
    
    @GetMapping("/new")
    public String showAdminForm(Model model) {
        model.addAttribute("admin", new Admin());
        // Get all active admins as potential parent admins for sub-admins
        List<Admin> parentAdmins = adminService.getActiveAdmins();
        model.addAttribute("parentAdmins", parentAdmins);
        model.addAttribute("allPermissions", AdminPermissions.getAllPermissions());
        model.addAttribute("permissionLabels", AdminPermissions.getPermissionLabels());
        return "admin/admins/form";
    }
    
    @GetMapping("/edit/{id}")
    public String editAdmin(@PathVariable Long id, Model model) {
        Admin admin = adminService.getAdminById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid admin ID: " + id));
        model.addAttribute("admin", admin);
        // Get all active admins as potential parent admins (excluding self)
        List<Admin> parentAdmins = adminService.getActiveAdmins().stream()
                .filter(a -> !a.getId().equals(id))
                .toList();
        model.addAttribute("parentAdmins", parentAdmins);
        model.addAttribute("allPermissions", AdminPermissions.getAllPermissions());
        model.addAttribute("permissionLabels", AdminPermissions.getPermissionLabels());
        // Parse existing permissions
        if (admin.getPermissions() != null && !admin.getPermissions().isEmpty()) {
            model.addAttribute("selectedPermissions", AdminPermissions.parsePermissions(admin.getPermissions()));
        }
        return "admin/admins/form";
    }
    
    @PostMapping("/save")
    public String saveAdmin(@Valid @ModelAttribute Admin admin,
                           @RequestParam(required = false) Long parentAdminId,
                           @RequestParam(required = false) String[] permissions,
                           BindingResult result,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        // Handle permissions
        if (permissions != null && permissions.length > 0) {
            admin.setPermissions(String.join(",", permissions));
        } else {
            admin.setPermissions("");
        }
        
        if (result.hasErrors()) {
            List<Admin> parentAdmins = adminService.getActiveAdmins().stream()
                    .filter(a -> admin.getId() == null || !a.getId().equals(admin.getId()))
                    .toList();
            model.addAttribute("parentAdmins", parentAdmins);
            model.addAttribute("allPermissions", AdminPermissions.getAllPermissions());
            model.addAttribute("permissionLabels", AdminPermissions.getPermissionLabels());
            if (permissions != null && permissions.length > 0) {
                model.addAttribute("selectedPermissions", Arrays.asList(permissions));
            }
            return "admin/admins/form";
        }
        
        // Check for duplicate email
        if (admin.getId() == null) {
            if (adminService.emailExists(admin.getEmail())) {
                result.rejectValue("email", "error.admin", "Email already exists");
                List<Admin> parentAdmins = adminService.getActiveAdmins();
                model.addAttribute("parentAdmins", parentAdmins);
                model.addAttribute("allPermissions", AdminPermissions.getAllPermissions());
                model.addAttribute("permissionLabels", AdminPermissions.getPermissionLabels());
                if (permissions != null && permissions.length > 0) {
                    model.addAttribute("selectedPermissions", Arrays.asList(permissions));
                }
                return "admin/admins/form";
            }
            if (adminService.usernameExists(admin.getUsername())) {
                result.rejectValue("username", "error.admin", "Username already exists");
                List<Admin> parentAdmins = adminService.getActiveAdmins();
                model.addAttribute("parentAdmins", parentAdmins);
                model.addAttribute("allPermissions", AdminPermissions.getAllPermissions());
                model.addAttribute("permissionLabels", AdminPermissions.getPermissionLabels());
                if (permissions != null && permissions.length > 0) {
                    model.addAttribute("selectedPermissions", Arrays.asList(permissions));
                }
                return "admin/admins/form";
            }
        } else {
            if (adminService.emailExistsForOtherAdmin(admin.getEmail(), admin.getId())) {
                result.rejectValue("email", "error.admin", "Email already exists");
                List<Admin> parentAdmins = adminService.getActiveAdmins().stream()
                        .filter(a -> !a.getId().equals(admin.getId()))
                        .toList();
                model.addAttribute("parentAdmins", parentAdmins);
                model.addAttribute("allPermissions", AdminPermissions.getAllPermissions());
                model.addAttribute("permissionLabels", AdminPermissions.getPermissionLabels());
                if (permissions != null && permissions.length > 0) {
                    model.addAttribute("selectedPermissions", Arrays.asList(permissions));
                }
                return "admin/admins/form";
            }
            if (adminService.usernameExistsForOtherAdmin(admin.getUsername(), admin.getId())) {
                result.rejectValue("username", "error.admin", "Username already exists");
                List<Admin> parentAdmins = adminService.getActiveAdmins().stream()
                        .filter(a -> !a.getId().equals(admin.getId()))
                        .toList();
                model.addAttribute("parentAdmins", parentAdmins);
                model.addAttribute("allPermissions", AdminPermissions.getAllPermissions());
                model.addAttribute("permissionLabels", AdminPermissions.getPermissionLabels());
                if (permissions != null && permissions.length > 0) {
                    model.addAttribute("selectedPermissions", Arrays.asList(permissions));
                }
                return "admin/admins/form";
            }
        }
        
        // Set parent admin if provided
        if (parentAdminId != null && parentAdminId > 0) {
            Admin parentAdmin = adminService.getAdminById(parentAdminId)
                    .orElse(null);
            admin.setParentAdmin(parentAdmin);
        } else {
            admin.setParentAdmin(null);
        }
        
        adminService.saveAdmin(admin);
        redirectAttributes.addFlashAttribute("success", "Admin saved successfully!");
        return "redirect:/admin/admins";
    }
    
    // Delete functionality removed - admins cannot be deleted
    // @GetMapping("/delete/{id}")
    // public String deleteAdmin(@PathVariable Long id, RedirectAttributes redirectAttributes) {
    //     try {
    //         adminService.deleteAdmin(id);
    //         redirectAttributes.addFlashAttribute("success", "Admin deleted successfully!");
    //     } catch (Exception e) {
    //         redirectAttributes.addFlashAttribute("error", "Error deleting admin: " + e.getMessage());
    //     }
    //     return "redirect:/admin/admins";
    // }
    
    @GetMapping("/view/{id}")
    public String viewAdmin(@PathVariable Long id, Model model) {
        Admin admin = adminService.getAdminById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid admin ID: " + id));
        model.addAttribute("admin", admin);
        
        // Get sub-admins if any
        List<Admin> subAdmins = adminService.getSubAdmins(id);
        model.addAttribute("subAdmins", subAdmins);
        
        return "admin/admins/view";
    }
}
