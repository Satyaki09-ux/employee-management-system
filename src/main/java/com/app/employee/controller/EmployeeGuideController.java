package com.app.employee.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/employee")
public class EmployeeGuideController {

    /**
     * Display the employee portal user guide
     * This is a public endpoint accessible to all authenticated employees
     */
    @GetMapping("/guide")
    public String showGuide() {
        return "employee/guide";
    }
}
