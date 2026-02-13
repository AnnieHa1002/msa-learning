# MSA Learning

MSA(Microservice Architecture) 학습을 위한 프로젝트

## 프로젝트 구조

| 모듈 | 역할 | 포트 |
|------|------|------|
| `com.sparta.msa_learning.server` | Eureka Server (서비스 레지스트리) | 19090 |
| `com.sparta.msa_learning.gateway` | API Gateway | 19091 |
| `com.sparta.msa_learning.order` | Order Service (주문) | 19092 |
| `com.sparta.msa_learning.product` | Product Service (상품) | 19093 |
| `com.sparta.msa_learning.product2` | Product Service (상품) | 19094 |
| `com.sparta.msa_learning.product3` | Product Service (상품) | 19095 |
| `com.sparta.msa_learning.auth` | Auth Service (인증/JWT) | 19096 |

## 기술 스택

- Java 21
- Spring Boot 4.0.2
- Spring Cloud 2025.1.0 (Oakwood)
- Spring Cloud Netflix Eureka
- Spring Cloud OpenFeign
- Resilience4j (Circuit Breaker)
- Spring Cloud Gateway
- JJWT 0.13.0 (JWT 인증)

## 실습 진행 과정

### 1단계: Eureka Server 실습

Eureka Server를 띄우고, first와 second 서비스를 등록하여 서비스 디스커버리가 동작하는지 확인했다.

| 커밋 | 내용 |
|------|------|
| `419f436` | Eureka Server 및 first, second 서비스 프로젝트 세팅 |

### 2단계: OpenFeign 로드 밸런서 실습

서비스 이름을 order, product로 변경하고, OpenFeign을 활용한 서비스 간 통신과 로드 밸런싱을 실습했다.

| 커밋 | 내용 |
|------|------|
| `d6128ec` | first, second를 order, product로 변경 |
| `5700bda` | ProductController 구현 |
| `c124252` | OrderController 구현 |
| `f84e727` | ProductClient (FeignClient 인터페이스) 구현 |
| `91b2784` | OrderService에서 ProductClient를 통한 서비스 간 호출 구현 |
| `6ff31b9` | OpenFeign 관련 문서 정리 |

### 3단계: Circuit Breaker (Resilience4j) 실습

Resilience4j를 활용하여 장애 상황에서의 fallback 처리를 실습했다.

| 커밋 | 내용 |
|------|------|
| `3b13d4a` | Product 엔티티 구성 |
| `c3d283e` | ProductService에 CircuitBreaker 및 fallback 메서드 구현 |

### 4단계: API Gateway 실습

Spring Cloud Gateway를 활용하여 클라이언트 요청을 각 서비스로 라우팅하고, GlobalFilter를 통한 요청 로깅을 실습했다.

| 커밋 | 내용 |
|------|------|
| `a12b2f5` | API Gateway 프로젝트 추가 |
| `112c6a9` | Gateway 설정 수정 (Spring Cloud Gateway 5.0 property 경로 대응) |

### 5단계: JWT 인증 실습

Auth Service를 추가하고, Gateway에서 JWT 토큰을 검증하는 인증 흐름을 구현했다.

| 커밋 | 내용 |
|------|------|
| - | Auth Service 프로젝트 추가 (JWT 토큰 발급) |
| - | Gateway에 `LocalJwtAuthenticationFilter` 구현 (JWT 토큰 검증) |
| - | Gateway에서 `/api/auth/**` 경로 인증 bypass 처리 |
| - | Gateway에서 `spring-boot-starter-security` 제거 (JWT GlobalFilter로 대체) |
| - | Auth Service에 `jjwt-impl`, `jjwt-jackson` 런타임 의존성 추가 |
| - | Gateway JWT 키 디코딩 방식을 Auth Service와 동일하게 통일 (`Decoders.BASE64URL`) |

> 트러블슈팅은 [TROUBLESHOOTING.md](TROUBLESHOOTING.md) 참고

## 학습 문서

| 문서 | 내용 |
|------|------|
| [MSA](#msa-microservice-architecture) | MSA 개념, Monolithic과 비교, 과거 경험 회고 |
| [Eureka](docs/01-eureka.md) | 서비스 디스커버리, 동작 방식, 실습 결과 |
| [OpenFeign과 로드밸런싱](docs/02-openfeign.md) | 선언적 HTTP 클라이언트, Round Robin 로드밸런싱 검증 |
| [Circuit Breaker](docs/03-circuitbreaker.md) | Resilience4j를 활용한 장애 격리와 fallback |
| [API Gateway](docs/04-gateway.md) | Spring Cloud Gateway를 활용한 라우팅과 필터 |
| [JWT 인증](docs/05-jwt-auth.md) | JWT 토큰 발급/검증, Gateway 인증 필터 |

---

## MSA (Microservice Architecture)

하나의 애플리케이션을 여러 개의 독립적인 서비스로 분리하여 개발, 배포, 유지보수를 용이하게 하는 소프트웨어 아키텍처 스타일이다.

- 각 서비스는 특정 비즈니스 기능을 수행하며, 서로 독립적으로 배포되고 확장될 수 있음
- 서비스 간의 통신은 주로 HTTP/HTTPS, 메시지 큐 등을 통해 이루어짐

### Monolithic vs MSA

| | Monolithic | MSA |
|---|---|---|
| 배포 | 전체 애플리케이션을 한 번에 배포 | 서비스 단위로 독립 배포 |
| 장애 영향 | 하나의 장애가 전체 서비스에 영향 | 장애가 해당 서비스에 한정 |
| 확장 | 전체를 통째로 스케일링 | 필요한 서비스만 스케일링 |
| 개발 복잡도 | 낮음 (단일 코드베이스) | 높음 (서비스 간 통신, 분산 환경 관리) |

### 과거 경험에서 돌아보는 MSA

과거에 회사에서 서비스를 개발하면 CTO님이 내게 MSA 개발에 대해 말한 적이 있었다.
현재 자바로 개발하고 있는 부분들을 서비스 단위로 별개로 배포하여서 배포 및 관리할 때 훨씬 안전하게 할 수 있다는 말이었다.

문제는 모놀리틱 아키텍처로만 처리해보았기 때문에 이해가 안 되는 부분이 생긴다.
어떻게 배포를 별개로 할 수 있는 걸까? 결국 모든 게 연관되어 있는데.

실제 비슷한 방식을 어드민 페이지에 도입하곤 했다.
당연히 어드민 페이지는 웹서비스와 별도의 내부 웹사이트기 때문에 따로 서버를 돌리는 것이 안전하다는 판단이 있었다.
하지만 많은 어드민 기능이 실 서비스와 연관된 부분이 많아 기존 서비스 기능들이 개발되어 있는 운영 중인 백엔드에서 개발하는 것이 개발 기간적으로도 더 빠르고 편리했다.
그럼에도 불구하고 위험성을 고려해 Go로 따로 구성하였는데 이것도 MSA적 사고방식이었을까.

생각해보면 예전에 Lambda로 모든 API를 개발하였었는데 이것도 일종의 MSA 구조였을지 궁금했는데 알아보니 좀 다른것 같다.
Lambda는 각 함수가 독립적으로 배포되고 실행되므로, 서비스를 잘게 쪼갠다는 관점에서 MSA보다 더 극단적인 형태인 **FaaS(Function as a Service)** 에 해당한다.

### MSA를 가능하게 해주는 주요 도구들

- **Eureka** : MSA 서비스들의 위치를 동적으로 관리 (서비스 레지스트리, 헬스 체크)
- **API Gateway** : 클라이언트 요청을 적절한 서비스로 라우팅
- **Config Server** : 서비스들의 설정을 중앙에서 관리
- **Circuit Breaker** : 장애 서비스 호출 시 빠르게 실패하여 전체 시스템 보호
- **JWT 인증** : Gateway에서 토큰 기반 인증을 통해 서비스 접근 제어

### Learning Points

**MSA는 이미 경험하고 있었다** — 어드민 페이지를 별도의 Go 서버로 분리한 것, Lambda로 각 API를 독립적으로 배포한 것 모두 MSA적 사고방식이었다. MSA는 새로운 개념이 아니라 "서비스를 독립적으로 분리한다"는 원칙의 체계화된 형태인 것 같다.

**"안전한 배포"의 의미** — 모놀리틱에서는 하나의 기능을 수정해도 전체를 다시 배포해야 하고, 그 과정에서 관련 없는 기능에 영향을 줄 수 있다. MSA에서는 변경된 서비스만 독립적으로 배포할 수 있어, 전에 들었던 "안전한 배포"는 바로 이 맥락이었다.

**트레이드오프가 존재한다** — MSA는 배포와 장애 격리에서는 이점이 있지만, 개발 복잡도가 확실히 올라간다. 서비스마다 별도의 프로젝트를 생성하고, 서비스 간 통신을 관리하고, Eureka 같은 인프라 도구를 추가로 운영해야 한다. 어드민 페이지를 Go로 분리했을 때 개발 기간이 늘어난 경험이 이를 잘 보여준다.

---

## Timeline

- 2026-02-10 : MSA와 Eureka에 대한 기본 개념 학습 시작
- 2026-02-11 : Eureka Server 및 Client 프로젝트 생성 및 실행
- 2026-02-11 : OpenFeign 적용, Product 3개 인스턴스 로드밸런싱 확인
- 2026-02-12 : Resilience4j Circuit Breaker 적용, fallback 동작 확인
- 2026-02-12 : API Gateway 추가, 라우팅 및 GlobalFilter 적용
- 2026-02-13 : Auth Service 추가, JWT 토큰 발급/검증 구현
- 2026-02-13 : Gateway JWT 인증 필터 적용, Spring Security 이슈 해결