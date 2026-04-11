package com.app.admin.controller;

import com.app.model.Lead;
import com.app.service.LeadService;
import com.app.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/leads")
public class LeadController {
    
    @Autowired
    private LeadService leadService;
    
    @Autowired
    private EmployeeService employeeService;
    
    @GetMapping
    public String listLeads(Model model, 
                           @RequestParam(required = false) String status,
                           @RequestParam(required = false) Long assignedTo) {
        List<Lead> leads;
        
        if (status != null && !status.isEmpty()) {
            leads = leadService.getLeadsByStatus(status);
        } else if (assignedTo != null) {
            leads = leadService.getLeadsByAssignedEmployee(assignedTo);
        } else {
            leads = leadService.getAllLeads();
        }
        
        model.addAttribute("leads", leads);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedAssignedTo", assignedTo);
        model.addAttribute("employees", employeeService.getAllEmployees());
        return "admin/leads/list";
    }
    
    @GetMapping("/view/{id}")
    public String viewLead(@PathVariable Long id, Model model) {
        Lead lead = leadService.getLeadById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid lead ID: " + id));
        
        model.addAttribute("lead", lead);
        model.addAttribute("employees", employeeService.getAllEmployees());
        return "admin/leads/view";
    }
    
    @PostMapping("/update-status/{id}")
    public String updateStatus(@PathVariable Long id,
                              @RequestParam String status,
                              RedirectAttributes redirectAttributes) {
        leadService.updateLeadStatus(id, status);
        redirectAttributes.addFlashAttribute("message", "Lead status updated successfully");
        return "redirect:/admin/leads/view/" + id;
    }
    
    @PostMapping("/assign/{id}")
    public String assignLead(@PathVariable Long id,
                             @RequestParam(required = false) Long employeeId,
                             RedirectAttributes redirectAttributes) {
        leadService.assignLead(id, employeeId);
        redirectAttributes.addFlashAttribute("message", "Lead assigned successfully");
        return "redirect:/admin/leads/view/" + id;
    }
    
    @PostMapping("/update-notes/{id}")
    public String updateNotes(@PathVariable Long id,
                             @RequestParam String notes,
                             RedirectAttributes redirectAttributes) {
        leadService.updateLeadNotes(id, notes);
        redirectAttributes.addFlashAttribute("message", "Notes updated successfully");
        return "redirect:/admin/leads/view/" + id;
    }
    
    @PostMapping("/delete/{id}")
    public String deleteLead(@PathVariable Long id,
                            RedirectAttributes redirectAttributes) {
        leadService.deleteLead(id);
        redirectAttributes.addFlashAttribute("message", "Lead deleted successfully");
        return "redirect:/admin/leads";
    }
}
