package com.woorifisa.won_card_channel_server.global.config;

import com.woorifisa.won_card_channel_server.domain.auth.service.TokenBlacklistService;
import com.woorifisa.won_card_channel_server.global.security.InternalApiAuthFilter;
import com.woorifisa.won_card_channel_server.global.security.JwtAuthenticationFilter;
import com.woorifisa.won_card_channel_server.global.security.JwtTokenProvider;
import com.woorifisa.won_card_channel_server.global.security.RestAccessDeniedHandler;
import com.woorifisa.won_card_channel_server.global.security.RestAuthenticationEntryPoint;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtTokenProvider jwtTokenProvider,
            TokenBlacklistService tokenBlacklistService,
            InternalApiAuthFilter internalApiAuthFilter,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/signup", "/api/auth/login", "/api/auth/refresh").permitAll()
                        .requestMatchers("/internal/**").hasRole("INTERNAL")
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/cards/applications").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/cards/applications/invest-accounts").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/cards").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/cards/spend-summary").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/cards/rewards/monthly").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/cards/rewards/ledger", "/api/cards/rewards/ledger/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/cards/*/auto-invest").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/cards/*/auto-invest").authenticated()
                        .requestMatchers("/api/users", "/api/users/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/ai/chat").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterAfter(
                        new JwtAuthenticationFilter(
                                jwtTokenProvider,
                                tokenBlacklistService
                        ),
                        UsernamePasswordAuthenticationFilter.class
                )
                .addFilterBefore(internalApiAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .cors(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
