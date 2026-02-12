package com.sparta.msa_learning.order.service;

import com.sparta.msa_learning.order.client.ProductClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final ProductClient productClient;

    public String getProductInfo(Long productId) {
        String product = productClient.getProductById(productId);
        return product + " 주문이 생성되었습니다.";
    }

    public String getOrder(Long orderId) {
        if (orderId == 1L) {
            String productInfo = getProductInfo(2L);
            return "주문 ID: " + orderId + ", " + productInfo;
        }
        else {
            return "주문 정보를 찾을 수 없습니다.";
        }
    }
}
