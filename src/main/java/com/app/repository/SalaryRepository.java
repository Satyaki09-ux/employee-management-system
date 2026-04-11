package com.app.repository;

import com.app.model.Salary;
import com.app.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalaryRepository extends JpaRepository<Salary, Long> {
    List<Salary> findByEmployee(Employee employee);
    List<Salary> findByEmployeeOrderByStartDateDesc(Employee employee);
    List<Salary> findByEmployeeAndStatus(Employee employee, String status);
}
