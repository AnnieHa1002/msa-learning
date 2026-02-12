package com.sparta.msa_learning.order.controller;

import com.sparta.msa_learning.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("")
    public String getOrder() {
        return "Order";
    }




    @GetMapping("/{orderId}")
    public String getOrderById(@PathVariable Long orderId) {
        return orderService.getOrder(orderId);
    }
}
