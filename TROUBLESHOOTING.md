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
