package com.example.board.frontend.dto;

public record EmailVerificationSendResponse(long otpExpiresInSeconds, long resendCooldownSeconds) {
}
