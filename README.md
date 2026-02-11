# MSA Learning

MSA(Microservice Architecture) 학습을 위한 프로젝트

## 프로젝트 구조

| 모듈 | 역할 | 포트 |
|------|------|------|
| `com.sparta.msa_learning.server` | Eureka Server (서비스 레지스트리) | 19090 |
| `com.sparta.msa_learning.order` | Order Service (주문) | 19091 |
| `com.sparta.msa_learning.product` | Product Service (상품) | 19092 |
| `com.sparta.msa_learning.product2` | Product Service (상품) | 19093 |
| `com.sparta.msa_learning.product3` | Product Service (상품) | 19094 |

## 기술 스택

- Java 21
- Spring Boot 4.0.2
- Spring Cloud 2025.1.0 (Oakwood)
- Spring Cloud Netflix Eureka
- Spring Cloud OpenFeign

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

> 트러블슈팅은 [TROUBLESHOOTING.md](TROUBLESHOOTING.md) 참고

---

## 1. MSA (Microservice Architecture)

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

---

## 2. Eureka

Netflix에서 개발한 **서비스 디스커버리(Service Discovery)** 도구로, MSA 환경에서 각 서비스의 위치(IP, 포트)를 동적으로 관리한다.

### 왜 필요한가?

MSA에서는 서비스가 여러 개의 인스턴스로 실행될 수 있고, 배포할 때마다 IP나 포트가 바뀔 수 있다.
서비스 A가 서비스 B를 호출하려면 B의 주소를 알아야 하는데, 이를 하드코딩하면 변경될 때마다 수정이 필요하다.
Eureka가 이 문제를 해결해준다.

### 동작 방식

```
[Eureka Server] (:19090)
    ├── ORDER-SERVICE   (:19091) ── UP
    └── PRODUCT-SERVICE (:19092, :19093, :19094) ── UP
```

1. **서비스 등록(Register)** : 각 서비스(Client)가 실행되면 Eureka Server에 자신의 정보를 등록
2. **헬스 체크(Heartbeat)** : 등록된 서비스는 주기적(기본 30초)으로 Eureka Server에 heartbeat를 전송. 일정 시간 응답이 없으면 해당 서비스를 레지스트리에서 제거
3. **서비스 조회(Discovery)** : 다른 서비스를 호출할 때 Eureka Server에서 대상 서비스의 주소를 조회하여 통신

### 직접 사용해본 결과

![Eureka Server Dashboard](com.sparta.msa_learning.server/src/main/resources/static/eureka%20server%20success.png)

직접 사용해보니 Eureka는 여러 포트로 열린 많은 프로젝트를 관리 및 연결해주는 서비스로 보인다.
아직 실제로 내부 서비스를 구현하지 않았기에 어떤 식으로 서비스 간의 통신이 되는지는 모르겠지만,
일단 어떤 프로세스가 **UP** 이고 접근 가능한지를 동적으로 체크해주는 것으로 보인다.

---

## 3. OpenFeign과 로드밸런싱

### OpenFeign이란?

Spring Cloud OpenFeign은 선언적 HTTP 클라이언트로, 인터페이스와 어노테이션만으로 다른 서비스의 API를 호출할 수 있게 해준다. Eureka와 함께 사용하면 서비스 이름만으로 호출이 가능하고, 별도의 URL 관리가 필요 없다.

### 구현 구조

```
[Order Service] (:19091)
    │
    │  @FeignClient(name="product-service")
    │  ProductClient 인터페이스로 호출
    │
    ├──→ [Product Service] (:19092)
    ├──→ [Product Service] (:19093)   ← Round Robin 로드밸런싱
    └──→ [Product Service] (:19094)
```

Order Service에서 `ProductClient` 인터페이스를 선언하면, OpenFeign이 Eureka에 등록된 `product-service` 인스턴스 중 하나를 선택하여 호출한다.

```java
@FeignClient(name="product-service")
public interface ProductClient {
    @GetMapping("/api/products/{id}")
    String getProductById(@PathVariable Long id);
}
```

### 검증 과정

#### Step 1. Eureka에 서비스 등록 확인

Order Service 1개와 Product Service 3개를 실행하고 Eureka Dashboard에서 모든 인스턴스가 UP 상태인지 확인했다.

![Eureka Dashboard - Order와 Product 서비스 등록 확인](images/orderAndProducts.png)

- ORDER-SERVICE: 1개 인스턴스 (19091)
- PRODUCT-SERVICE: 3개 인스턴스 (19092, 19093, 19094)

#### Step 2. Product API 포트별 직접 호출

각 Product 인스턴스에 직접 요청하여 포트별로 정상 응답하는지 확인했다.

![Product API 포트별 호출 결과](images/getProductWithPort.png)

- `GET localhost:19092/api/products/1` → `상품 ID 1에 대한 정보를 반환합니다. PORT : 19092`
- `GET localhost:19093/api/products/1` → `상품 ID 1에 대한 정보를 반환합니다. PORT : 19093`
- `GET localhost:19094/api/products/1` → `상품 ID 1에 대한 정보를 반환합니다. PORT : 19094`

각 인스턴스가 자신의 포트를 응답에 포함하여, 어떤 인스턴스가 처리했는지 구분할 수 있도록 했다.

#### Step 3. Order API에서 Product 호출 확인

Order Service를 통해 주문을 조회하면 내부적으로 FeignClient가 Product Service를 호출한다.

![Order API 호출 결과](images/Order1and2.png)

- `GET localhost:19091/api/orders/1` → `주문 ID: 1, 상품 ID 2에 대한 정보를 반환합니다. PORT : 19092 주문이 생성되었습니다.`
- `GET localhost:19091/api/orders/2` → `주문 정보를 찾을 수 없습니다.`

Order에서 Product를 FeignClient로 호출했을 때 응답에 Product의 포트 번호가 포함되어, 서비스 간 통신이 정상적으로 이루어지는 것을 확인했다.

#### Step 4. Round Robin 로드밸런싱 확인

같은 Order API를 반복 호출하여 Product Service의 포트가 순차적으로 바뀌는지 확인했다.

![Round Robin 로드밸런싱 확인](images/seeHowPortChanges.png)

```
1차 호출 → PORT : 19093
2차 호출 → PORT : 19092
3차 호출 → PORT : 19094
4차 호출 → PORT : 19093
5차 호출 → PORT : 19092
```

같은 `GET localhost:19091/api/orders/1`을 반복 호출했을 때, 응답의 PORT가 19092 → 19093 → 19094 순서로 순환하며 바뀌는 것을 확인했다. OpenFeign + Eureka 조합에서 기본으로 **Round Robin** 방식의 로드밸런싱이 적용된다.

---

## 4. Learning Points

### MSA는 이미 경험하고 있었다

어드민 페이지를 별도의 Go 서버로 분리한 것, Lambda로 각 API를 독립적으로 배포한 것 모두 MSA적 사고방식이었다. MSA는 새로운 개념이 아니라 "서비스를 독립적으로 분리한다"는 원칙의 체계화된 형태인 것 같다.

### "안전한 배포"의 의미

모놀리틱에서는 하나의 기능을 수정해도 전체를 다시 배포해야 하고, 그 과정에서 관련 없는 기능에 영향을 줄 수 있다. MSA에서는 변경된 서비스만 독립적으로 배포할 수 있어, 전에 들었던 "안전한 배포"는 바로 이 맥락이었다.

### 트레이드오프가 존재한다

MSA는 배포와 장애 격리에서는 이점이 있지만, 개발 복잡도가 확실히 올라간다. 서비스마다 별도의 프로젝트를 생성하고, 서비스 간 통신을 관리하고, Eureka 같은 인프라 도구를 추가로 운영해야 한다. 어드민 페이지를 Go로 분리했을 때 개발 기간이 늘어난 경험이 이를 잘 보여준다.

### Eureka는 서비스 간 "전화번호부"

Eureka의 핵심 역할은 각 서비스가 서로의 위치를 알 수 있게 해주는 것이다. 직접 실행해보니 Eureka Dashboard에서 어떤 서비스가 살아있고(UP), 어떤 포트에서 접근 가능한지를 한눈에 확인할 수 있었다.

### OpenFeign으로 서비스 간 통신이 이렇게 간단해진다

FeignClient 인터페이스 하나만 선언하면 다른 서비스의 API를 마치 로컬 메서드처럼 호출할 수 있다. URL을 하드코딩할 필요 없이 Eureka에 등록된 서비스 이름(`product-service`)만 지정하면 알아서 찾아간다. 이전에 궁금했던 "서비스 간 통신이 어떻게 되는지"에 대한 답을 찾은 것 같다.

### 로드밸런싱은 자동으로 되었다

별도의 로드밸런서 설정 없이도 OpenFeign + Eureka 조합만으로 Round Robin 로드밸런싱이 자동 적용되었다. 같은 API를 반복 호출하면 3개의 Product 인스턴스에 순차적으로 분배되는 것을 포트 번호로 직접 확인할 수 있었다.

---

## 5. Timeline

- 2026-02-10 : MSA와 Eureka에 대한 기본 개념 학습 시작
- 2026-02-11 : Eureka Server 및 Client 프로젝트 생성 및 실행
- 2026-02-11 : OpenFeign 적용, Product 3개 인스턴스 로드밸런싱 확인

