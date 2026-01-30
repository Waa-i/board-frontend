package com.example.board.frontend.security.handler;

import com.example.board.frontend.config.SessionProperties;
import com.example.board.frontend.security.details.LoginSuccessDetails;
import com.example.board.frontend.utils.SessionKeys;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
    private final SessionProperties sessionProperties;

    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        var details = (LoginSuccessDetails) authentication.getDetails();
        var tokens = details.tokens();
        var principalIndexName = getIndexName(tokens.id(), details.type());

        // 기존에 로그인 되어 있는 세션 삭제
        removePreviousSession(principalIndexName);
        // 세션 생성
        var session = request.getSession(true);
        session.setAttribute(SessionKeys.MEMBER_ID, tokens.id());
        session.setAttribute(SessionKeys.ROLE, tokens.role());
        session.setAttribute(SessionKeys.ACCESS_TOKEN, tokens.accessToken());
        session.setAttribute(SessionKeys.REFRESH_TOKEN, tokens.refreshToken());
        session.setAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, principalIndexName);
        setSessionTtl(session, tokens.refreshTokenExpiresIn());

        setDefaultTargetUrl("/");
        super.onAuthenticationSuccess(request, response, authentication);
    }

    private String getIndexName(Long memberId, String deviceType) {
        return "%d:%s".formatted(memberId, deviceType);
    }

    private void setSessionTtl(HttpSession session, long refreshTokenExpiresIn) {
        var sessionTtl = (int) (refreshTokenExpiresIn - sessionProperties.expiryBuffer().toSeconds());
        if(sessionTtl > 0) {
            session.setMaxInactiveInterval(sessionTtl);
        }
    }

    private void removePreviousSession(String principalIndexName) {
        var sessionMap = sessionRepository.findByPrincipalName(principalIndexName);
        for (var session : sessionMap.values()) {
            sessionRepository.deleteById(session.getId());
        }
    }
}
