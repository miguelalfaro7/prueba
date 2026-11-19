package com.example.iot.actiondispatcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.springframework.scheduling.annotation.EnableScheduling
@SpringBootApplication
public class ActionDispatcherApplication {

    public static void main(String[] args) {

        SpringApplication.run(
                ActionDispatcherApplication.class,
                args
        );

    }
}
