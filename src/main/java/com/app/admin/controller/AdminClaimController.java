package com.app.admin.controller;

import com.app.model.Claim;
import com.app.model.Admin;
import com.app.service.ClaimService;
import com.app.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/claims")
public class AdminClaimController {
    
    @Autowired
    private ClaimService claimService;
    
    @Autowired
    private AdminService adminService;
    
    @GetMapping
    public String listApprovedClaims(Model model,
                                    @RequestParam(required = false) String search) {
        List<Claim> claims = claimService.getApprovedClaims();
        
        // Filter by search query if provided
        if (search != null && !search.isEmpty()) {
            final String searchLower = search.toLowerCase();
            claims = claims.stream()
                    .filter(claim ->
                        (claim.getEmployee() != null && claim.getEmployee().getFullName() != null && 
                         claim.getEmployee().getFullName().toLowerCase().contains(searchLower)) ||
                        (claim.getClaimType() != null && claim.getClaimType().toLowerCase().contains(searchLower)) ||
                        (claim.getDescription() != null && claim.getDescription().toLowerCase().contains(searchLower))
                    )
                    .collect(java.util.stream.Collectors.toList());
            model.addAttribute("searchQuery", search);
        }
        
        model.addAttribute("claims", claims);
        return "admin/claims/list";
    }
    
    @GetMapping("/view/{id}")
    public String viewClaim(@PathVariable Long id, Model model) {
        Claim claim = claimService.getClaimById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid claim ID: " + id));
        
        if (!"APPROVED".equals(claim.getStatus())) {
            throw new IllegalArgumentException("Only approved claims can be viewed here");
        }
        
        model.addAttribute("claim", claim);
        return "admin/claims/view";
    }
    
    @PostMapping("/mark-paid/{id}")
    public String markClaimAsPaid(@PathVariable Long id,
                                 @RequestParam String paymentMethod,
                                 @RequestParam(required = false) String transactionReference,
                                 RedirectAttributes redirectAttributes) {
        try {
            // Get current admin
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            Optional<Admin> adminOpt = adminService.getAdminByUsername(username);
            
            if (adminOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Admin not found");
                return "redirect:/admin/claims/view/" + id;
            }
            
            claimService.markClaimAsPaid(id, adminOpt.get(), paymentMethod, transactionReference);
            
            redirectAttributes.addFlashAttribute("message", "Claim marked as paid successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error marking claim as paid: " + e.getMessage());
        }
        
        return "redirect:/admin/claims/view/" + id;
    }
}
