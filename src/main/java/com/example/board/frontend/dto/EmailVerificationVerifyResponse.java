package com.example.board.frontend.dto;

public record EmailVerificationVerifyResponse(String token, long expiresInSeconds) {
}
