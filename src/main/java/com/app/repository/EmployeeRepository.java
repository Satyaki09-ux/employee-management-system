package com.app.repository;

import com.app.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findByStatus(String status);
    List<Employee> findByDepartment(String department);
    Employee findByEmail(String email);
    Employee findByPhone(String phone);
    Employee findByEmailAndPhone(String email, String phone);
    Employee findByUsername(String username);
    List<Employee> findByFirstNameContainingOrLastNameContaining(String firstName, String lastName);
    List<Employee> findByHierarchyLevel(String hierarchyLevel);

    @Query("SELECT e FROM Employee e WHERE e.hierarchyLevel = :level AND (LOWER(e.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(e.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Employee> findByHierarchyLevelAndNameContaining(@Param("level") String hierarchyLevel, @Param("keyword") String keyword);

    @Query("SELECT e FROM Employee e WHERE e.reportingManager.id = :managerId AND e.status = 'ACTIVE'")
    List<Employee> findDirectReportsByManagerId(@Param("managerId") Long managerId);

    List<Employee> findByReportingManager_Id(Long managerId);
}

