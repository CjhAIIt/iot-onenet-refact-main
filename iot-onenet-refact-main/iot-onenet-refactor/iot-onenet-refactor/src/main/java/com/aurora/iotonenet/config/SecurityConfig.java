package com.aurora.iotonenet.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
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
                                "/api/logout",
                                "/api/status"
                        ).permitAll()
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}