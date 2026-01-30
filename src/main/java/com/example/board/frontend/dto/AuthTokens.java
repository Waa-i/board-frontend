package com.example.board.frontend.dto;

public record AuthTokens(Long id, String role, String accessToken, long accessTokenExpiresIn, String refreshToken, long refreshTokenExpiresIn) {
}
