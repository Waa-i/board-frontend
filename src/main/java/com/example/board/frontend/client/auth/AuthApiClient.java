package com.example.board.frontend.client.auth;

import com.example.board.frontend.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "api-gateway", contextId = "authApiClient")
public interface AuthApiClient {
    @PostMapping("/auth/signup")
    ApiResponse<Void> signUp(@RequestHeader("X-Signup-Proof") String token, @RequestBody MemberSignUpRequest request);

    @GetMapping("/auth/members/availability/username")
    ApiResponse<AvailabilityData> checkUsernameAvailability(@RequestParam("username") String username);

    @GetMapping("/auth/members/availability/email")
    ApiResponse<AvailabilityData> checkEmailAvailability(@RequestParam("email") String email);

    @PostMapping("/auth/email-verifications")
    ApiResponse<EmailVerificationSendResponse> sendOtp(@RequestBody EmailVerificationSendRequest request);

    @PostMapping("/auth/email-verifications/verify")
    ApiResponse<EmailVerificationVerifyResponse> verifyOtp(@RequestBody EmailVerificationVerifyRequest request);
}
