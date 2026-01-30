package com.example.board.frontend.config;

import com.example.board.frontend.security.details.LoginRequestDetails;
import com.example.board.frontend.security.filter.SessionAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SessionAuthenticationFilter authenticationFilter, AuthenticationDetailsSource<HttpServletRequest, LoginRequestDetails> detailsSource, SessionProperties sessionProperties) {
        http.csrf(AbstractHttpConfigurer::disable);
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                .requestMatchers("/", "/signup/**").permitAll()
                .anyRequest().authenticated());
        http.formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .authenticationDetailsSource(detailsSource)
                .permitAll());
        http.logout(logout -> logout
                .logoutUrl("/logout")
                .invalidateHttpSession(true)
                .deleteCookies("sid"));
        http.securityContext(security -> security
                .securityContextRepository(new RequestAttributeSecurityContextRepository()));
        http.addFilterBefore(authenticationFilter, AuthorizationFilter.class);
        http.sessionManagement(session -> session
                .sessionFixation(SessionManagementConfigurer.SessionFixationConfigurer::changeSessionId));

        return http.build();
    }
}
