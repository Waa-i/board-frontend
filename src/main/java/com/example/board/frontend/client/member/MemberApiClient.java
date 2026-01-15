package com.example.board.frontend.client.member;

import com.example.board.frontend.dto.ApiResponse;
import com.example.board.frontend.dto.AvailabilityData;
import com.example.board.frontend.dto.MemberProfileResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "api-gateway", contextId = "memberApiClient")
public interface MemberApiClient {
    @GetMapping("/api/members/{member-id}")
    ApiResponse<MemberProfileResponse> getProfile(@PathVariable("member-id") Long id);
    @GetMapping("/api/members/availability")
    ApiResponse<AvailabilityData> checkNicknameAvailability(@RequestParam("nickname") String nickname);
}
