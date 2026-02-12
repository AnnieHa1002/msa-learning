# API Gateway (Spring Cloud Gateway)

## API Gateway란?

MSA 환경에서 클라이언트의 모든 요청을 단일 진입점에서 받아 적절한 서비스로 라우팅하는 역할을 한다. 클라이언트는 각 서비스의 주소를 알 필요 없이 Gateway 하나만 바라보면 된다.

## 구현 구조

```
[Client]
    │
    ▼
[Gateway] (:19091)
    │
    ├── /api/orders/**   → lb://order-service   (:19092)
    └── /api/products/** → lb://product-service  (:19093, :19094, :19095)
```

Gateway가 Eureka에 등록된 서비스 이름을 기반으로 `lb://` (로드밸런싱) URI를 통해 요청을 전달한다.

## 라우팅 설정

Spring Cloud Gateway 5.0 (2025.x)에서는 설정 prefix가 `spring.cloud.gateway.server.webflux`로 변경되었다.

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: order-service
              uri: lb://order-service
              predicates:
                - Path=/api/orders,/api/orders/**
            - id: product-service
              uri: lb://product-service
              predicates:
                - Path=/api/products,/api/products/**
```

## GlobalFilter를 통한 요청 로깅

모든 요청에 대해 URI를 로깅하는 `CustomPreFilter`를 구현했다.

```java
@Slf4j
@Component
public class CustomPreFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        log.info("Custom Pre Filter: request id -> {}", request.getURI());
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
```

`GlobalFilter`를 구현하면 모든 라우트에 공통으로 적용되며, `Ordered`를 통해 필터 실행 순서를 제어할 수 있다.

## 배운 점

- Spring Cloud Gateway 5.0에서 설정 property 경로가 `spring.cloud.gateway.*`에서 `spring.cloud.gateway.server.webflux.*`로 변경되었다. 버전에 따른 설정 차이를 주의해야 한다.
- Gateway를 통해 클라이언트는 서비스 주소를 몰라도 되고, Gateway 하나의 포트만 알면 모든 서비스에 접근할 수 있다. MSA 도구 목록에서 봤던 "클라이언트 요청을 적절한 서비스로 라우팅"하는 역할을 직접 확인했다.
- GlobalFilter로 인증, 로깅, 헤더 조작 등 공통 관심사를 한 곳에서 처리할 수 있다는 점이 실용적이다.
