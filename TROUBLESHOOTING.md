# Troubleshooting

프로젝트 구성 과정에서 발생한 에러와 해결 과정을 정리한 문서

---

### 1. Could not find spring-cloud-starter-netflix-eureka-client

**에러 메시지**
```
Could not resolve all files for configuration ':compileClasspath'.
> Could not find org.springframework.cloud:spring-cloud-starter-netflix-eureka-client:.
```

**원인**

`spring-cloud-starter-netflix-eureka-client`의 버전이 비어있음. `ext`에 `springCloudVersion`을 선언했지만 `dependencyManagement` 블록에서 Spring Cloud BOM을 import하지 않아 버전이 해석되지 않음.

**해결**

`build.gradle`에 `dependencyManagement` 블록 추가:
```groovy
dependencyManagement {
    imports {
        mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
    }
}
```

---

### 2. ClassNotFoundException: WebServerInitializedEvent (Eureka Client)

**에러 메시지**
```
Caused by: java.lang.ClassNotFoundException:
    org.springframework.boot.web.server.context.WebServerInitializedEvent
```

**원인**

Eureka Client가 서비스 등록 시 웹 서버 정보(포트 등)가 필요한데, `spring-boot-starter-web` 의존성이 없어 웹 서버 관련 클래스를 찾지 못함.

**해결**

`com.sparta.msa_learning.first`의 `build.gradle`에 웹 스타터 추가:
```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
}
```

---

### 3. Gateway 라우트 404 Not Found

**에러 메시지**
```
Whitelabel Error Page
There was an unexpected error (type=Not Found, status=404).
```

**원인**

Spring Cloud Gateway 5.0 (Spring Cloud 2025.x)에서 설정 property prefix가 변경되었다.
기존 `spring.cloud.gateway.routes`가 더 이상 인식되지 않아 라우트가 등록되지 않음.

**해결**

`spring.cloud.gateway.routes`를 `spring.cloud.gateway.server.webflux.routes`로 변경:
```yaml
# 변경 전 (동작 안 함)
spring:
  cloud:
    gateway:
      routes:
        - id: order-service
          ...

# 변경 후 (동작)
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: order-service
              ...
```

---

### 4. JJWT ExceptionInInitializerError — jjwt-impl 누락

**에러 메시지**
```
Caused by: io.jsonwebtoken.lang.UnknownClassException:
    Unable to load class named [io.jsonwebtoken.impl.security.KeysBridge]
    ...Have you remembered to include the jjwt-impl.jar in your runtime classpath?
```

**원인**

JJWT 0.11+ 버전부터 라이브러리가 3개의 jar로 분리되었다. `jjwt-api`만 추가하고 런타임 구현체(`jjwt-impl`, `jjwt-jackson`)를 추가하지 않아 클래스를 찾지 못함.

**해결**

`build.gradle`에 런타임 의존성 추가:
```groovy
dependencies {
    implementation 'io.jsonwebtoken:jjwt-api:0.13.0'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.13.0'      // 런타임 구현체
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.13.0'    // JSON 직렬화
}
```

---

### 5. Gateway에서 모든 요청에 로그인 팝업 표시

**증상**

`http://localhost:19091/api/auth`로 접근 시 브라우저에서 HTTP Basic 인증 팝업이 표시됨. 직접 Auth Service(`localhost:19096`)로 접근하면 정상 동작.

**원인**

두 가지 문제가 복합적으로 발생:

1. **Spring Security 기본 설정** — Gateway에 `spring-boot-starter-security`가 있지만 커스텀 Security 설정 클래스가 없어서 Spring Security가 기본 설정(모든 요청 인증 필요, HTTP Basic 활성화)으로 동작함.

2. **Servlet/Reactive 충돌** — Gateway는 WebFlux(reactive) 기반인데, `spring-boot-starter-webmvc`(servlet)가 함께 있어 servlet 기반 `WebSecurityConfiguration`과 reactive 기반 `ReactiveManagementWebSecurityAutoConfiguration`이 동시에 빈 등록을 시도하여 충돌 발생.

**에러 메시지 (빈 충돌 시)**
```
The bean 'springSecurityFilterChain' could not be registered.
A bean with that name has already been defined in class path resource
[org/springframework/security/config/annotation/web/configuration/WebSecurityConfiguration.class]
and overriding is disabled.
```

**해결**

Gateway에서는 `LocalJwtAuthenticationFilter`(GlobalFilter)가 JWT 인증을 처리하므로 Spring Security가 불필요. 다음을 제거:
- `spring-boot-starter-security` 의존성 제거
- `spring-boot-starter-webmvc` 의존성 제거 (Gateway는 WebFlux 기반)

```groovy
dependencies {
    // security, webmvc 제거 — JWT GlobalFilter가 인증 처리
    implementation 'org.springframework.cloud:spring-cloud-starter-gateway-server-webflux'
    // ...
}
```

**핵심 교훈**: Spring Cloud Gateway는 WebFlux 기반이므로 servlet 관련 의존성(`webmvc`, `@EnableWebSecurity`)을 사용하면 안 된다. Security가 필요하면 `@EnableWebFluxSecurity`와 `ServerHttpSecurity`를 사용해야 한다.

---

### 6. Gateway JWT 토큰 검증 실패 — Secret Key 디코딩 방식 불일치

**증상**

Auth Service에서 발급한 JWT 토큰이 Gateway에서 항상 검증 실패. 같은 secret-key 문자열을 사용하는데도 `JwtException` 발생.

**원인**

Auth Service와 Gateway에서 secret-key를 `SecretKey` 객체로 변환하는 방식이 달랐다:

```java
// Auth Service (토큰 생성) — Base64URL 디코딩
Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(secretKey));

// Gateway (토큰 검증) — 문자열을 그대로 바이트 변환
Keys.hmacShaKeyFor(jwtSecret.getBytes());
```

같은 문자열이지만 `Base64URL.decode()`와 `getBytes()`는 완전히 다른 바이트 배열을 생성하므로 서로 다른 키가 된다.

**해결**

Gateway의 키 생성 방식을 Auth Service와 동일하게 변경:
```java
// Gateway — Auth Service와 동일한 방식으로 통일
SecretKey secretKey = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(jwtSecret));
```

**핵심 교훈**: JWT 토큰의 서명(sign)과 검증(verify)에서 동일한 키를 사용해야 하며, 문자열에서 `SecretKey`로 변환하는 방식도 반드시 일치해야 한다.
