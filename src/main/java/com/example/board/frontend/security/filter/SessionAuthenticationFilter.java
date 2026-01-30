package com.example.board.frontend.security.filter;

import com.example.board.frontend.utils.SessionKeys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.Strings;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {
    private static final Set<String> SKIP_EXACT = Set.of("/favicon.ico", "/error", "/login", "/signup");
    private static final String[] SKIP_PREFIX = {"/assets/", "/signup/"};
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if(SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        var session = request.getSession(false);
        if(session == null) {
            filterChain.doFilter(request, response);
            return;
        }

        var memberId = (Long) session.getAttribute(SessionKeys.MEMBER_ID);
        var role = (String) session.getAttribute(SessionKeys.ROLE);
        if(memberId == null || role == null) {
            filterChain.doFilter(request, response);
            return;
        }

        var auth = new PreAuthenticatedAuthenticationToken(memberId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        var path = request.getServletPath();
        return SKIP_EXACT.contains(path) || Strings.CS.startsWithAny(path, SKIP_PREFIX);
    }
}
