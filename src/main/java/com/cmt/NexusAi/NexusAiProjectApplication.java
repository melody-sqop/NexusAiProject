package com.cmt.NexusAi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableRetry
@EnableScheduling
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true) // springSecurity权限注解启动
public class NexusAiProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexusAiProjectApplication.class, args);
    }

}
