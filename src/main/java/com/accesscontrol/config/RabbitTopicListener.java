package com.accesscontrol.config;

import com.accesscontrol.repository.CountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitTopicListener {

    private CountRepository countRepository;

    @RabbitListener(queues = "test.queue")
    public void receive(Message message) {
        log.info("Received Message: " + message.toString());
//        System.out.println("Received Message: " + message);
//        Count entity = Count.createOf(message.getExtra().getCount(), message.getEventTime());
//        countRepository.save(entity);
//        log.info("Entity", entity);
    }
}
