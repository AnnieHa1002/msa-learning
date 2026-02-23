# Config Server (Spring Cloud Config)

## Config Server란?

MSA 환경에서는 서비스가 많아질수록 각 서비스의 설정 파일(포트, DB 접속 정보, 외부 API 키 등)을 개별적으로 관리하기 어려워진다. Spring Cloud Config Server는 모든 서비스의 설정을 한 곳에서 중앙 관리하고, 각 서비스는 실행 시 Config Server로부터 자신의 설정값을 받아오는 구조를 제공한다.

- 설정 변경 시 서비스 재배포 없이 Config Server의 파일만 수정하면 됨
- 환경(dev/stage/prod)별 설정을 프로필로 분리하여 관리
- Git 저장소 또는 로컬 파일 시스템에서 설정 파일을 읽어옴

## 동작 흐름

```
[Product Service] 기동 시
    │
    │ 1. Eureka에서 'config-server' 서비스 탐색 (bootstrap context)
    ▼
[Eureka Server] (:19090)
    │
    │ 2. Config Server 위치 반환
    ▼
[Config Server] (:18080)
    │
    │ 3. product-service-local.yml 반환 (app name + profile 매칭)
    ▼
[Product Service] 설정 적용 (포트: 19083)
```

1. Product Service 기동 시 bootstrap context가 Eureka에서 Config Server를 탐색
2. Config Server는 `{application-name}-{profile}.yml` 파일을 서빙
3. Product Service는 받아온 설정값으로 자신을 구성

## Config Server 설정

### build.gradle

```groovy
dependencies {
    implementation 'org.springframework.cloud:spring-cloud-config-server'
    implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
}
```

### application.yaml

```yaml
server:
  port: 18080

spring:
  profiles:
    active: native
  application:
    name: config-server
  cloud:
    config:
      server:
        native:
          search-locations: classpath:/config-repo  # 설정 파일 위치

eureka:
  client:
    service-url:
      defaultZone: http://localhost:19090/eureka/
```

- `native` 프로필: Git 대신 로컬 파일 시스템에서 설정 파일을 읽음
- `search-locations`: 설정 파일들이 위치한 경로

### ConfigApplication.java

```java
@EnableConfigServer   // Config Server 기능 활성화 (필수)
@SpringBootApplication
public class ConfigApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigApplication.class, args);
    }
}
```

`@EnableConfigServer`가 없으면 Config Server 기능이 활성화되지 않는다.

### 설정 파일 구조

```
resources/
└── config-repo/
    ├── product-service.yml        # 기본 프로필
    └── product-service-local.yml  # local 프로필
```

Config Server는 `{application-name}-{profile}.yml` 규칙으로 파일을 매칭한다.

```yaml
# product-service.yml
server:
  port: 19093

message: "product-service config"
```

```yaml
# product-service-local.yml
server:
  port: 19083

message: "product-service-local config"
```

## Config Client (Product Service) 설정

### build.gradle

```groovy
dependencies {
    implementation 'org.springframework.cloud:spring-cloud-starter-config'      // Config 클라이언트
    implementation 'org.springframework.cloud:spring-cloud-starter-bootstrap'   // Bootstrap context 활성화
    implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
}
```

### bootstrap.yaml (신규 생성)

```yaml
spring:
  application:
    name: product-service   # Config Server에서 파일 매칭에 사용
  cloud:
    config:
      discovery:
        enabled: true
        service-id: "config-server"  # Eureka에 등록된 Config Server 서비스 ID

eureka:
  client:
    service-url:
      defaultZone: http://localhost:19090/eureka/
```

### application.yaml

```yaml
spring:
  application:
    name: product-service
  profiles:
    active: local   # → product-service-local.yml 을 Config Server에서 수신

server:
  port: 0   # Config Server에서 받아온 포트로 덮어씌워짐
```

## 설정값 사용 — @RefreshScope

Config Server에서 받아온 값을 `@Value`로 주입받고, `@RefreshScope`를 붙이면 재시작 없이 설정을 갱신할 수 있다.

```java
@RefreshScope   // 설정 변경 시 이 빈을 새로 초기화
@RestController
@RequestMapping("/product")
public class ProductController {

    @Value("${server.port}")
    private String serverPort;

    @Value("${message}")
    private String message;

    @GetMapping
    public String getProduct() {
        return "Product detail from PORT : " + serverPort + " and message : " + message;
    }
}
```

`@RefreshScope`가 없으면 `/actuator/refresh`를 호출해도 이미 주입된 값이 바뀌지 않는다.

## 실시간 설정 갱신 — /actuator/refresh

Config Server의 설정 파일을 변경한 후 서비스를 재시작하지 않고 반영하는 방법이다.

### 갱신 절차

```
1. Config Server의 설정 파일 수정 (e.g. message 값 변경)
2. Config Server 재시작 (native 모드는 재시작 필요)
3. Product Service에 POST /actuator/refresh 호출
4. 변경된 값 반영 확인
```

```yaml
# actuator refresh 엔드포인트 노출 설정 (application.yaml)
management:
  endpoints:
    web:
      exposure:
        include: refresh
```

> native 모드(로컬 파일)는 Config Server를 재시작해야 변경이 반영된다. Git 모드를 사용하면 push만으로 즉시 반영된다.

### 설정 갱신 방식 비교

| 방식 | 설명 | 특징 |
|------|------|------|
| `/actuator/refresh` 수동 호출 | 각 서비스에 직접 POST 요청 | 간단하지만 서비스가 많으면 번거로움 |
| Spring Cloud Bus | RabbitMQ/Kafka로 변경 사항 전파 | 여러 서비스에 한 번에 반영 가능 |
| Spring Boot DevTools | 파일 변경 자동 감지 후 재시작 | 개발 환경 전용 |

## 설정 저장소 — Native vs Git

| | Native (로컬 파일) | Git |
|---|---|---|
| 설정 파일 위치 | classpath 내부 | Git 원격 저장소 |
| 변경 반영 | Config Server 재시작 필요 | push 후 즉시 반영 |
| 버전 관리 | 없음 | 커밋 히스토리로 관리 |
| 실무 사용 | 학습/테스트용 | 일반적인 실무 방식 |

```yaml
# Git 모드 설정 예시
spring:
  cloud:
    config:
      server:
        git:
          uri: https://github.com/my-org/config-repo
          clone-on-start: true
```

> 실무에서는 직접 Config Server를 구현하기보다 이미 구축된 Config Server에 연동하는 경우가 많다고 한다. 결국 클라이언트 설정만 할 줄 알면 되는 경우가 대부분이라는 것.

## Config Server REST API

Config Server가 떠 있으면 아래 URL로 설정값을 직접 확인할 수 있다.

```
GET http://localhost:18080/{application-name}/{profile}
```

예시: `http://localhost:18080/product-service/local`

```json
{
  "name": "product-service",
  "profiles": ["local"],
  "propertySources": [
    {
      "name": "classpath:/config-repo/product-service-local.yml",
      "source": {
        "server.port": 19083,
        "message": "product-service-local config"
      }
    }
  ]
}
```

이 엔드포인트로 어떤 설정이 실제로 서빙되고 있는지 확인할 수 있어서 디버깅할 때 유용하다.

---

## 기동 순서

Config Server는 Eureka에 등록되고, Product Service는 Eureka를 통해 Config Server를 찾기 때문에 순서가 중요하다.

```
1. Eureka Server 기동
2. Config Server 기동  (Eureka에 'config-server'로 등록)
3. Product Service 기동 (bootstrap → Eureka → Config Server → 설정 수신)
```

## Spring Boot 4.0에서의 변경사항

Spring Boot 3.x에서는 `spring.config.import: "configserver:"` + `spring.cloud.config.discovery.enabled: true`만으로도 Eureka discovery 기반 config 로딩이 동작했다.

Spring Boot 4.0부터는 **bootstrap context가 기본 비활성화**되어, discovery 기반 config를 사용하려면 다음이 필요하다:

| 항목 | Spring Boot 3.x | Spring Boot 4.0 |
|------|----------------|----------------|
| bootstrap context | 기본 활성화 | 기본 비활성화 |
| discovery-first config | `application.yaml`에서 설정 가능 | `spring-cloud-starter-bootstrap` + `bootstrap.yaml` 필요 |
| 대안 | - | 직접 URL 지정 (`configserver:http://localhost:18080`) |

## 배운 점

- **`configserver:`는 고정 URI 스키마다** — `spring.config.import`에서 `configserver:`는 Spring Cloud Config가 인식하는 고정 키워드다. Eureka 서비스 ID(`config-server`)와 다른 개념이므로 혼동하지 않아야 한다.

- **Config Server와 Config Client는 다른 의존성이다** — 클라이언트 서비스에는 `spring-cloud-starter-config`를, Config Server 자체에는 `spring-cloud-config-server`를 사용한다. 클라이언트에 서버용 의존성을 넣으면 `ConfigServerConfigDataLocationResolver`가 정상 등록되지 않아 설정을 읽어오지 못한다.

- **bootstrap context는 일반 application context보다 먼저 실행된다** — discovery 기반 Config Server 탐색은 bootstrap context에서 이루어진다. 설정이 `application.yaml`에 있으면 이미 application context 단계라 너무 늦다. discovery 관련 설정은 반드시 `bootstrap.yaml`에 작성해야 한다.

- **YAML 들여쓰기 오류는 조용히 실패한다** — `spring.cloud.config.server`가 `spring.application` 하위에 중첩되면 에러 없이 그냥 무시되어 Config Server가 동작하지 않는다. YAML 구조는 항상 꼼꼼히 확인해야 한다.

- **`@RefreshScope`는 재시작 없이 설정을 갱신하는 핵심이다** — 이 어노테이션이 없으면 `/actuator/refresh`를 호출해도 이미 주입된 `@Value` 값이 바뀌지 않는다. 처음엔 왜 refresh를 해도 변경이 안 되는지 이해가 안 됐는데, 빈이 초기화 시점에 값을 주입받고 그걸 그냥 들고 있는 거라 납득이 됐다.

- **Config Server의 존재 이유를 이제는 이해한다** — 서비스가 10개, 20개로 늘어나면 각 서비스마다 설정 파일을 따로 관리하는 게 얼마나 힘들지 체감이 된다. 환경별(dev/prod)로 같은 코드인데 설정만 다르게 배포해야 할 때 Config Server가 없으면 배포할 때마다 설정 파일을 직접 바꿔줘야 하는데, 그게 얼마나 실수하기 쉬운 작업인지 이전 업무 경험상 잘 안다.

- **native 모드는 학습용이다** — 설정을 바꿀 때마다 Config Server를 재시작해야 한다면 사실 큰 의미가 없다. 실무에서는 Git 저장소를 사용해서 push만으로 반영되게 하거나, Spring Cloud Bus로 변경 사항을 전파하는 구조를 쓴다고 한다. 이번 실습은 native로 구조 자체를 이해하는 게 목적이었다.