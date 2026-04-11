package com.app.config;

import com.app.model.Admin;
import com.app.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Adds currentAdmin to the model for all admin portal pages.
 */
@ControllerAdvice(basePackages = "com.app.admin.controller")
public class AdminPortalControllerAdvice {

    @Autowired
    private AdminService adminService;

    @ModelAttribute("currentAdmin")
    public Admin currentAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return adminService.getAdminByUsername(auth.getName()).orElse(null);
    }
}
