package com.app.repository;

import com.app.model.LeaveRequest;
import com.app.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByEmployee(Employee employee);
    List<LeaveRequest> findByEmployeeAndStatus(Employee employee, String status);
    List<LeaveRequest> findByApprovedBy(Employee manager);
    List<LeaveRequest> findByApprovedByAndStatus(Employee manager, String status);
    List<LeaveRequest> findByAssignedToAndStatus(Employee manager, String status);
    List<LeaveRequest> findByAssignedTo(Employee manager);
    List<LeaveRequest> findByEmployeeAndStartDateBetween(Employee employee, LocalDate start, LocalDate end);
}
