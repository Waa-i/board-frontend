package com.example.board.frontend.utils;

import com.example.board.frontend.dto.ApiCode;
import com.example.board.frontend.dto.ApiResponse;
import com.example.board.frontend.dto.CommonErrorCode;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class FeignExceptionUtils {
    private final JsonMapper jsonMapper;

    public ApiResponse<Void> extractErrorResponse(FeignException e) {
        String body = e.contentUTF8();
        if(body == null || body.isBlank()) return null;
        try {
            return jsonMapper.readValue(body, new TypeReference<>() {});
        } catch (Exception _) {
            return null;
        }
    }

    public ResponseEntity<ApiResponse<Void>> handleFeignException(FeignException e) {
        var status = e.status() > 0 ? e.status() : 503;
        var error = extractErrorResponse(e);
        if(error != null)
            return ResponseEntity.status(status).body(error);
        // 파싱 실패
        if(status == 503) {
            handleError(CommonErrorCode.SERVICE_UNAVAILABLE);
        }
        if(status >= 500) {
            handleError(CommonErrorCode.INTERNAL_SERVER_ERROR);
        }
        if(status == 403) {
            handleError(CommonErrorCode.FORBIDDEN);
        }
        if(status == 401) {
            handleError(CommonErrorCode.UNAUTHORIZED);
        }
        return ResponseEntity.badRequest().body(ApiResponse.error(CommonErrorCode.MALFORMED_REQUEST));
    }

    private ResponseEntity<ApiResponse<Void>> handleError(ApiCode code) {
        return ResponseEntity.status(code.getHttpStatus()).body(ApiResponse.error(code));
    }
}
