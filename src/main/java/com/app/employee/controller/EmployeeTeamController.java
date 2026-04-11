package com.app.employee.controller;

import com.app.config.AuditHelper;
import com.app.model.Employee;
import com.app.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/employee/team")
public class EmployeeTeamController {

    @Autowired
    private EmployeeService employeeService;

    private Employee getCurrentEmployee() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return employeeService.getEmployeeByUsername(username)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
    }

    private void ensureCanManage(String type) {
        Employee current = getCurrentEmployee();
        List<String> allowed = employeeService.getManageableTypesForHierarchy(current.getHierarchyLevel());
        if (!allowed.contains(type)) {
            throw new AccessDeniedException("You do not have permission to manage " + type);
        }
    }

    private void addTypeAttributes(Model model, String type) {
        String base = "/employee/team/" + type;
        model.addAttribute("employeeType", type);
        model.addAttribute("employeeTypeLabel", employeeService.getLabelForType(type));
        model.addAttribute("listUrl", base);
        model.addAttribute("baseUrl", base);
        model.addAttribute("saveUrl", base + "/save");
    }

    /** Redirect /employee/team to first manageable type, or dashboard if ASM. */
    @GetMapping
    public String index() {
        Employee current = getCurrentEmployee();
        List<String> types = employeeService.getManageableTypesForHierarchy(current.getHierarchyLevel());
        if (types.isEmpty()) return "redirect:/employee/dashboard";
        return "redirect:/employee/team/" + types.get(0);
    }

    @GetMapping("/{type}")
    public String listByType(@PathVariable String type,
                             @RequestParam(required = false) String search,
                             Model model) {
        ensureCanManage(type);
        Employee currentEmployee = getCurrentEmployee();
        String hierarchy = employeeService.getHierarchyForType(type);
        List<Employee> employees = employeeService.getAllReportingEmployees(currentEmployee).stream()
                .filter(e -> hierarchy.equals(e.getHierarchyLevel()))
                .filter(e -> search == null || search.isBlank()
                        || e.getFullName().toLowerCase().contains(search.toLowerCase())
                        || (e.getEmail() != null && e.getEmail().toLowerCase().contains(search.toLowerCase()))
                        || (e.getUsername() != null && e.getUsername().toLowerCase().contains(search.toLowerCase()))
                        || (e.getPhone() != null && e.getPhone().contains(search)))
                .toList();

        model.addAttribute("employees", employees);
        model.addAttribute("search", search);
        addTypeAttributes(model, type);
        return "employee/team/list";
    }

    @GetMapping("/{type}/new")
    public String newForm(@PathVariable String type, Model model) {
        ensureCanManage(type);
        String hierarchy = employeeService.getHierarchyForType(type);
        Employee employee = new Employee();
        employee.setHierarchyLevel(hierarchy);
        Employee current = getCurrentEmployee();
        employee.setReportingManager(current);
        model.addAttribute("employee", employee);
        //model.addAttribute("managers", employeeService.getReportingManagerOptionsForAdmin(hierarchy));
        model.addAttribute("managers",
                employeeService.getReportingManagerOptionsForEmployeePortal(current, hierarchy));
        addTypeAttributes(model, type);
        return "employee/team/form";
    }

    @GetMapping("/{type}/edit/{id}")
    public String editForm(@PathVariable String type, @PathVariable Long id, Model model) {
        ensureCanManage(type);
        Employee current = getCurrentEmployee();
        employeeService.getHierarchyForType(type);
        Employee employee = employeeService.getEmployeeById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid employee ID: " + id));
        model.addAttribute("employee", employee);
        List<Employee> managers = employeeService
                .getReportingManagerOptionsForEmployeePortal(current, employee.getHierarchyLevel())
                .stream()
                .filter(e -> !e.getId().equals(id))
                .toList();
        model.addAttribute("managers", managers);
        addTypeAttributes(model, type);
        return "employee/team/form";
    }

    @GetMapping("/{type}/view/{id}")
    public String view(@PathVariable String type, @PathVariable Long id, Model model) {
        ensureCanManage(type);
        employeeService.getHierarchyForType(type);
        Employee employee = employeeService.getEmployeeById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid employee ID: " + id));
        model.addAttribute("employee", employee);
        model.addAttribute("createdByDisplay", AuditHelper.formatAuditForDisplay(employee.getCreatedBy()));
        model.addAttribute("lastUpdatedByDisplay", AuditHelper.formatAuditForDisplay(employee.getLastUpdatedBy()));
        addTypeAttributes(model, type);
        return "employee/team/view";
    }

    // Delete functionality removed - employees cannot be deleted, only inactivated
    // @GetMapping("/{type}/delete/{id}")
    // public String delete(@PathVariable String type, @PathVariable Long id, RedirectAttributes ra) {
    //     ensureCanManage(type);
    //     employeeService.getHierarchyForType(type);
    //     employeeService.deleteEmployee(id);
    //     ra.addFlashAttribute("message", employeeService.getLabelForType(type) + " deleted successfully!");
    //     return "redirect:/employee/team/" + type;
    // }

    @PostMapping("/{type}/save")
    public String save(@PathVariable String type,
                       @Valid @ModelAttribute Employee employee,
                       @RequestParam(required = false) Long reportingManagerId,
                       BindingResult result,
                       Model model,
                       RedirectAttributes ra) {
        ensureCanManage(type);
        String hierarchy = employeeService.getHierarchyForType(type);
        if (result.hasErrors()) {
            String h = employee.getHierarchyLevel() != null ? employee.getHierarchyLevel() : hierarchy;
//            model.addAttribute("managers", employeeService.getReportingManagerOptionsForAdmin(h));
            model.addAttribute("managers",
                    employeeService.getReportingManagerOptionsForEmployeePortal(getCurrentEmployee(), h));
            if (reportingManagerId != null) {
                employee.setReportingManager(employeeService.getEmployeeById(reportingManagerId).orElse(null));
            } else if (employee.getId() == null) {
                employee.setReportingManager(getCurrentEmployee());
            }
            addTypeAttributes(model, type);
            return "employee/team/form";
        }

        // Check for duplicate email and phone (both must be unique independently)
        if (employee.getId() == null) {
            // Check email uniqueness
            if (employeeService.emailExists(employee.getEmail())) {
                result.rejectValue("email", "error.employee", "An employee with this email already exists");
                model.addAttribute("managers", employeeService.getReportingManagerOptionsForAdmin(hierarchy));
                addTypeAttributes(model, type);
                return "employee/team/form";
            }
            // Check phone uniqueness
            if (employeeService.phoneExists(employee.getPhone())) {
                result.rejectValue("phone", "error.employee", "An employee with this phone number already exists");
                model.addAttribute("managers", employeeService.getReportingManagerOptionsForAdmin(hierarchy));
                addTypeAttributes(model, type);
                return "employee/team/form";
            }
        } else {
            // Check email uniqueness for other employees
            if (employeeService.emailExistsForOtherEmployee(employee.getEmail(), employee.getId())) {
                result.rejectValue("email", "error.employee", "An employee with this email already exists");
                model.addAttribute("managers", employeeService.getReportingManagerOptionsForAdmin(
                        employee.getHierarchyLevel()));
                addTypeAttributes(model, type);
                return "employee/team/form";
            }
            // Check phone uniqueness for other employees
            if (employeeService.phoneExistsForOtherEmployee(employee.getPhone(), employee.getId())) {
                result.rejectValue("phone", "error.employee", "An employee with this phone number already exists");
                model.addAttribute("managers", employeeService.getReportingManagerOptionsForAdmin(
                        employee.getHierarchyLevel()));
                addTypeAttributes(model, type);
                return "employee/team/form";
            }
        }

        if (reportingManagerId != null) {
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
        ra.addFlashAttribute("message", employeeService.getLabelForType(type) + " saved successfully!");
        return "redirect:/employee/team/" + type;
    }
}
