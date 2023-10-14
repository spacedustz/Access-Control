package com.accesscontrol.thread;

import com.accesscontrol.dto.InstanceDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;

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
            InstanceMonitoringThread instanceThread = new InstanceMonitoringThread(executor, webClient, mapper);
            executor.execute(instanceThread);
        });
    }
}
