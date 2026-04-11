package com.app.service;

import com.app.model.Product;
import com.app.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProductService {
    
    @Autowired
    private ProductRepository productRepository;
    
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
    
    public List<Product> getActiveProducts() {
        return productRepository.findByStatus("ACTIVE");
    }
    
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }
    
    public Product saveProduct(Product product) {
        // Generate product ID for new products or existing products without productId
        if (product.getProductId() == null || product.getProductId().isEmpty()) {
            String productId = generateProductId();
            product.setProductId(productId);
        }
        
        return productRepository.save(product);
    }
    
    private String generateProductId() {
        SecureRandom random = new SecureRandom();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder productId = new StringBuilder("PGES");

        // Generate 6 more alphanumeric characters (total 10: PGES + 6 chars)
        for (int i = 0; i < 6; i++) {
            productId.append(chars.charAt(random.nextInt(chars.length())));
        }

        // Ensure uniqueness
        while (productRepository.findByProductId(productId.toString()) != null) {
            productId = new StringBuilder("PGES");
            for (int i = 0; i < 6; i++) {
                productId.append(chars.charAt(random.nextInt(chars.length())));
            }
        }

        return productId.toString();
    }
    
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
    
    public List<Product> searchProducts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllProducts();
        }
        return productRepository.findByNameContainingIgnoreCase(keyword.trim());
    }
    
    public boolean nameExists(String name) {
        return productRepository.findByName(name) != null;
    }
    
    public boolean nameExistsForOtherProduct(String name, Long id) {
        Product product = productRepository.findByName(name);
        return product != null && !product.getId().equals(id);
    }
}
