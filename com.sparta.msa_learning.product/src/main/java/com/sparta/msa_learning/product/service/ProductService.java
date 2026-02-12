package com.sparta.msa_learning.product.service;

import com.sparta.msa_learning.product.domain.Product;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @PostConstruct
    public void registerEventListener() {
        circuitBreakerRegistry.circuitBreaker("productService").getEventPublisher()
                .onStateTransition(event -> log.info("#######CircuitBreaker State Transition: {}", event)) // 상태 전환 이벤트 리스너
                .onFailureRateExceeded(event -> log.info("#######CircuitBreaker Failure Rate Exceeded: {}", event)) // 실패율 초과 이벤트 리스너
                .onCallNotPermitted(event -> log.info("#######CircuitBreaker Call Not Permitted: {}", event)) // 호출 차단 이벤트 리스너
                .onError(event -> log.info("#######CircuitBreaker Error: {}", event)); // 오류 발생 이벤트 리스너
    }

    @CircuitBreaker(name="productService", fallbackMethod = "fallbackGetProductDetails")
    public Product getProductDetails(Long productId) {
        log.info("Get product by id: {}", productId);
        // In a real application, this would fetch data from a database
        if(productId == 111L) {
            log.warn("Get product by id {} failed", productId);
          throw  new IllegalArgumentException("Empty Response Body");
        } else {
            return new Product(productId, "Sample Product", 100);
        }
    }

    public Product fallbackGetProductDetails(Long productId, Throwable t) {

        log.error("Get product by id {} failed , detail : {}", productId, t.getMessage());
        return new Product(productId, "Fallback Product", 0);
    }
}
