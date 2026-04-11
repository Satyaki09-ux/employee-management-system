package com.app.service;

import com.app.model.Admin;
import com.app.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AdminService {
    
    @Autowired
    private AdminRepository adminRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public List<Admin> getAllAdmins() {
        return adminRepository.findAll();
    }
    
    public List<Admin> getActiveAdmins() {
        return adminRepository.findByStatus("ACTIVE");
    }
    
    public List<Admin> getAdminsByRole(String role) {
        return adminRepository.findByRole(role);
    }
    
    public List<Admin> getSubAdmins(Long parentAdminId) {
        return adminRepository.findByParentAdmin_Id(parentAdminId);
    }
    
    public Optional<Admin> getAdminById(Long id) {
        return adminRepository.findById(id);
    }
    
    public Admin saveAdmin(Admin admin) {
        // Encode password if it's being changed and not already encoded
        if (admin.getPassword() != null && !admin.getPassword().isEmpty()) {
            // Check if password is already encoded (starts with $2a$ or $2b$)
            if (!admin.getPassword().startsWith("$2a$") && !admin.getPassword().startsWith("$2b$")) {
                admin.setPassword(passwordEncoder.encode(admin.getPassword()));
            }
        } else if (admin.getId() != null) {
            // If password is empty during edit, don't change it - load existing password
            Optional<Admin> existingAdmin = adminRepository.findById(admin.getId());
            if (existingAdmin.isPresent()) {
                admin.setPassword(existingAdmin.get().getPassword());
            }
        }
        
        // Set default role if not specified
        if (admin.getRole() == null || admin.getRole().isEmpty()) {
            admin.setRole("SUB_ADMIN");
        }
        
        return adminRepository.save(admin);
    }
    
    public void deleteAdmin(Long id) {
        adminRepository.deleteById(id);
    }
    
    public boolean emailExists(String email) {
        return adminRepository.findByEmail(email) != null;
    }
    
    public boolean emailExistsForOtherAdmin(String email, Long id) {
        Admin admin = adminRepository.findByEmail(email);
        return admin != null && !admin.getId().equals(id);
    }
    
    public boolean usernameExists(String username) {
        return adminRepository.findByUsername(username) != null;
    }
    
    public boolean usernameExistsForOtherAdmin(String username, Long id) {
        Admin admin = adminRepository.findByUsername(username);
        return admin != null && !admin.getId().equals(id);
    }
    
    public Optional<Admin> getAdminByUsername(String username) {
        Admin admin = adminRepository.findByUsername(username);
        return Optional.ofNullable(admin);
    }
    
    /**
     * Check if admin has a specific permission
     */
    public boolean hasPermission(Admin admin, String permission) {
        if (admin == null) {
            return false;
        }
        
        // SUPER_ADMIN has all permissions
        if ("SUPER_ADMIN".equals(admin.getRole())) {
            return true;
        }
        
        // Check permissions string
        String permissions = admin.getPermissions();
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }
        
        String[] perms = permissions.split(",");
        for (String perm : perms) {
            if (perm.trim().equals(permission)) {
                return true;
            }
        }
        return false;
    }
}
