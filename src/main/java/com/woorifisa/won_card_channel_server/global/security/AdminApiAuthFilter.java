package com.woorifisa.won_card_channel_server.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.woorifisa.won_card_channel_server.domain.auth.exception.code.AuthErrorCode;
import com.woorifisa.won_card_channel_server.global.response.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Component
public class AdminApiAuthFilter extends OncePerRequestFilter {

    private static final String ADMIN_API_TOKEN_HEADER = "X-Admin-Api-Token";

    private final ObjectMapper objectMapper;
    private final String expectedAdminApiToken;

    public AdminApiAuthFilter(
            ObjectMapper objectMapper,
            @Value("${admin.api-token:}") String expectedAdminApiToken
    ) {
        this.objectMapper = objectMapper;
        this.expectedAdminApiToken = normalize(expectedAdminApiToken);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = resolveRequestPath(request);
        return !(path.equals("/api/admin") || path.startsWith("/api/admin/"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String adminApiToken = normalize(request.getHeader(ADMIN_API_TOKEN_HEADER));

        if (!constantTimeEquals(expectedAdminApiToken, adminApiToken)) {
            writeUnauthorizedResponse(response);
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "admin",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private String resolveRequestPath(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        if (servletPath != null && !servletPath.isBlank()) {
            return servletPath;
        }

        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null
                && !contextPath.isBlank()
                && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }

        return requestUri;
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (expected.isBlank() || actual.isBlank()) {
            return false;
        }

        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);

        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private void writeUnauthorizedResponse(HttpServletResponse response) throws IOException {
        response.setStatus(AuthErrorCode.AUTHENTICATION_REQUIRED.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(AuthErrorCode.AUTHENTICATION_REQUIRED));
    }
}
