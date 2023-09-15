package com.accesscontrol.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RabbitTopicListener {

    @RabbitListener(queues = "test.queue")
    public void receive(String message) {
        log.info("Received Message: {}", message);
        System.out.println("Received Message: " + message);
    }
}
