package com.app.service;

import com.app.model.Salary;
import com.app.model.Employee;
import com.app.repository.SalaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SalaryService {
    
    @Autowired
    private SalaryRepository salaryRepository;
    
    public List<Salary> getSalariesByEmployee(Employee employee) {
        return salaryRepository.findByEmployeeOrderByStartDateDesc(employee);
    }
    
    public List<Salary> getSalariesByEmployeeAndStatus(Employee employee, String status) {
        return salaryRepository.findByEmployeeAndStatus(employee, status);
    }
    
    public Salary saveSalary(Salary salary) {
        return salaryRepository.save(salary);
    }
    
    public Optional<Salary> getSalaryById(Long id) {
        return salaryRepository.findById(id);
    }
}
