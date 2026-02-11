package com.sparta.msa_learning.product.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Value("${server.port}")
    String serverPort;

    @GetMapping("")
    public String getProducts() {
        return "상품 목록을 반환합니다.";
    }

    @GetMapping("/{id}")
    public String getProductById(@PathVariable Long id) {
        return "상품 ID " + id + "에 대한 정보를 반환합니다. PORT : " + serverPort;
    }
}
