package com.example.board.frontend.security.details;

import com.example.board.frontend.dto.AuthTokens;

public record LoginSuccessDetails(AuthTokens tokens, String type) {
}
