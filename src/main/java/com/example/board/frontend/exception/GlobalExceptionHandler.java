package com.example.board.frontend.exception;

import com.example.board.frontend.dto.ApiCode;
import com.example.board.frontend.dto.ApiResponse;
import com.example.board.frontend.dto.CommonErrorCode;
import com.example.board.frontend.utils.FeignExceptionUtils;
import feign.FeignException;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private final FeignExceptionUtils feignExceptionUtils;

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiResponse<Void>> handleFeignException(FeignException e) {
        return feignExceptionUtils.handleFeignException(e);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException() {
        return handleError(CommonErrorCode.MALFORMED_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException() {
        return handleError(CommonErrorCode.MALFORMED_REQUEST);
    }

    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingRequestCookieException(MissingRequestCookieException e) {
        if("signup_proof".equals(e.getCookieName())) {
            return handleError(CommonErrorCode.SIGNUP_PROOF_TOKEN_EXPIRED);
        }
        return handleError(CommonErrorCode.MALFORMED_REQUEST);
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException() {
        return handleError(CommonErrorCode.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ApiResponse<Void>> handleError(ApiCode code) {
        return ResponseEntity.status(code.getHttpStatus()).body(ApiResponse.error(code));
    }
}
