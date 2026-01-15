package com.example.board.frontend.dto;

import org.springframework.http.HttpStatus;

public interface ApiCode {
    String getCode();
    String getMessage();
    HttpStatus getHttpStatus();
}
