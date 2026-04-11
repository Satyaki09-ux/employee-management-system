package com.app.util;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class to manage admin permissions for menu items
 */
public class AdminPermissions {
    
    // Permission constants
    public static final String DASHBOARD = "DASHBOARD";
    public static final String ADMINS = "ADMINS";
    public static final String EMPLOYEE_MANAGEMENT = "EMPLOYEE_MANAGEMENT";
    public static final String ATTENDANCE = "ATTENDANCE";
    public static final String CUSTOMERS = "CUSTOMERS";
    public static final String PRODUCTS = "PRODUCTS";
    public static final String SALES = "SALES";
    public static final String SALES_TARGETS = "SALES_TARGETS";
    public static final String TRANSACTIONS = "TRANSACTIONS";
    public static final String SALARY_MANAGEMENT = "SALARY_MANAGEMENT";
    public static final String CLAIMS = "CLAIMS";
    public static final String LEADS = "LEADS";
    
    /**
     * Get all available permissions
     */
    public static List<String> getAllPermissions() {
        return Arrays.asList(
            DASHBOARD,
            ADMINS,
            EMPLOYEE_MANAGEMENT,
            ATTENDANCE,
            CUSTOMERS,
            PRODUCTS,
            SALES,
            SALES_TARGETS,
            TRANSACTIONS,
            SALARY_MANAGEMENT,
            CLAIMS,
            LEADS
        );
    }
    
    /**
     * Get permission labels for display
     */
    public static Map<String, String> getPermissionLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(DASHBOARD, "Dashboard");
        labels.put(ADMINS, "Admins Management");
        labels.put(EMPLOYEE_MANAGEMENT, "Employee Management");
        labels.put(ATTENDANCE, "Attendance");
        labels.put(CUSTOMERS, "Customers");
        labels.put(PRODUCTS, "Products");
        labels.put(SALES, "Sales");
        labels.put(SALES_TARGETS, "Sales Targets");
        labels.put(TRANSACTIONS, "Transactions");
        labels.put(SALARY_MANAGEMENT, "Salary Management");
        labels.put(CLAIMS, "Claims");
        labels.put(LEADS, "Leads");
        return labels;
    }
    
    /**
     * Check if admin has a specific permission
     */
    public static boolean hasPermission(String permissions, String permission) {
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }
        
        // Check if SUPER_ADMIN (has all permissions)
        if (permissions.contains("SUPER_ADMIN")) {
            return true;
        }
        
        // Check comma-separated permissions
        String[] perms = permissions.split(",");
        for (String perm : perms) {
            if (perm.trim().equals(permission)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Parse permissions string to list
     */
    public static List<String> parsePermissions(String permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(permissions.split(","));
    }
    
    /**
     * Convert list of permissions to comma-separated string
     */
    public static String formatPermissions(List<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return "";
        }
        return String.join(",", permissions);
    }
}
