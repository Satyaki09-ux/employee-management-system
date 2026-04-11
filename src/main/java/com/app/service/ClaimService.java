package com.app.service;

import com.app.model.Claim;
import com.app.model.Employee;
import com.app.model.Admin;
import com.app.repository.ClaimRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ClaimService {
    
    @Autowired
    private ClaimRepository claimRepository;
    
    public List<Claim> getClaimsByEmployee(Employee employee) {
        return claimRepository.findByEmployee(employee);
    }
    
    public List<Claim> getClaimsByEmployeeAndStatus(Employee employee, String status) {
        return claimRepository.findByEmployeeAndStatus(employee, status);
    }
    
    public List<Claim> getPendingClaims() {
        return claimRepository.findByStatus("PENDING");
    }
    
    public List<Claim> getPendingClaimsForManager(Employee manager) {
        return claimRepository.findByAssignedToAndStatus(manager, "PENDING");
    }
    
    public Claim saveClaim(Claim claim) {
        // Assign to reporting manager if not already assigned
        if (claim.getAssignedTo() == null && claim.getEmployee() != null 
                && claim.getEmployee().getReportingManager() != null) {
            claim.setAssignedTo(claim.getEmployee().getReportingManager());
        }
        
        return claimRepository.save(claim);
    }
    
    public Optional<Claim> getClaimById(Long id) {
        return claimRepository.findById(id);
    }
    
    public Claim approveClaim(Long claimId, Employee approvedBy, String remarks) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found"));
        claim.setStatus("APPROVED");
        claim.setApprovedBy(approvedBy);
        claim.setApprovedAt(LocalDateTime.now());
        claim.setRemarks(remarks);
        return claimRepository.save(claim);
    }
    
    public Claim rejectClaim(Long claimId, Employee rejectedBy, String remarks) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found"));
        claim.setStatus("REJECTED");
        claim.setApprovedBy(rejectedBy);
        claim.setApprovedAt(LocalDateTime.now());
        claim.setRemarks(remarks);
        return claimRepository.save(claim);
    }
    
    public void deleteClaim(Long id) {
        claimRepository.deleteById(id);
    }
    
    /**
     * Get all approved claims
     */
    public List<Claim> getApprovedClaims() {
        return claimRepository.findByStatus("APPROVED");
    }
    
    /**
     * Mark claim as paid
     */
    @Transactional
    public Claim markClaimAsPaid(Long claimId, Admin paidBy, String paymentMethod, String transactionReference) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found"));
        
        if (!"APPROVED".equals(claim.getStatus())) {
            throw new IllegalStateException("Only approved claims can be marked as paid");
        }
        
        claim.setIsPaid(true);
        claim.setPaidBy(paidBy);
        claim.setPaidAt(LocalDateTime.now());
        claim.setPaymentMethod(paymentMethod);
        claim.setTransactionReference(transactionReference);
        
        return claimRepository.save(claim);
    }
}
