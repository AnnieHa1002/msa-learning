# Eureka

Netflix에서 개발한 **서비스 디스커버리(Service Discovery)** 도구로, MSA 환경에서 각 서비스의 위치(IP, 포트)를 동적으로 관리한다.

## 왜 필요한가?

MSA에서는 서비스가 여러 개의 인스턴스로 실행될 수 있고, 배포할 때마다 IP나 포트가 바뀔 수 있다.
서비스 A가 서비스 B를 호출하려면 B의 주소를 알아야 하는데, 이를 하드코딩하면 변경될 때마다 수정이 필요하다.
Eureka가 이 문제를 해결해준다.

## 동작 방식

```
[Eureka Server] (:19090)
    ├── ORDER-SERVICE   (:19091) ── UP
    └── PRODUCT-SERVICE (:19092, :19093, :19094) ── UP
```

1. **서비스 등록(Register)** : 각 서비스(Client)가 실행되면 Eureka Server에 자신의 정보를 등록
2. **헬스 체크(Heartbeat)** : 등록된 서비스는 주기적(기본 30초)으로 Eureka Server에 heartbeat를 전송. 일정 시간 응답이 없으면 해당 서비스를 레지스트리에서 제거
3. **서비스 조회(Discovery)** : 다른 서비스를 호출할 때 Eureka Server에서 대상 서비스의 주소를 조회하여 통신

## 직접 사용해본 결과

![Eureka Server Dashboard](../com.sparta.msa_learning.server/src/main/resources/static/eureka%20server%20success.png)

직접 사용해보니 Eureka는 여러 포트로 열린 많은 프로젝트를 관리 및 연결해주는 서비스로 보인다.
아직 실제로 내부 서비스를 구현하지 않았기에 어떤 식으로 서비스 간의 통신이 되는지는 모르겠지만,
일단 어떤 프로세스가 **UP** 이고 접근 가능한지를 동적으로 체크해주는 것으로 보인다.

## 배운 점

Eureka의 핵심 역할은 각 서비스가 서로의 위치를 알 수 있게 해주는 것이다. 직접 실행해보니 Eureka Dashboard에서 어떤 서비스가 살아있고(UP), 어떤 포트에서 접근 가능한지를 한눈에 확인할 수 있었다.