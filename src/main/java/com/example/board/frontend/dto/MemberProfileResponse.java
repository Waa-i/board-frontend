package com.example.board.frontend.dto;

import java.time.LocalDateTime;

public record MemberProfileResponse(String nickname, LocalDateTime signedUpAt) {
}
