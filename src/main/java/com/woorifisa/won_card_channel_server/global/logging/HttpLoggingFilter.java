package com.woorifisa.won_card_channel_server.global.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
@RequiredArgsConstructor
public class HttpLoggingFilter extends OncePerRequestFilter {

    private final ErrorLogService errorLogService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            int status = response.getStatus();
            if (status >= 400) {
                long elapsed = System.currentTimeMillis() - startTime;
                try {
                    MDC.put("http.method", request.getMethod());
                    MDC.put("http.uri", request.getRequestURI());
                    MDC.put("http.status", String.valueOf(status));
                    MDC.put("elapsed_ms", String.valueOf(elapsed));
                    if (status >= 500) {
                        log.error("http error method={} uri={} status={} elapsed_ms={}",
                                request.getMethod(), request.getRequestURI(), status, elapsed);
                    } else {
                        log.warn("http error method={} uri={} status={} elapsed_ms={}",
                                request.getMethod(), request.getRequestURI(), status, elapsed);
                    }
                } finally {
                    MDC.remove("http.method");
                    MDC.remove("http.uri");
                    MDC.remove("http.status");
                    MDC.remove("elapsed_ms");
                }
                try {
                    errorLogService.record(status, request.getMethod(), request.getRequestURI(), elapsed);
                } catch (Exception e) {
                    log.warn("error log record failed: {}", e.getMessage());
                }
            }
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/actuator")
                || uri.startsWith("/swagger-ui")
                || uri.startsWith("/v3/api-docs");
    }
}
