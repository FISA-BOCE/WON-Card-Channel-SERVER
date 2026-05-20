package com.woorifisa.won_card_channel_server.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.woorifisa.won_card_channel_server.domain.auth.exception.code.AuthErrorCode;
import com.woorifisa.won_card_channel_server.domain.auth.service.TokenBlacklistService;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import com.woorifisa.won_card_channel_server.global.response.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final List<String> protectedPrefixes;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider,
            TokenBlacklistService tokenBlacklistService,
            List<String> protectedPrefixes
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenBlacklistService = tokenBlacklistService;
        this.protectedPrefixes = protectedPrefixes;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return protectedPrefixes.stream().noneMatch(prefix -> request.getRequestURI().startsWith(prefix));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                throw new BusinessException(AuthErrorCode.AUTHENTICATION_REQUIRED);
            }

            String token = authorization.substring(7);
            AuthenticatedUser authenticatedUser = jwtTokenProvider.parse(token);
            if (tokenBlacklistService.isBlacklisted(authenticatedUser.jti())) {
                throw new BusinessException(AuthErrorCode.TOKEN_EXPIRED, "이미 만료되었거나 유효하지 않은 토큰입니다.");
            }

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    authenticatedUser,
                    null,
                    AuthorityUtils.NO_AUTHORITIES
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (BusinessException e) {
            SecurityContextHolder.clearContext();
            response.setStatus(e.getErrorCode().getHttpStatus().value());
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), ErrorResponse.of(e.getErrorCode(), e.getMessage()));
        }
    }
}