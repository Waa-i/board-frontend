package com.example.board.frontend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class BoardFrontendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BoardFrontendApplication.class, args);
    }

}
