package com.accesscontrol.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitTopicListener {

    @RabbitListener(queues = "test.queue")
    public void receive(String message) {
        log.info("Received Message: {}", message);
        System.out.println("Received Message: " + message);

        ClassPathResource resource = new ClassPathResource("sample/sample-tripwire.json");
        List<JsonNode> rootNode = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();

        String jsonContent = null;
        try {
            jsonContent = Files.readString(Paths.get(resource.getURI()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String[] splitJson = jsonContent.split("\n");

        for (String json : splitJson) {
            JsonNode jsonObject = null;
            try {
                jsonObject = objectMapper.readTree(json);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            rootNode.add(jsonObject);
        }

        for (JsonNode node : rootNode) {

        }
    }
}
