package com.shweta.consultation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan
public class ConsultationApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConsultationApplication.class, args);
        System.out.println("consultation application started");
    }
}
