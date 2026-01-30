package com.example.board.frontend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "session")
public record SessionProperties(Duration expiryBuffer) {
}
