package com.app.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OfficeExecutiveDesignations {

    private static final Map<String, List<String>> DEPARTMENT_DESIGNATIONS =
            new LinkedHashMap<>();

    static {
        DEPARTMENT_DESIGNATIONS.put("HR",List.of(
                "HR Manager",
                "HR Executive",
                "Human Resource"
        ));

        DEPARTMENT_DESIGNATIONS.put("Executive",List.of(
                "CEO"
        ));

        DEPARTMENT_DESIGNATIONS.put("Fintech",List.of("Fintech Lead"));

        DEPARTMENT_DESIGNATIONS.put("Sales",List.of(
                "Sales Lead",
                "Sales Executive",
                "Inside Sales Executive"
        ));

        DEPARTMENT_DESIGNATIONS.put("Product",List.of(
                "Product Manager"
        ));

        DEPARTMENT_DESIGNATIONS.put("Tech",List.of(
                "Tech Lead"
        ));

        DEPARTMENT_DESIGNATIONS.put("Manager",List.of(
                "Back Office",
                "Daily Operation",
                "Operation Team",
                "Operations Team",
                "Operation Executive",
                "Back Office Executive",
                "Office Executive"
        ));

        DEPARTMENT_DESIGNATIONS.put("Admin / Pantry", List.of(
                "Chef"
        ));

        DEPARTMENT_DESIGNATIONS.put("Admin / Support", List.of(
                "Driver",
                "Driver",
                "Back Office Boy",
                "Office Caretaker"
        ));

        DEPARTMENT_DESIGNATIONS.put("Management", List.of(
                "Manager",
                "Zonal Manager",
                "State Head",
                "Cluster Head"
        ));

        DEPARTMENT_DESIGNATIONS.put("Finance", List.of(
                "Finance Executive",
                "Accountant"
        ));

        DEPARTMENT_DESIGNATIONS.put("Field Sales", List.of(
                "ASM"
        ));

        DEPARTMENT_DESIGNATIONS.put("Consulting", List.of(
                "Business Relationship Consultant"
        ));
    }

    public static Map<String, List<String>> getDepartmentDesignations() {
        return DEPARTMENT_DESIGNATIONS;
    }

    public static List<String> getDepartments() {
        return DEPARTMENT_DESIGNATIONS.keySet().stream().toList();
    }

    public static List<String> getDesignationsForDepartment(String department) {
        if (department == null) {
            return List.of();
        }

        return DEPARTMENT_DESIGNATIONS.getOrDefault(department, List.of());
    }

    private OfficeExecutiveDesignations() {
        // Utility class
    }

}
