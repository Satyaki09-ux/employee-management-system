package com.app.service;

import com.app.model.Lead;
import com.app.model.Employee;
import com.app.repository.LeadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class LeadService {
    
    @Autowired
    private LeadRepository leadRepository;
    
    @Autowired
    private EmployeeService employeeService;
    
    public List<Lead> getAllLeads() {
        return leadRepository.findAllByOrderByCreatedAtDesc();
    }
    
    public List<Lead> getLeadsByStatus(String status) {
        return leadRepository.findByStatusOrderByCreatedAtDesc(status);
    }
    
    public List<Lead> getLeadsByAssignedEmployee(Long employeeId) {
        return leadRepository.findByAssignedToIdOrderByCreatedAtDesc(employeeId);
    }
    
    public Optional<Lead> getLeadById(Long id) {
        return leadRepository.findById(id);
    }
    
    public Lead saveLead(Lead lead) {
        return leadRepository.save(lead);
    }
    
    public Lead updateLeadStatus(Long id, String status) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lead not found"));
        lead.setStatus(status);
        return leadRepository.save(lead);
    }
    
    public Lead assignLead(Long id, Long employeeId) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lead not found"));
        if (employeeId != null) {
            Employee employee = employeeService.getEmployeeById(employeeId)
                    .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
            lead.setAssignedTo(employee);
        } else {
            lead.setAssignedTo(null);
        }
        return leadRepository.save(lead);
    }
    
    public Lead updateLeadNotes(Long id, String notes) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lead not found"));
        lead.setNotes(notes);
        return leadRepository.save(lead);
    }
    
    public void deleteLead(Long id) {
        leadRepository.deleteById(id);
    }
}
