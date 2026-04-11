package com.app.service;

import com.app.model.SalesTarget;
import com.app.model.Employee;
import com.app.model.Sale;
import com.app.repository.SalesTargetRepository;
import com.app.repository.SaleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SalesTargetService {
    
    @Autowired
    private SalesTargetRepository salesTargetRepository;
    
    @Autowired
    private SaleRepository saleRepository;
    
    @Autowired
    private EmployeeService employeeService;
    
    public List<SalesTarget> getSalesTargetsByEmployee(Employee employee) {
        return salesTargetRepository.findByEmployeeOrderByPeriodStartDesc(employee);
    }
    
    public List<SalesTarget> getSalesTargetsByEmployeeAndProduct(Employee employee, com.app.model.Product product) {
        return salesTargetRepository.findByEmployeeAndProduct(employee, product);
    }
    
    public Optional<SalesTarget> getCurrentSalesTarget(Employee employee) {
        LocalDate today = LocalDate.now();
        return salesTargetRepository.findByEmployeeAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqual(
                employee, today, today);
    }
    
    public Optional<SalesTarget> getSalesTargetById(Long id) {
        return salesTargetRepository.findById(id);
    }
    
    public SalesTarget saveSalesTarget(SalesTarget salesTarget) {
        return salesTargetRepository.save(salesTarget);
    }
    
    public void deleteSalesTarget(Long id) {
        salesTargetRepository.deleteById(id);
    }
    
    public void updateAchievedAmount(SalesTarget salesTarget) {
        LocalDate startDate = salesTarget.getPeriodStart();
        LocalDate endDate = salesTarget.getPeriodEnd();
        
        List<Sale> sales = saleRepository.findByCreatedByIdAndSaleDateBetween(
                salesTarget.getEmployee().getId(), startDate, endDate);
        
        // If target is product-specific, filter by product
        if (salesTarget.getProduct() != null) {
            sales = sales.stream()
                    .filter(s -> s.getProduct() != null && s.getProduct().getId().equals(salesTarget.getProduct().getId()))
                    .toList();
        }
        
        BigDecimal total = sales.stream()
                .filter(s -> "COMPLETED".equals(s.getSaleStatus()))
                .map(Sale::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        salesTarget.setAchievedAmount(total);
        salesTargetRepository.save(salesTarget);
    }
    
    public void updateAllAchievedAmounts() {
        List<SalesTarget> targets = salesTargetRepository.findAll();
        for (SalesTarget target : targets) {
            updateAchievedAmount(target);
        }
    }
    
    public List<SalesTarget> getAllSalesTargets() {
        return salesTargetRepository.findAll();
    }
    
    public List<SalesTarget> getSalesTargetsByDateRange(LocalDate startDate, LocalDate endDate) {
        return salesTargetRepository.findAll().stream()
                .filter(target -> !target.getPeriodStart().isAfter(endDate) && !target.getPeriodEnd().isBefore(startDate))
                .toList();
    }
    
    public void calculateAndUpdateSalary(SalesTarget salesTarget) {
        // Update achieved amount first
        updateAchievedAmount(salesTarget);
        
        // Calculate salary based on target achievement
        BigDecimal calculatedSalary = calculateSalary(salesTarget);
        salesTarget.setCalculatedSalary(calculatedSalary);
        
        // Mark as calculated if target is achieved
        if (salesTarget.getProgressPercentage().compareTo(BigDecimal.valueOf(100)) >= 0) {
            salesTarget.setSalaryCalculated(true);
        }
        
        salesTargetRepository.save(salesTarget);
        
        // Update employee salary if target is achieved
        if (salesTarget.getEmployee() != null && salesTarget.getProgressPercentage().compareTo(BigDecimal.valueOf(100)) >= 0) {
            Employee employee = salesTarget.getEmployee();
            employee.setSalary(calculatedSalary.doubleValue());
            employeeService.saveEmployee(employee);
        }
    }
    
    public void calculateSalariesForCompletedMonths() {
        LocalDate today = LocalDate.now();
        List<SalesTarget> targets = salesTargetRepository.findAll();
        
        for (SalesTarget target : targets) {
            // Only calculate for targets that have ended and haven't been calculated yet
            if (target.getPeriodEnd().isBefore(today) && !Boolean.TRUE.equals(target.getSalaryCalculated())) {
                calculateAndUpdateSalary(target);
            }
        }
    }
    
    public void recalculateAllSalaries() {
        List<SalesTarget> targets = salesTargetRepository.findAll();
        for (SalesTarget target : targets) {
            updateAchievedAmount(target);
            // Calculate salary if target is achieved
            if (target.getProgressPercentage().compareTo(BigDecimal.valueOf(100)) >= 0 
                    && !Boolean.TRUE.equals(target.getSalaryCalculated())) {
                BigDecimal calculatedSalary = calculateSalary(target);
                target.setCalculatedSalary(calculatedSalary);
                target.setSalaryCalculated(true);
                
                // Update employee salary
                if (target.getEmployee() != null) {
                    Employee employee = target.getEmployee();
                    employee.setSalary(calculatedSalary.doubleValue());
                    employeeService.saveEmployee(employee);
                }
            }
        }
    }
    
    private BigDecimal calculateSalary(SalesTarget salesTarget) {
        BigDecimal base = salesTarget.getBaseSalary() != null 
                ? salesTarget.getBaseSalary() 
                : BigDecimal.ZERO;
        
        if (salesTarget.getTargetAmount() == null || salesTarget.getTargetAmount().compareTo(BigDecimal.ZERO) == 0) {
            return base;
        }
        
        BigDecimal progressPercentage = salesTarget.getProgressPercentage();
        
        // If target is achieved (100% or more), add commission
        if (progressPercentage.compareTo(BigDecimal.valueOf(100)) >= 0) {
            BigDecimal commissionRate = salesTarget.getCommissionRate() != null 
                    ? salesTarget.getCommissionRate() 
                    : BigDecimal.ZERO;
            
            // Calculate commission: commissionRate% of achieved amount
            BigDecimal commission = salesTarget.getAchievedAmount()
                    .multiply(commissionRate)
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            
            return base.add(commission);
        } else {
            // If target not achieved, return base salary only
            return base;
        }
    }

    public List<SalesTarget> getSalesTargetsForEmployeeHierarchy(Employee employee) {
        if (employee == null) return List.of();
        List<Long> ids = employeeService.getAllReportingEmployeeIds(employee);
        // your method already includes self, so no need to add again if it does
        return salesTargetRepository.findByEmployee_IdInOrderByPeriodStartDesc(ids);
    }
}

