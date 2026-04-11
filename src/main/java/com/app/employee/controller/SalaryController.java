package com.app.employee.controller;

import com.app.model.Salary;
import com.app.model.Employee;
import com.app.service.SalaryService;
import com.app.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/employee/salary")
public class SalaryController {
    
    @Autowired
    private SalaryService salaryService;
    
    @Autowired
    private EmployeeService employeeService;
    
    private Employee getCurrentEmployee() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return employeeService.getEmployeeByUsername(username)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
    }
    
    @GetMapping
    public String listSalaries(Model model) {
        Employee employee = getCurrentEmployee();
        List<Salary> salaries = salaryService.getSalariesByEmployee(employee);
        model.addAttribute("employee", employee);
        model.addAttribute("salaries", salaries);
        return "employee/salary/list";
    }
    
    @GetMapping("/view/{id}")
    public String viewSalary(@PathVariable Long id, Model model) {
        Employee employee = getCurrentEmployee();
        Salary salary = salaryService.getSalaryById(id)
                .orElseThrow(() -> new RuntimeException("Salary record not found"));
        
        // Verify ownership
        if (!salary.getEmployee().getId().equals(employee.getId())) {
            throw new RuntimeException("Unauthorized access");
        }
        
        model.addAttribute("employee", employee);
        model.addAttribute("salary", salary);
        return "employee/salary/view";
    }
    
    @GetMapping("/download/{id}")
    @ResponseBody
    public ResponseEntity<Resource> downloadPayslip(@PathVariable Long id) {
        try {
            Employee employee = getCurrentEmployee();
            Salary salary = salaryService.getSalaryById(id)
                    .orElseThrow(() -> new RuntimeException("Salary record not found"));
            
            // Verify ownership
            if (!salary.getEmployee().getId().equals(employee.getId())) {
                throw new RuntimeException("Unauthorized access");
            }
            
            // Check if payslip exists
            if (salary.getPayslipPath() == null || salary.getPayslipPath().isEmpty()) {
                throw new RuntimeException("Payslip not available");
            }
            
            Path filePath = Paths.get(salary.getPayslipPath());
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                String contentType = "application/pdf";
                String fileName = "payslip_" + employee.getUsername() + "_" + 
                    salary.getStartDate().format(DateTimeFormatter.ofPattern("yyyy-MM")) + ".pdf";
                
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                        .body(resource);
            } else {
                throw new RuntimeException("Payslip file not found");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error downloading payslip: " + e.getMessage());
        }
    }
}
