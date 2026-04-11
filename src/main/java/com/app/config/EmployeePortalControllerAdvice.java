package com.app.config;

import com.app.model.Employee;
import com.app.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adds currentEmployee and manageableEmployeeTypes to the model for all employee portal pages.
 * Used by the employee sidebar to conditionally show Employee Management submenus.
 */
@ControllerAdvice(basePackages = "com.app.employee.controller")
public class EmployeePortalControllerAdvice {

    @Autowired
    private EmployeeService employeeService;

    @ModelAttribute("currentEmployee")
    public Employee currentEmployee() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return employeeService.getEmployeeByUsername(auth.getName()).orElse(null);
    }

    @ModelAttribute("manageableEmployeeTypes")
    public List<String> manageableEmployeeTypes() {
        Employee emp = currentEmployee();
        if (emp == null || emp.getHierarchyLevel() == null) {
            return Collections.emptyList();
        }
        return employeeService.getManageableTypesForHierarchy(emp.getHierarchyLevel());
    }

    @ModelAttribute("manageableEmployeeTypeLabels")
    public Map<String, String> manageableEmployeeTypeLabels() {
        List<String> types = manageableEmployeeTypes();
        Map<String, String> labels = new LinkedHashMap<>();
        for (String slug : types) {
            labels.put(slug, employeeService.getLabelForType(slug));
        }
        return labels;
    }
    
    @ModelAttribute("employeeHierarchyLabel")
    public String employeeHierarchyLabel() {
        Employee emp = currentEmployee();
        if (emp == null || emp.getHierarchyLevel() == null) {
            return "";
        }
        return employeeService.formatHierarchyLevel(emp.getHierarchyLevel());
    }
}
