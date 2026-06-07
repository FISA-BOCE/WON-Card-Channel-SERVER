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
import java.util.Arrays;
import java.util.List;

@Component
public class InternalApiAuthFilter extends OncePerRequestFilter {

    private static final String SERVICE_ID_HEADER = "X-Service-ID";
    private static final String API_KEY_HEADER = "X-Internal-Api-Key";

    private final ObjectMapper objectMapper;
    private final List<String> allowedServiceIds;
    private final String expectedApiKey;

    public InternalApiAuthFilter(
            ObjectMapper objectMapper,
            @Value("${internal.allowed-service-ids:}") String allowedServiceIds,
            @Value("${internal.api-key:}") String expectedApiKey
    ) {
        this.objectMapper = objectMapper;
        this.allowedServiceIds = parseAllowedServiceIds(allowedServiceIds);
        this.expectedApiKey = normalize(expectedApiKey);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = resolveRequestPath(request);
        return !(path.equals("/internal") || path.startsWith("/internal/"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String serviceId = normalize(request.getHeader(SERVICE_ID_HEADER));
        String apiKey = request.getHeader(API_KEY_HEADER);

        if (!isValidInternalRequest(serviceId, apiKey)) {
            writeUnauthorizedResponse(response);
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        serviceId,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_INTERNAL"))
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

    private boolean isValidInternalRequest(String serviceId, String apiKey) {
        return allowedServiceIds.stream().anyMatch(allowedServiceId -> constantTimeEquals(allowedServiceId, serviceId))
                && constantTimeEquals(expectedApiKey, apiKey);
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || expected.isBlank() || actual == null) {
            return false;
        }

        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);

        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }

    private List<String> parseAllowedServiceIds(String value) {
        return Arrays.stream(normalize(value).split(","))
                .map(this::normalize)
                .filter(serviceId -> !serviceId.isBlank())
                .toList();
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
