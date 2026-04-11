package com.app.service;

import com.app.model.Customer;
import com.app.model.Employee;
import com.app.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class CustomerService {
    
    @Autowired
    private CustomerRepository customerRepository;
    
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }
    
    public List<Customer> getActiveCustomers() {
        return customerRepository.findByStatus("ACTIVE");
    }
    
    public Optional<Customer> getCustomerById(Long id) {
        return customerRepository.findById(id);
    }
    
    public Customer saveCustomer(Customer customer, Employee creatingEmployee) {
        if (creatingEmployee != null && customer.getId() == null) {
            // Populate hierarchy fields based on the employee creating the customer
            populateHierarchyFields(customer, creatingEmployee);
        }
        return customerRepository.save(customer);
    }
    
    public Customer saveCustomer(Customer customer) {
        return customerRepository.save(customer);
    }
    
    /**
     * Populates hierarchy fields based on the employee creating the customer.
     * - Promoter: Sets promoterId
     * - Zonal Head: Sets zonalHeadId and promoterId (from reporting manager)
     * - Cluster Head: Sets clusterHeadId, zonalHeadId, and promoterId (from reporting chain)
     * - Area Sales Manager: Sets areaSalesManagerId, clusterHeadId, zonalHeadId, and promoterId (from reporting chain)
     */
    private void populateHierarchyFields(Customer customer, Employee employee) {
        String hierarchyLevel = employee.getHierarchyLevel();
        
        if ("PROMOTER".equals(hierarchyLevel)) {
            customer.setPromoterId(employee.getId());
        } else if ("ZONAL_HEAD".equals(hierarchyLevel)) {
            customer.setZonalHeadId(employee.getId());
            if (employee.getReportingManager() != null) {
                customer.setPromoterId(employee.getReportingManager().getId());
            }
        } else if ("CLUSTER_HEAD".equals(hierarchyLevel)) {
            customer.setClusterHeadId(employee.getId());
            if (employee.getReportingManager() != null) {
                Employee zonalHead = employee.getReportingManager();
                customer.setZonalHeadId(zonalHead.getId());
                if (zonalHead.getReportingManager() != null) {
                    customer.setPromoterId(zonalHead.getReportingManager().getId());
                }
            }
        } else if ("AREA_SALES_MANAGER".equals(hierarchyLevel)) {
            customer.setAreaSalesManagerId(employee.getId());
            if (employee.getReportingManager() != null) {
                Employee clusterHead = employee.getReportingManager();
                customer.setClusterHeadId(clusterHead.getId());
                if (clusterHead.getReportingManager() != null) {
                    Employee zonalHead = clusterHead.getReportingManager();
                    customer.setZonalHeadId(zonalHead.getId());
                    if (zonalHead.getReportingManager() != null) {
                        customer.setPromoterId(zonalHead.getReportingManager().getId());
                    }
                }
            }
        }
    }
    
    public void deleteCustomer(Long id) {
        customerRepository.deleteById(id);
    }
    
    public List<Customer> searchCustomers(String keyword) {
        List<Customer> results = customerRepository.findByContactPersonContainingIgnoreCase(keyword);
        results.addAll(customerRepository.findByEmailContainingIgnoreCase(keyword));
        results.addAll(customerRepository.findByPhoneContaining(keyword));
        return results.stream().distinct().toList();
    }
    
    public boolean emailAndPhoneExists(String email, String phone) {
        return customerRepository.findByEmailAndPhone(email, phone) != null;
    }
    
    public boolean emailAndPhoneExistsForOtherCustomer(String email, String phone, Long id) {
        Customer customer = customerRepository.findByEmailAndPhone(email, phone);
        return customer != null && !customer.getId().equals(id);
    }
    
    @Autowired
    private EmployeeService employeeService;
    
    /**
     * Gets all customers visible to the given employee.
     * Employees can see customers:
     * 1. Assigned to them (based on hierarchy fields)
     * 2. Created by them (based on createdBy field)
     */
    public List<Customer> getCustomersForEmployee(Employee employee) {
        if (employee == null || employee.getId() == null) {
            return List.of();
        }
        
        // Get customers assigned based on hierarchy
        List<Customer> assignedCustomers = getCustomersForEmployeeByHierarchy(employee);
        
        // Get customers created by this employee
        String employeeIdentifier = "EMPLOYEE:" + employee.getUsername();
        List<Customer> createdCustomers = customerRepository.findAll().stream()
                .filter(c -> employeeIdentifier.equals(c.getCreatedBy()))
                .toList();
        
        // Combine and remove duplicates
        java.util.Set<Customer> allCustomers = new java.util.HashSet<>(assignedCustomers);
        allCustomers.addAll(createdCustomers);
        
        return allCustomers.stream().toList();
    }
    
    /**
     * Gets all customers visible to the given employee based on their hierarchy level.
     * - Promoter: Customers where promoterId = employee.id
     * - Zonal Head: Customers where zonalHeadId = employee.id OR (clusterHeadId in reporting cluster heads OR areaSalesManagerId in reporting ASMs)
     * - Cluster Head: Customers where clusterHeadId = employee.id OR areaSalesManagerId in reporting ASMs
     * - Area Sales Manager: Customers where areaSalesManagerId = employee.id
     */
    private List<Customer> getCustomersForEmployeeByHierarchy(Employee employee) {
        if (employee == null || employee.getId() == null) {
            return List.of();
        }
        
        String hierarchyLevel = employee.getHierarchyLevel();
        Long employeeId = employee.getId();
        
        if ("PROMOTER".equals(hierarchyLevel)) {
            return customerRepository.findByPromoterId(employeeId);
        } else if ("ZONAL_HEAD".equals(hierarchyLevel)) {
            // Get customers directly assigned to this zonal head
            List<Customer> customers = new java.util.ArrayList<>(customerRepository.findByZonalHeadId(employeeId));
            
            // Get all cluster heads reporting to this zonal head
            List<Employee> clusterHeads = employeeService.getAllEmployees().stream()
                    .filter(e -> "CLUSTER_HEAD".equals(e.getHierarchyLevel()) 
                            && e.getReportingManager() != null 
                            && e.getReportingManager().getId().equals(employeeId))
                    .toList();
            
            // Get customers of those cluster heads
            for (Employee clusterHead : clusterHeads) {
                customers.addAll(customerRepository.findByClusterHeadId(clusterHead.getId()));
            }
            
            // Get all ASMs reporting to those cluster heads
            for (Employee clusterHead : clusterHeads) {
                List<Employee> asms = employeeService.getAllEmployees().stream()
                        .filter(e -> "AREA_SALES_MANAGER".equals(e.getHierarchyLevel())
                                && e.getReportingManager() != null
                                && e.getReportingManager().getId().equals(clusterHead.getId()))
                        .toList();
                for (Employee asm : asms) {
                    customers.addAll(customerRepository.findByAreaSalesManagerId(asm.getId()));
                }
            }
            
            return customers.stream().distinct().toList();
        } else if ("CLUSTER_HEAD".equals(hierarchyLevel)) {
            // Get customers directly assigned to this cluster head
            List<Customer> customers = new java.util.ArrayList<>(customerRepository.findByClusterHeadId(employeeId));
            
            // Get all ASMs reporting to this cluster head
            List<Employee> asms = employeeService.getAllEmployees().stream()
                    .filter(e -> "AREA_SALES_MANAGER".equals(e.getHierarchyLevel())
                            && e.getReportingManager() != null
                            && e.getReportingManager().getId().equals(employeeId))
                    .toList();
            
            // Get customers of those ASMs
            for (Employee asm : asms) {
                customers.addAll(customerRepository.findByAreaSalesManagerId(asm.getId()));
            }
            
            return customers.stream().distinct().toList();
        } else if ("AREA_SALES_MANAGER".equals(hierarchyLevel)) {
            return customerRepository.findByAreaSalesManagerId(employeeId);
        }
        
        return List.of();
    }

    /**
     * Assigns a customer to an employee and updates the hierarchy fields
     */
    public void assignCustomerToEmployee(Customer customer, Employee targetEmployee, Employee assigner) {
        if (customer == null || targetEmployee == null) {
            throw new IllegalArgumentException("Customer and target employee cannot be null");
        }

        // Update customer hierarchy based on target employee's position
        String hierarchyLevel = targetEmployee.getHierarchyLevel();

        if ("PROMOTER".equals(hierarchyLevel)) {
            customer.setPromoterId(targetEmployee.getId());
            customer.setZonalHeadId(null);
            customer.setClusterHeadId(null);
            customer.setAreaSalesManagerId(null);
        } else if ("ZONAL_HEAD".equals(hierarchyLevel)) {
            customer.setZonalHeadId(targetEmployee.getId());
            customer.setClusterHeadId(null);
            customer.setAreaSalesManagerId(null);
            // Keep promoter if it exists
        } else if ("CLUSTER_HEAD".equals(hierarchyLevel)) {
            customer.setClusterHeadId(targetEmployee.getId());
            customer.setAreaSalesManagerId(null);
            // Keep higher level assignments
        } else if ("AREA_SALES_MANAGER".equals(hierarchyLevel)) {
            customer.setAreaSalesManagerId(targetEmployee.getId());
            // Keep higher level assignments
        }

        // Update audit fields
        customer.setLastUpdatedBy(assigner.getFullName());

        customerRepository.save(customer);
    }
}

