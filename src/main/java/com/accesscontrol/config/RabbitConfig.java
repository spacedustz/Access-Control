package com.accesscontrol.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    @Value("${spring.rabbitmq.host}")
    private String host;

    @Value("${spring.rabbitmq.port}")
    private int port;

    @Value("${spring.rabbitmq.username}")
    private String id;

    @Value("${spring.rabbitmq.password}")
    private String pw;

    // Message Converter Bean 주입
    @Bean
    MessageConverter converter() { return new Jackson2JsonMessageConverter(); }

    // RabbitMQ와의 연결을 위한 Connection Factory Bean 생성
    @Bean
    public ConnectionFactory factory() {
        CachingConnectionFactory factory = new CachingConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername(id);
        factory.setPassword(pw);

        return factory;
    }

    // Rabbit Template 생성
    @Bean
    RabbitTemplate template(org.springframework.amqp.rabbit.connection.ConnectionFactory factory, MessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(factory);
        template.setMessageConverter(converter);

        return template;
    }

    // Subscribe Listener
    @Bean
    SimpleRabbitListenerContainerFactory listener() {
        final SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(factory());
        factory.setMessageConverter(converter());
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);

        return factory;
    }
}
