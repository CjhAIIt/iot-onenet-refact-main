package com.aurora.iotonenet.config;

import org.springframework.context.annotation.Bean;
import org.springframework.security.authorization.AuthorizationDecision;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, error) -> response.sendError(401))
                        .accessDeniedHandler((request, response, error) -> response.sendError(403)))
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/preview.html",
                                "/login.html",
                                "/register.html",
                                "/index.html",
                                "/docs/**",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/api/login",
                                "/api/register",
                                "/api/check-login",
                                "/api/csrf",
                                "/api/health"
                        ).permitAll()
                        .requestMatchers("/api/**").access((authentication, context) -> {
                            HttpSession session = context.getRequest().getSession(false);
                            Object username = session == null ? null : session.getAttribute("logged_in_user");
                            return new AuthorizationDecision(username instanceof String && !((String) username).isBlank());
                        })
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
