package com.sparta.msa_learning.product.service;

import com.sparta.msa_learning.product.domain.Product;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;

@Service
public class ProductService {


    @CircuitBreaker(name="productService", fallbackMethod = "fallbackGetProductDetails")
    public Product getProductDetails(Long productId) {
        // In a real application, this would fetch data from a database
        if(productId == 111L) {
          throw  new IllegalArgumentException("Empty Response Body");
        } else {
            return new Product(productId, "Sample Product", 100);
        }
    }

    public Product fallbackGetProductDetails(Long productId, Throwable t) {
        return new Product(productId, "Fallback Product", 0);
    }
}
