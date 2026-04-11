package com.app.repository;

import com.app.model.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long> {
    List<Lead> findByStatusOrderByCreatedAtDesc(String status);
    List<Lead> findByAssignedToIdOrderByCreatedAtDesc(Long employeeId);
    List<Lead> findAllByOrderByCreatedAtDesc();
}
