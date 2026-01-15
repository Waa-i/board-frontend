package com.example.board.frontend.dto;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CommonErrorCode implements ApiCode {
    MALFORMED_REQUEST("FRONT_COMMON_E_001", "잘못된 요청 형식입니다.", HttpStatus.BAD_REQUEST),
    UNSUPPORTED_PARAMETER("FRONT_COMMON_E_002", "지원하지 않는 파라미터입니다.", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("FRONT_COMMON_E_003", "로그인이 필요합니다. 다시 로그인해주세요.", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("FRONT_COMMON_E_004", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    SIGNUP_PROOF_TOKEN_EXPIRED("FRONT_COMMON_E_005", "이메일 인증이 만료되었습니다. 다시 인증해주세요.", HttpStatus.BAD_REQUEST),
    INTERNAL_SERVER_ERROR("FRONT_COMMON_E_006", "현재 요청을 처리할 수 없습니다. 잠시 후 다시 시도해주세요.", HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE("FRONT_COMMON_E_007", "현재 요청을 처리할 수 없습니다. 잠시 후 다시 시도해주세요.", HttpStatus.SERVICE_UNAVAILABLE)
    ;

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    CommonErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
