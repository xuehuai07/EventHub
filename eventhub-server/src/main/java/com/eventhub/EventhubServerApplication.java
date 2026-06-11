package com.eventhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EventhubServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventhubServerApplication.class, args);
    }
}
