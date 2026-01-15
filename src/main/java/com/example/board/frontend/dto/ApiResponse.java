package com.example.board.frontend.dto;

public record ApiResponse<T>(Boolean success, String code, String message, T data) {
    public static <T> ApiResponse<T> error(ApiCode code) {
        return new ApiResponse<>(false, code.getCode(), code.getMessage(), null);
    }
}