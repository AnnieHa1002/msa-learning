# JWT 인증 (Gateway + Auth Service)

## JWT 인증이란?

MSA 환경에서 각 서비스에 대한 접근을 제어하기 위해 JWT(JSON Web Token) 기반 인증을 사용한다. 클라이언트가 Auth Service에서 토큰을 발급받고, 이후 요청 시 토큰을 함께 전송하면 Gateway에서 토큰을 검증하여 인가된 요청만 서비스로 전달한다.

## 인증 흐름

```
[Client]
    │
    │ 1. POST /api/auth/sign-in?username=user1
    ▼
[Gateway] (:19091) ── /api/auth는 인증 bypass ──> [Auth Service] (:19096)
                                                        │
                                                   2. JWT 토큰 발급
                                                        │
    <───────────── 3. { accessToken: "eyJ..." } ────────┘
    │
    │ 4. GET /api/orders (Authorization: Bearer eyJ...)
    ▼
[Gateway] ── JWT 검증 (LocalJwtAuthenticationFilter) ──> [Order Service] (:19092)
```

1. 클라이언트가 `/api/auth/sign-in`으로 로그인 요청
2. Auth Service가 JWT 토큰을 생성하여 반환
3. 클라이언트가 토큰을 저장
4. 이후 요청 시 `Authorization: Bearer <token>` 헤더와 함께 전송
5. Gateway의 `LocalJwtAuthenticationFilter`가 토큰 검증 후 서비스로 전달

## Auth Service — 토큰 발급

```java
@Service
public class AuthService {

    private final SecretKey secretKey;

    public AuthService(@Value("${service.jwt.secret-key}") String secretKey) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(secretKey));
    }

    public String createAccessToken(String userId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .issuer(issuer)
                .claim("user_id", userId)
                .claim("role", "ADMIN")
                .issuedAt(new Date(now))
                .expiration(new Date(now + accessExpiration))
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }
}
```

- `Decoders.BASE64URL.decode()`로 secret-key 문자열을 디코딩하여 `SecretKey` 생성
- HS512 알고리즘으로 서명
- `user_id`, `role` 등 claims 포함

## Gateway — 토큰 검증 (LocalJwtAuthenticationFilter)

```java
@Component
public class LocalJwtAuthenticationFilter implements GlobalFilter {

    @Value("${service.jwt.secret-key}")
    private String jwtSecret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path.startsWith("/api/auth")) {
            return chain.filter(exchange);  // 인증 API는 bypass
        }

        String token = extractToken(exchange);
        if (token == null || !validateToken(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }

    private boolean validateToken(String token) {
        try {
            SecretKey secretKey = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(jwtSecret));
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
```

- `GlobalFilter`를 구현하여 모든 요청에 대해 JWT 검증
- `/api/auth` 경로는 토큰 없이 접근 가능 (로그인/회원가입)
- `Authorization: Bearer <token>` 헤더에서 토큰 추출
- Auth Service와 **동일한 방식**(`Decoders.BASE64URL`)으로 secret-key를 디코딩하여 검증

## JJWT 의존성 구성

JJWT 0.11+ 버전부터 라이브러리가 3개로 분리되었다. 세 가지 모두 필요하다:

```groovy
dependencies {
    implementation 'io.jsonwebtoken:jjwt-api:0.13.0'       // 컴파일 타임 API
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.13.0'         // 런타임 구현체
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.13.0'      // JSON 직렬화 (Jackson)
}
```

`jjwt-impl`과 `jjwt-jackson`은 `runtimeOnly`로 선언한다. 컴파일 시에는 `jjwt-api`의 인터페이스만 사용하고, 런타임에 구현체가 자동으로 로드된다.

## Gateway에서 Spring Security를 사용하지 않는 이유

Spring Cloud Gateway는 **WebFlux(reactive)** 기반이다. `spring-boot-starter-security`를 추가하면:

1. 커스텀 설정 없이는 기본적으로 모든 요청에 인증을 요구 (HTTP Basic 로그인 팝업)
2. `spring-boot-starter-webmvc`(servlet)와 함께 사용 시 servlet/reactive Security 빈 충돌 발생
3. Security 설정을 하더라도 `@EnableWebSecurity`가 아닌 `@EnableWebFluxSecurity`를 사용해야 함

이 프로젝트에서는 `LocalJwtAuthenticationFilter`(GlobalFilter)가 JWT 인증을 직접 처리하므로 Spring Security 의존성이 불필요하다.

## 배운 점

- **JWT는 MSA 인증의 핵심이다** — 각 서비스가 독립적으로 배포되는 MSA 환경에서, 세션 기반 인증은 상태를 공유해야 하므로 적합하지 않다. JWT는 토큰 자체에 인증 정보가 포함되어 있어 서비스 간 상태 공유가 필요 없다.

- **Gateway가 인증 관문 역할을 한다** — 개별 서비스가 각각 인증을 처리하는 대신, Gateway에서 한 번만 검증하면 된다. 이는 "단일 진입점"이라는 API Gateway의 역할과 자연스럽게 맞아떨어진다.

- **서명/검증 키 일치는 필수다** — 같은 secret-key 문자열을 사용하더라도 `Base64URL.decode()`와 `getBytes()`는 완전히 다른 바이트 배열을 만든다. 토큰 발급과 검증에서 동일한 디코딩 방식을 사용해야 한다.

- **Gateway는 WebFlux 세계다** — Spring Cloud Gateway에서는 servlet 관련 의존성(`webmvc`, `@EnableWebSecurity`)을 절대 사용하면 안 된다. reactive 환경에서는 `ServerHttpSecurity`, `@EnableWebFluxSecurity`, `SecurityWebFilterChain`을 사용해야 한다.
