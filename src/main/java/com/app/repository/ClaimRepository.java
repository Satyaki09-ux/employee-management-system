package com.app.repository;

import com.app.model.Claim;
import com.app.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {
    List<Claim> findByEmployee(Employee employee);
    List<Claim> findByEmployeeAndStatus(Employee employee, String status);
    List<Claim> findByStatus(String status);
    List<Claim> findByAssignedToAndStatus(Employee manager, String status);
    List<Claim> findByAssignedTo(Employee manager);
}
