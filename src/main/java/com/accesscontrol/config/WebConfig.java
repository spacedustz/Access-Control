package com.accesscontrol.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class WebConfig {

    @Bean(name = "api")
    public RestTemplate template() {
        return new RestTemplate();
    }
}
