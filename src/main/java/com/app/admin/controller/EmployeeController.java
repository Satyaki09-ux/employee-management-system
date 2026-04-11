package com.app.admin.controller;

import com.app.config.AuditHelper;
import com.app.model.Employee;
import com.app.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/employees")
public class EmployeeController {

    private static final Map<String, String> TYPE_TO_HIERARCHY = Map.of(
            "promoters", "PROMOTER",
            "zonal-heads", "ZONAL_HEAD",
            "cluster-heads", "CLUSTER_HEAD",
            "area-sales-managers", "AREA_SALES_MANAGER"
    );

    private static final Map<String, String> TYPE_TO_LABEL = Map.of(
            "promoters", "Promoter",
            "zonal-heads", "Zonal Head",
            "cluster-heads", "Cluster Head",
            "area-sales-managers", "Area Sales Manager"
    );

    @Autowired
    private EmployeeService employeeService;

    private String hierarchyFor(String type) {
        String h = TYPE_TO_HIERARCHY.get(type);
        if (h == null) throw new IllegalArgumentException("Invalid employee type: " + type);
        return h;
    }

    private String labelFor(String type) {
        return TYPE_TO_LABEL.getOrDefault(type, type);
    }

    private void addTypeAttributes(Model model, String type) {
        String base = "/admin/employees/" + type;
        model.addAttribute("employeeType", type);
        model.addAttribute("employeeTypeLabel", labelFor(type));
        model.addAttribute("listUrl", base);
        model.addAttribute("baseUrl", base);
        model.addAttribute("saveUrl", base + "/save");
    }

    /** Redirect /admin/employees to promoters list. */
    @GetMapping
    public String index() {
        return "redirect:/admin/employees/promoters";
    }

    @GetMapping("/{type}")
    public String listByType(@PathVariable String type,
                             @RequestParam(required = false) String search,
                             Model model) {
        String hierarchy = hierarchyFor(type);
        List<Employee> employees = employeeService.searchEmployeesByHierarchyLevel(hierarchy, search);
        model.addAttribute("employees", employees);
        model.addAttribute("search", search);
        addTypeAttributes(model, type);
        return "admin/employees/list";
    }

    @GetMapping("/{type}/new")
    public String newForm(@PathVariable String type, Model model) {
        String hierarchy = hierarchyFor(type);
        Employee employee = new Employee();
        employee.setHierarchyLevel(hierarchy);
        model.addAttribute("employee", employee);
        model.addAttribute("managers", employeeService.getReportingManagerOptionsForAdmin(hierarchy));
        model.addAttribute("reportingManagerIsSelf", "PROMOTER".equals(hierarchy));
        addTypeAttributes(model, type);
        return "admin/employees/form";
    }

    @GetMapping("/{type}/edit/{id}")
    public String editForm(@PathVariable String type, @PathVariable Long id, Model model) {
        hierarchyFor(type);
        Employee employee = employeeService.getEmployeeById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid employee ID: " + id));
        model.addAttribute("employee", employee);
        List<Employee> managers = employeeService.getReportingManagerOptionsForAdmin(employee.getHierarchyLevel())
                .stream()
                .filter(e -> !e.getId().equals(id))
                .toList();
        model.addAttribute("managers", managers);
        model.addAttribute("reportingManagerIsSelf", "PROMOTER".equals(employee.getHierarchyLevel()));
        addTypeAttributes(model, type);
        return "admin/employees/form";
    }

    @GetMapping("/{type}/view/{id}")
    public String view(@PathVariable String type, @PathVariable Long id, Model model) {
        hierarchyFor(type);
        Employee employee = employeeService.getEmployeeById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid employee ID: " + id));
        model.addAttribute("employee", employee);
        model.addAttribute("createdByDisplay", AuditHelper.formatAuditForDisplay(employee.getCreatedBy()));
        model.addAttribute("lastUpdatedByDisplay", AuditHelper.formatAuditForDisplay(employee.getLastUpdatedBy()));
        addTypeAttributes(model, type);
        return "admin/employees/view";
    }

    // Delete functionality removed - employees cannot be deleted, only inactivated
    // @GetMapping("/{type}/delete/{id}")
    // public String delete(@PathVariable String type, @PathVariable Long id, RedirectAttributes ra) {
    //     hierarchyFor(type);
    //     employeeService.deleteEmployee(id);
    //     ra.addFlashAttribute("message", labelFor(type) + " deleted successfully!");
    //     return "redirect:/admin/employees/" + type;
    // }

    @PostMapping("/{type}/save")
    public String save(@PathVariable String type,
                       @Valid @ModelAttribute Employee employee,
                       @RequestParam(required = false) Long reportingManagerId,
                       BindingResult result,
                       Model model,
                       RedirectAttributes ra) {
        String hierarchy = hierarchyFor(type);
        if (result.hasErrors()) {
            String h = employee.getHierarchyLevel() != null ? employee.getHierarchyLevel() : hierarchy;
            model.addAttribute("managers", employeeService.getReportingManagerOptionsForAdmin(h));
            model.addAttribute("reportingManagerIsSelf", "PROMOTER".equals(h));
            if (!"PROMOTER".equals(h) && reportingManagerId != null) {
                employee.setReportingManager(employeeService.getEmployeeById(reportingManagerId).orElse(null));
            } else if ("PROMOTER".equals(h)) {
                employee.setReportingManager(null);
            }
            addTypeAttributes(model, type);
            return "admin/employees/form";
        }

        // Check for duplicate email and phone (both must be unique independently)
        if (employee.getId() == null) {
            // Check email uniqueness
            if (employeeService.emailExists(employee.getEmail())) {
                result.rejectValue("email", "error.employee", "An employee with this email already exists");
                model.addAttribute("managers", employeeService.getReportingManagerOptionsForAdmin(hierarchy));
                model.addAttribute("reportingManagerIsSelf", "PROMOTER".equals(hierarchy));
                addTypeAttributes(model, type);
                return "admin/employees/form";
            }
            // Check phone uniqueness
            if (employeeService.phoneExists(employee.getPhone())) {
                result.rejectValue("phone", "error.employee", "An employee with this phone number already exists");
                model.addAttribute("managers", employeeService.getReportingManagerOptionsForAdmin(hierarchy));
                model.addAttribute("reportingManagerIsSelf", "PROMOTER".equals(hierarchy));
                addTypeAttributes(model, type);
                return "admin/employees/form";
            }
        } else {
            // Check email uniqueness for other employees
            if (employeeService.emailExistsForOtherEmployee(employee.getEmail(), employee.getId())) {
                result.rejectValue("email", "error.employee", "An employee with this email already exists");
                model.addAttribute("managers", employeeService.getReportingManagerOptionsForAdmin(
                        employee.getHierarchyLevel()));
                model.addAttribute("reportingManagerIsSelf", "PROMOTER".equals(employee.getHierarchyLevel()));
                addTypeAttributes(model, type);
                return "admin/employees/form";
            }
            // Check phone uniqueness for other employees
            if (employeeService.phoneExistsForOtherEmployee(employee.getPhone(), employee.getId())) {
                result.rejectValue("phone", "error.employee", "An employee with this phone number already exists");
                model.addAttribute("managers", employeeService.getReportingManagerOptionsForAdmin(
                        employee.getHierarchyLevel()));
                model.addAttribute("reportingManagerIsSelf", "PROMOTER".equals(employee.getHierarchyLevel()));
                addTypeAttributes(model, type);
                return "admin/employees/form";
            }
        }

        if ("PROMOTER".equals(employee.getHierarchyLevel() != null ? employee.getHierarchyLevel() : hierarchy)) {
            employee.setReportingManager(null);
        } else if (reportingManagerId != null) {
            employee.setReportingManager(
                    employeeService.getEmployeeById(reportingManagerId).orElse(null));
        } else {
            employee.setReportingManager(null);
        }

        if (employee.getHierarchyLevel() == null || employee.getHierarchyLevel().isEmpty()) {
            employee.setHierarchyLevel(hierarchy);
        }

        String audit = AuditHelper.currentUserAuditString();
        if (employee.getId() == null) {
            employee.setCreatedBy(audit);
        } else {
            employeeService.getEmployeeById(employee.getId())
                    .ifPresent(existing -> employee.setCreatedBy(existing.getCreatedBy()));
            employee.setLastUpdatedBy(audit);
        }

        employeeService.saveEmployee(employee);
        ra.addFlashAttribute("message", labelFor(type) + " saved successfully!");
        return "redirect:/admin/employees/" + type;
    }

    /**
     * Show reset password form for an employee
     */
    @GetMapping("/{type}/reset-password/{id}")
    public String showResetPasswordForm(@PathVariable String type, @PathVariable Long id, Model model) {
        hierarchyFor(type);
        Employee employee = employeeService.getEmployeeById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid employee ID: " + id));
        model.addAttribute("employee", employee);
        addTypeAttributes(model, type);
        return "admin/employees/reset-password";
    }

    /**
     * Reset password for an employee
     */
    @PostMapping("/{type}/reset-password/{id}")
    public String resetPassword(@PathVariable String type,
                               @PathVariable Long id,
                               @RequestParam String newPassword,
                               @RequestParam String confirmPassword,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        hierarchyFor(type);
        
        // Validate passwords match
        if (newPassword == null || newPassword.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "New password cannot be empty");
            return "redirect:/admin/employees/" + type + "/reset-password/" + id;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match");
            return "redirect:/admin/employees/" + type + "/reset-password/" + id;
        }
        
        // Validate password length (minimum 6 characters)
        if (newPassword.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "Password must be at least 6 characters long");
            return "redirect:/admin/employees/" + type + "/reset-password/" + id;
        }
        
        try {
            Employee employee = employeeService.getEmployeeById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid employee ID: " + id));
            
            employeeService.resetPassword(id, newPassword);
            redirectAttributes.addFlashAttribute("message", 
                "Password has been reset successfully for " + employee.getFullName() + " (ID: " + employee.getUsername() + ")");
            return "redirect:/admin/employees/" + type + "/view/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error resetting password: " + e.getMessage());
            return "redirect:/admin/employees/" + type + "/reset-password/" + id;
        }
    }
}
