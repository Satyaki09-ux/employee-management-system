package com.app.service;

import com.app.model.Attendance;
import com.app.model.Employee;
import com.app.repository.AttendanceRepository;
import com.app.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AttendanceService {
    
    @Autowired
    private AttendanceRepository attendanceRepository;
    
    @Autowired
    private EmployeeRepository employeeRepository;

    private static final ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");
    
    public List<Attendance> getAllAttendances() {
        return attendanceRepository.findAll();
    }
    
    public List<Attendance> getAttendancesByEmployee(Long employeeId) {
        Optional<Employee> employee = employeeRepository.findById(employeeId);
        return employee.map(attendanceRepository::findByEmployee).orElse(List.of());
    }
    
    public List<Attendance> getAttendancesByDate(LocalDate date) {
        return attendanceRepository.findByDate(date);
    }
    
    public Optional<Attendance> getAttendanceById(Long id) {
        return attendanceRepository.findById(id);
    }
    
    public Attendance checkIn(Long employeeId) {
        Optional<Employee> employeeOpt = employeeRepository.findById(employeeId);
        if (employeeOpt.isEmpty()) {
            throw new RuntimeException("Employee not found");
        }
        
        Employee employee = employeeOpt.get();
//        LocalDate today = LocalDate.now();
        LocalDate today = LocalDate.now(INDIA_ZONE);
        
        Optional<Attendance> existingAttendance = attendanceRepository.findByEmployeeAndDate(employee, today);
        
        if (existingAttendance.isPresent()) {
            Attendance attendance = existingAttendance.get();
            if (attendance.getCheckIn() != null) {
                throw new RuntimeException("Employee already checked in today");
            }
//            attendance.setCheckIn(LocalDateTime.now());
            attendance.setCheckIn(LocalDateTime.now(INDIA_ZONE));
            attendance.setStatus("PRESENT");
            return attendanceRepository.save(attendance);
        } else {
            Attendance attendance = new Attendance();
            attendance.setEmployee(employee);
            attendance.setDate(today);
            attendance.setCheckIn(LocalDateTime.now());
            attendance.setStatus("PRESENT");
            return attendanceRepository.save(attendance);
        }
    }
    
    public Attendance checkOut(Long employeeId) {
        Optional<Employee> employeeOpt = employeeRepository.findById(employeeId);
        if (employeeOpt.isEmpty()) {
            throw new RuntimeException("Employee not found");
        }
        
        Employee employee = employeeOpt.get();
//        LocalDate today = LocalDate.now();
        LocalDate today = LocalDate.now(INDIA_ZONE);
        
        Optional<Attendance> attendanceOpt = attendanceRepository.findByEmployeeAndDate(employee, today);
        
        if (attendanceOpt.isEmpty() || attendanceOpt.get().getCheckIn() == null) {
            throw new RuntimeException("Employee has not checked in today");
        }
        
        Attendance attendance = attendanceOpt.get();
        if (attendance.getCheckOut() != null) {
            throw new RuntimeException("Employee already checked out today");
        }
        
//        attendance.setCheckOut(LocalDateTime.now());
        attendance.setCheckOut(LocalDateTime.now(INDIA_ZONE));
        return attendanceRepository.save(attendance);
    }
    
    public Attendance saveAttendance(Attendance attendance) {
        return attendanceRepository.save(attendance);
    }
    
    public void deleteAttendance(Long id) {
        attendanceRepository.deleteById(id);
    }
    
    public List<Attendance> getAttendancesByEmployeeAndDateRange(Long employeeId, LocalDate startDate, LocalDate endDate) {
        Optional<Employee> employee = employeeRepository.findById(employeeId);
        return employee.map(emp -> attendanceRepository.findByEmployeeAndDateBetween(emp, startDate, endDate))
                      .orElse(List.of());
    }
    
    public Attendance getTodayAttendance(Long employeeId) {
        Optional<Employee> employee = employeeRepository.findById(employeeId);
        if (employee.isEmpty()) {
            return null;
        }
        LocalDate today = LocalDate.now();
        return attendanceRepository.findByEmployeeAndDate(employee.get(), today).orElse(null);
    }
}

