## 📘 인스턴스 Health Check 스레드

무한루프틑 돌며 카메라 인스턴스의 상태를 Spring WebClient를 사용해 카메라 서버에 Rest API 요청을 통해 받습니다.

그 후, 받아온 인스턴스의 정보를 DTO로 역직렬화 하여, 상태값에 따라 인스턴스가 실행중이 아니라면,

다시 Webclient를 이용해 Post 요청을 보내 인스턴스를 시작 시키는 스레드입니다.

<br>  

> 📌 **WebConfig**

WebClient와 TaskExecutor Bean을 정의 해줍니다.

스레드풀은 어플리케이션의 부하 상태나 요구 사항에 따라 Custom 하게 바꿔주면 됩니다.

```java  
@Configuration  
public class WebConfig {  
  
    @Bean(name = "api")  
    public RestTemplate template() {  
        return new RestTemplate();  
    }  
  
    @Bean  
    public WebClient webClient() {  
        return WebClient.builder().build();  
    }  
  
    @Bean  
    public TaskExecutor executor() {  
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();  
        executor.setCorePoolSize(1);  
        executor.setMaxPoolSize(1);  
        executor.setQueueCapacity(1);  
        executor.setThreadNamePrefix("Instance Thread-");  
        executor.initialize();  
  
        return executor;  
    }  
} 
```  

<br>  

> 📌 **InstanceMonitoringThread**

무한 루프를 돌며, 카메라 인스턴스의 상태를 Health Check 하는 스레드입니다.

- init() : 이 클래스가 초기화 될 때 내부 함수인 **monitoringInstanceConnection**를 Execute 시켜 줍니다.
- run() : 스레드가 실행되면 인스턴스의 상태값을 Rest API에 요청해 가져오고, 상태값에 따라 인스턴스를 시작 시킵니다.
- getRequest() : Spring WebClient를 통해 Rest API를 호출합니다. [GET]
- postRequest() : 카메라 인스턴스를 시작 시키기 위한 Rest API를 호출합니다. [POST]
- monitoringInstanceConnection() : 스레드풀 만큼의 스레드를 만들어 루프를 돌며 Health Check Thread를 실행시키는 Executor 입니다.

```java  
@Slf4j  
@Service  
@RequiredArgsConstructor  
public class InstanceMonitoringThread extends Thread {  
    private final TaskExecutor executor;  
    private final WebClient webClient;  
    private final ObjectMapper mapper;  
    private String instanceName = "SecuRT-Tripwire";  
  
    @PostConstruct  
    public void init() {  
        this.monitoringInstanceConnection();  
    }  
  
    @Override  
    public void run() {  
        while (true) {  
            try {  
                String uri = "http://localhost:8080/api/instance/get?instance_name=" + instanceName;  
                String instanceStatement = getRequest(uri).block();  
                InstanceDto[] instances = mapper.readValue(instanceStatement, InstanceDto[].class);  
                log.info("Instance 결과값 : {}", instanceStatement);  
  
                if (instances != null && instances.length > 0) {  
                    Arrays.stream(instances).forEach(instance -> {  
  
                        log.info("Instance 상태 : {}", instances.toString());  
  
                        if (instance.getState() == 0 || instance.getState() == 1 || instance.getState() == 3 || instance.getState() == 5) {  
                            String startUri = "http://localhost:8080/api/instance/start";  
  
                            InstanceDto requestBody = new InstanceDto();  
                            requestBody.setInstanceName(instance.getInstanceName());  
                            requestBody.setSolution(instance.getSolution());  
  
                            try {  
                                String requestBodyStr = mapper.writeValueAsString(requestBody);  
                                String response = postRequest(startUri, requestBodyStr).block();  
  
                                log.info("Instance 시작 : {}, {}", requestBodyStr, response);  
                            } catch (Exception e) {  
                                log.error("Instance 시작 실패 with Exception : {}", e.getMessage());  
                            }  
                        }  
                    });  
                }  
            } catch (Exception e) {  
                log.warn("Instance Monitoring Failed with an Exception : {}", e.getMessage());  
            }  
  
            try {  
                Thread.sleep(10000);  
            } catch (InterruptedException e) {  
                e.printStackTrace();  
            }  
        }  
    }  
  
    public Mono<String> getRequest(final String uri) {  
        return webClient.get().uri(uri).retrieve().bodyToMono(String.class);  
    }  
  
    public Mono<String> postRequest(final String uri, final Object data) {  
        return webClient.post().uri(uri).bodyValue(data).retrieve().bodyToMono(String.class);  
    }  
  
    private void monitoringInstanceConnection() {  
        executor.execute(() -> {  
            while (true) {  
                InstanceMonitoringThread instanceThread = new InstanceMonitoringThread(executor, webClient, mapper);  
                executor.execute(instanceThread);  
            }  
        });  
    }  
} 
```  

<br>  

> 📌 **실행 결과**

![img](https://raw.githubusercontent.com/spacedustz/Obsidian-Image-Server/main/img2/h-thread.png)