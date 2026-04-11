package com.app.admin.controller;

import com.app.model.Attendance;
import com.app.model.Employee;
import com.app.service.AttendanceService;
import com.app.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin/attendances")
public class AttendanceController {
    
    @Autowired
    private AttendanceService attendanceService;
    
    @Autowired
    private EmployeeService employeeService;
    
    @GetMapping
    public String listAttendances(Model model,
                                 @RequestParam(required = false) Long employeeId,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<Attendance> attendances;
        
        if (employeeId != null) {
            attendances = attendanceService.getAttendancesByEmployee(employeeId);
            model.addAttribute("selectedEmployeeId", employeeId);
        } else if (date != null) {
            attendances = attendanceService.getAttendancesByDate(date);
            model.addAttribute("selectedDate", date);
        } else {
            attendances = attendanceService.getAttendancesByDate(LocalDate.now());
            model.addAttribute("selectedDate", LocalDate.now());
        }
        
        model.addAttribute("attendances", attendances);
        model.addAttribute("employees", employeeService.getActiveEmployees());
        return "admin/attendances/list";
    }
    
    @GetMapping("/new")
    public String showAttendanceForm(Model model) {
        model.addAttribute("attendance", new Attendance());
        model.addAttribute("employees", employeeService.getActiveEmployees());
        return "admin/attendances/form";
    }
    
    @GetMapping("/edit/{id}")
    public String editAttendance(@PathVariable Long id, Model model) {
        Attendance attendance = attendanceService.getAttendanceById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid attendance ID: " + id));
        model.addAttribute("attendance", attendance);
        model.addAttribute("employees", employeeService.getActiveEmployees());
        return "admin/attendances/form";
    }
    
    @PostMapping("/save")
    public String saveAttendance(@ModelAttribute Attendance attendance,
                                @RequestParam(required = false) Long employeeId,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime checkIn,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime checkOut,
                                BindingResult result,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (employeeId != null) {
            Employee employee = employeeService.getEmployeeById(employeeId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid employee ID: " + employeeId));
            attendance.setEmployee(employee);
        }
        
        // Set checkIn and checkOut if provided
        if (checkIn != null) {
            attendance.setCheckIn(checkIn);
        }
        if (checkOut != null) {
            attendance.setCheckOut(checkOut);
        }
        
        // If checkIn is not set but date is set, set checkIn to start of day
        if (attendance.getCheckIn() == null && attendance.getDate() != null) {
            attendance.setCheckIn(attendance.getDate().atStartOfDay());
        }
        
        if (result.hasErrors()) {
            model.addAttribute("employees", employeeService.getActiveEmployees());
            return "admin/attendances/form";
        }
        
        attendanceService.saveAttendance(attendance);
        redirectAttributes.addFlashAttribute("message", "Attendance saved successfully!");
        return "redirect:/admin/attendances";
    }
}
