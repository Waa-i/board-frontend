package com.example.board.frontend.controller;

import com.example.board.frontend.client.auth.AuthApiClient;
import com.example.board.frontend.client.member.MemberApiClient;
import com.example.board.frontend.dto.*;
import com.example.board.frontend.dto.EmailVerificationSendRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Validated
@Controller
@RequiredArgsConstructor
public class MemberController {
    private final AuthApiClient authApiClient;
    private final MemberApiClient memberApiClient;

    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("form", MemberSignUpRequest.empty());
        return "auth/signup";
    }

    @PostMapping("/signup")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> signUp(@CookieValue("signup_proof") String token, @Valid @RequestBody MemberSignUpRequest request) {
        log.info("이메일 인증 완료 토큰: {}", token);
        return ResponseEntity.ok(authApiClient.signUp(token, request));
    }

    @GetMapping("/signup/availability")
    @ResponseBody
    public ResponseEntity<ApiResponse<AvailabilityData>> checkSignupAvailability(
            @NotBlank(message = "field 값은 필수입니다.")
            @RequestParam("field")
            String field,
            @NotBlank(message = "value 값은 필수입니다.")
            @RequestParam("value")
            String value) {
        var normField = field.strip();
        var normValue = value.strip();

        return switch (normField) {
            case "username" -> ResponseEntity.ok(authApiClient.checkUsernameAvailability(normValue));
            case "email" -> ResponseEntity.ok(authApiClient.checkEmailAvailability(normValue));
            case "nickname" -> ResponseEntity.ok(memberApiClient.checkNicknameAvailability(normValue));
            default -> ResponseEntity.badRequest().body(ApiResponse.error(CommonErrorCode.UNSUPPORTED_PARAMETER));
        };
    }

    @PostMapping("/signup/email-verifications")
    @ResponseBody
    public ResponseEntity<ApiResponse<EmailVerificationSendResponse>> sendOtp(@Valid @RequestBody EmailVerificationSendRequest request) {
        return ResponseEntity.ok(authApiClient.sendOtp(request));
    }

    @PostMapping("/signup/email-verifications/verify")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> verifyOtp(@Valid @RequestBody EmailVerificationVerifyRequest request, HttpServletResponse response) {
        var downstreamResponse = authApiClient.verifyOtp(request);
        EmailVerificationVerifyResponse data = downstreamResponse.data();

        ResponseCookie cookie = ResponseCookie.from("signup_proof", data.token())
                .httpOnly(true)
                .secure(false)
                .path("/signup")
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(new ApiResponse<>(true, downstreamResponse.code(), downstreamResponse.message(), null));
    }
}
