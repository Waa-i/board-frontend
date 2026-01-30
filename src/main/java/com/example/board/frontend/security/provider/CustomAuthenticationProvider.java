package com.example.board.frontend.security.provider;

import com.example.board.frontend.client.auth.AuthApiClient;
import com.example.board.frontend.security.details.LoginRequestDetails;
import com.example.board.frontend.security.details.LoginSuccessDetails;
import com.example.board.frontend.utils.FeignExceptionUtils;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationProvider implements AuthenticationProvider{
    private static final String BAD_CREDENTIALS = "AUTH_LOGIN_E_001";
    private static final String ACCOUNT_DORMANT = "AUTH_LOGIN_E_002";
    private static final String ACCOUNT_WITHDRAWN = "AUTH_LOGIN_E_003";
    private static final String ACCOUNT_PENDING = "AUTH_LOGIN_E_004";

    private final AuthApiClient authApiClient;
    private final FeignExceptionUtils feignExceptionUtils;

    @Override
    public @Nullable Authentication authenticate(Authentication authentication) throws AuthenticationException {
        var username = authentication.getName();
        var password = String.valueOf(authentication.getCredentials());
        var details = (LoginRequestDetails) authentication.getDetails();
        var type = details.type().name();
        try {
            var authTokens = authApiClient.login(type, LoginRequest.of(username, password)).data();
            var auth = UsernamePasswordAuthenticationToken.authenticated(authTokens.id(), null, List.of(new SimpleGrantedAuthority("ROLE_" + authTokens.role())));
            auth.setDetails(new LoginSuccessDetails(authTokens, type));
            return auth;
        } catch (FeignException e) {
            throw translateFeignException(e);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private AuthenticationException translateFeignException(FeignException e) {
        var response = feignExceptionUtils.extractErrorResponse(e);
        if(response == null) throw new AuthenticationServiceException("인증 서버에서 응답을 처리할 수 없습니다.");
        return switch (response.code()) {
            case BAD_CREDENTIALS -> new BadCredentialsException(response.message());
            case ACCOUNT_DORMANT -> new LockedException(response.message());
            case ACCOUNT_PENDING -> new DisabledException(response.message());
            case ACCOUNT_WITHDRAWN -> new AccountExpiredException(response.message());
            default -> new AuthenticationServiceException(response.message());
        };
    }
}
