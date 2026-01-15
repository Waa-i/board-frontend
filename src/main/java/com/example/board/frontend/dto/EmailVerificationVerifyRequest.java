package com.example.board.frontend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailVerificationVerifyRequest(
        @NotBlank
        @Email
        String email,
        @NotBlank
        String otp) {
}
