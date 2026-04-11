package com.app.service;

import com.app.model.LeaveRequest;
import com.app.model.Employee;
import com.app.repository.LeaveRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class LeaveRequestService {
    
    @Autowired
    private LeaveRequestRepository leaveRequestRepository;
    
    public List<LeaveRequest> getLeaveRequestsByEmployee(Employee employee) {
        return leaveRequestRepository.findByEmployee(employee);
    }
    
    public List<LeaveRequest> getLeaveRequestsByEmployeeAndStatus(Employee employee, String status) {
        return leaveRequestRepository.findByEmployeeAndStatus(employee, status);
    }
    
    public List<LeaveRequest> getPendingLeaveRequestsForManager(Employee manager) {
        return leaveRequestRepository.findByAssignedToAndStatus(manager, "PENDING");
    }
    
    public LeaveRequest saveLeaveRequest(LeaveRequest leaveRequest) {
        // Calculate number of days if not provided
        if (leaveRequest.getNumberOfDays() == null) {
            long days = ChronoUnit.DAYS.between(leaveRequest.getStartDate(), leaveRequest.getEndDate()) + 1;
            leaveRequest.setNumberOfDays((int) days);
        }
        
        // Assign to reporting manager if not already assigned
        if (leaveRequest.getAssignedTo() == null && leaveRequest.getEmployee() != null 
                && leaveRequest.getEmployee().getReportingManager() != null) {
            leaveRequest.setAssignedTo(leaveRequest.getEmployee().getReportingManager());
        }
        
        return leaveRequestRepository.save(leaveRequest);
    }
    
    public Optional<LeaveRequest> getLeaveRequestById(Long id) {
        return leaveRequestRepository.findById(id);
    }
    
    public LeaveRequest approveLeaveRequest(Long leaveRequestId, Employee approvedBy, String remarks) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new RuntimeException("Leave request not found"));
        leaveRequest.setStatus("APPROVED");
        leaveRequest.setApprovedBy(approvedBy);
        leaveRequest.setApprovedAt(LocalDateTime.now());
        leaveRequest.setRemarks(remarks);
        return leaveRequestRepository.save(leaveRequest);
    }
    
    public LeaveRequest rejectLeaveRequest(Long leaveRequestId, Employee rejectedBy, String remarks) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new RuntimeException("Leave request not found"));
        leaveRequest.setStatus("REJECTED");
        leaveRequest.setApprovedBy(rejectedBy);
        leaveRequest.setApprovedAt(LocalDateTime.now());
        leaveRequest.setRemarks(remarks);
        return leaveRequestRepository.save(leaveRequest);
    }
    
    public LeaveRequest cancelLeaveRequest(Long leaveRequestId) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new RuntimeException("Leave request not found"));
        if (!leaveRequest.getStatus().equals("APPROVED")) {
            leaveRequest.setStatus("CANCELLED");
            return leaveRequestRepository.save(leaveRequest);
        }
        throw new RuntimeException("Cannot cancel an approved leave request");
    }
    
    public void deleteLeaveRequest(Long id) {
        leaveRequestRepository.deleteById(id);
    }
}
