package com.example.board.frontend.security.detailssource;

import com.example.board.frontend.security.details.LoginRequestDetails;
import com.example.board.frontend.utils.DeviceTypeResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.stereotype.Component;

@Component
public class LoginRequestAuthenticationDetailsSource implements AuthenticationDetailsSource<HttpServletRequest, LoginRequestDetails> {
    @Override
    public LoginRequestDetails buildDetails(HttpServletRequest context) {
        var deviceType = DeviceTypeResolver.resolve(context.getHeader("User-Agent"));
        return new LoginRequestDetails(deviceType);
    }
}
