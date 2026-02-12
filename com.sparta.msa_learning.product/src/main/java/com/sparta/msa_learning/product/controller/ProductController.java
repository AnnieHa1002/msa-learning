package com.sparta.msa_learning.product.controller;

import com.sparta.msa_learning.product.domain.Product;
import com.sparta.msa_learning.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @Value("${server.port}")
    String serverPort;


    @GetMapping("")
    public String getProducts() {
        return "상품 목록을 반환합니다.";
    }

    @GetMapping("/{id}")
    public String getProductById(@PathVariable Long id) {
        Product product = productService.getProductDetails(id);
        String productInfo = "상품 ID: " + product.getId() + ", 이름: " + product.getName() + ", 가격: " +
                product.getPrice();
        productInfo += " (서버 포트: " + serverPort + ")";
        return productInfo;
    }
}
