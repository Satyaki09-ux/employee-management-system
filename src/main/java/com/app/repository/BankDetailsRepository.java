package com.app.repository;

import com.app.model.BankDetails;
import com.app.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankDetailsRepository extends JpaRepository<BankDetails, Long> {
    List<BankDetails> findByEmployee(Employee employee);
    Optional<BankDetails> findByEmployeeAndIsPrimary(Employee employee, Boolean isPrimary);

}
