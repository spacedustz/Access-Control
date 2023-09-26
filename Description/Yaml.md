## 📘 **application.yml**

RabbitMQ, JPA, H2 등등 설정 파일 구성

<br>

**H2 Embedded Mode : 데이터를 로컬 디스크에 쓰기**
- H2를 InMemory Mode로 사용하지 않고, Embedded Mode로 특정 Path 을 지정해 주었습니다. (E:\Data\H2\H2)
- 여기서 **H2가 2번인 이유**는 `첫 H2`는 폴더 이름이고 `두번째 H2`는 데이터가 저장될 mv.db 파일의 이름입니다.
- 즉, E:/Data/H2 디렉터리 밑에 H2라는 이름의 mv.db 파일을 만들거라는 의미입니다. (H2.mv.db)

<br>

**ddl-auto**
- 테스트 할떈 create, update를 사용했지만 상용에선 데이터의 축적을 위해 none으로 사용 합니다.

<br>

```yaml
server:  
  servlet:  
    encoding:  
      charset: UTF-8  
      force-response: true  
  port: 8090  
  
spring:  
  # H2 설정  
  h2:  
    console:  
      enabled: true  
      path: /h2  
  datasource:  
    url: jdbc:h2:file:E:\Data\H2\H2  
    username: root  
    password: 1234  
  
  # JPA 설정  
  jpa:  
    open-in-view: false  
    hibernate:  
      ddl-auto: none  
    show-sql: false  
    properties:  
      hibernate:  
        format_sql: true  
  
  # RabbitMQ 설정  
  rabbitmq:  
    host: localhost  
    port: 5672  
    username: guest  
    password: guest  
  
# Logging  
logging:  
  level:  
    org:  
      hibernate: info  
```