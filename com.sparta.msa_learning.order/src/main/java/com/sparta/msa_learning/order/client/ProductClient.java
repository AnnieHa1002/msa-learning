package com.sparta.msa_learning.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service")
public interface ProductClient {

    @GetMapping("/api/products")
    String getProducts();

    @GetMapping("/api/products/{id}")
    String getProductById(@PathVariable Long id);
}
