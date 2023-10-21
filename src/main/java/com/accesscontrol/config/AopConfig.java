package com.accesscontrol.config;

import com.accesscontrol.aspect.EventAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
public class AopConfig {
    @Bean
    public EventAspect aspect() {
        return new EventAspect();
    }
}
