package com.app.employee.controller;

import com.app.model.LeaveRequest;
import com.app.model.Employee;
import com.app.service.LeaveRequestService;
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
@RequestMapping("/employee/leave-requests")
public class LeaveRequestController {
    
    @Autowired
    private LeaveRequestService leaveRequestService;
    
    @Autowired
    private EmployeeService employeeService;
    
    private Employee getCurrentEmployee() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return employeeService.getEmployeeByUsername(username)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
    }
    
    @GetMapping
    public String listLeaveRequests(Model model) {
        Employee employee = getCurrentEmployee();
        List<LeaveRequest> leaveRequests = leaveRequestService.getLeaveRequestsByEmployee(employee);
        model.addAttribute("employee", employee);
        model.addAttribute("leaveRequests", leaveRequests);
        return "employee/leave-requests/list";
    }
    
    @GetMapping("/new")
    public String showLeaveRequestForm(Model model) {
        Employee employee = getCurrentEmployee();
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setEmployee(employee);
        model.addAttribute("employee", employee);
        model.addAttribute("leaveRequest", leaveRequest);
        return "employee/leave-requests/form";
    }
    
    @GetMapping("/view/{id}")
    public String viewLeaveRequest(@PathVariable Long id, Model model) {
        Employee employee = getCurrentEmployee();
        LeaveRequest leaveRequest = leaveRequestService.getLeaveRequestById(id)
                .orElseThrow(() -> new RuntimeException("Leave request not found"));
        
        // Verify ownership or manager assignment
        boolean isOwner = leaveRequest.getEmployee().getId().equals(employee.getId());
        boolean isAssignedManager = leaveRequest.getAssignedTo() != null && leaveRequest.getAssignedTo().getId().equals(employee.getId());
        
        if (!isOwner && !isAssignedManager) {
            throw new RuntimeException("Unauthorized access");
        }
        
        model.addAttribute("employee", employee);
        model.addAttribute("leaveRequest", leaveRequest);
        model.addAttribute("canApprove", isAssignedManager && "PENDING".equals(leaveRequest.getStatus()));
        return "employee/leave-requests/view";
    }
    
    @PostMapping("/save")
    public String saveLeaveRequest(@ModelAttribute LeaveRequest leaveRequest,
                                  RedirectAttributes redirectAttributes) {
        try {
            Employee employee = getCurrentEmployee();
            leaveRequest.setEmployee(employee);
            leaveRequest.setStatus("PENDING");
            
            // Assign to reporting manager for approval
            if (employee.getReportingManager() != null) {
                leaveRequest.setAssignedTo(employee.getReportingManager());
            }
            
            leaveRequestService.saveLeaveRequest(leaveRequest);
            redirectAttributes.addFlashAttribute("message", "Leave request submitted successfully and sent to your reporting manager for approval!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/employee/leave-requests";
    }
    
    @GetMapping("/cancel/{id}")
    public String cancelLeaveRequest(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Employee employee = getCurrentEmployee();
            LeaveRequest leaveRequest = leaveRequestService.getLeaveRequestById(id)
                    .orElseThrow(() -> new RuntimeException("Leave request not found"));
            
            // Verify ownership
            if (!leaveRequest.getEmployee().getId().equals(employee.getId())) {
                throw new RuntimeException("Unauthorized access");
            }
            
            leaveRequestService.cancelLeaveRequest(id);
            redirectAttributes.addFlashAttribute("message", "Leave request cancelled successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/employee/leave-requests";
    }
    
    @GetMapping("/pending-approvals")
    public String pendingApprovals(Model model) {
        Employee manager = getCurrentEmployee();
        List<LeaveRequest> pendingRequests = leaveRequestService.getPendingLeaveRequestsForManager(manager);
        model.addAttribute("manager", manager);
        model.addAttribute("pendingRequests", pendingRequests);
        return "employee/leave-requests/pending-approvals";
    }
    
    @PostMapping("/approve/{id}")
    public String approveLeaveRequest(@PathVariable Long id,
                                     @RequestParam(required = false) String remarks,
                                     RedirectAttributes redirectAttributes) {
        try {
            Employee manager = getCurrentEmployee();
            LeaveRequest leaveRequest = leaveRequestService.getLeaveRequestById(id)
                    .orElseThrow(() -> new RuntimeException("Leave request not found"));
            
            // Verify manager is assigned to approve this request
            if (leaveRequest.getAssignedTo() == null || !leaveRequest.getAssignedTo().getId().equals(manager.getId())) {
                throw new RuntimeException("You are not authorized to approve this leave request");
            }
            
            leaveRequestService.approveLeaveRequest(id, manager, remarks);
            redirectAttributes.addFlashAttribute("message", "Leave request approved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/employee/leave-requests/pending-approvals";
    }
    
    @PostMapping("/reject/{id}")
    public String rejectLeaveRequest(@PathVariable Long id,
                                    @RequestParam(required = false) String remarks,
                                    RedirectAttributes redirectAttributes) {
        try {
            Employee manager = getCurrentEmployee();
            LeaveRequest leaveRequest = leaveRequestService.getLeaveRequestById(id)
                    .orElseThrow(() -> new RuntimeException("Leave request not found"));
            
            // Verify manager is assigned to approve this request
            if (leaveRequest.getAssignedTo() == null || !leaveRequest.getAssignedTo().getId().equals(manager.getId())) {
                throw new RuntimeException("You are not authorized to reject this leave request");
            }
            
            leaveRequestService.rejectLeaveRequest(id, manager, remarks);
            redirectAttributes.addFlashAttribute("message", "Leave request rejected.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/employee/leave-requests/pending-approvals";
    }
}
