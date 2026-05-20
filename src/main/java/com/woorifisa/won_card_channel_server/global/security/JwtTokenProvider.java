package com.woorifisa.won_card_channel_server.global.security;

import com.woorifisa.won_card_channel_server.domain.auth.exception.code.AuthErrorCode;
import com.woorifisa.won_card_channel_server.global.config.SecurityProperties;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private final SecretKey signingKey;
    private final SecurityProperties securityProperties;

    public JwtTokenProvider(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
        this.signingKey = Keys.hmacShaKeyFor(securityProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UUID userUuid, UUID authUserUuid, String jti) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(securityProperties.getAccessTokenExpirationSeconds());

        return Jwts.builder()
                .subject(userUuid.toString())
                .claim("authUserUuid", authUserUuid.toString())
                .id(jti)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    public AuthenticatedUser parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(signingKey).build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new AuthenticatedUser(
                    UUID.fromString(claims.get("authUserUuid", String.class)),
                    UUID.fromString(claims.getSubject()),
                    claims.getId(),
                    token
            );
        } catch (ExpiredJwtException e) {
            throw new BusinessException(AuthErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
        }
    }

    public long getRemainingValidityMillis(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(signingKey).build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getExpiration().getTime() - System.currentTimeMillis();
        } catch (ExpiredJwtException e) {
            return 0L;
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
        }
    }

    public long getAccessTokenExpirationSeconds() {
        return securityProperties.getAccessTokenExpirationSeconds();
    }

    public long getAccessTokenExpirationMillis() {
        return securityProperties.getAccessTokenExpirationSeconds() * 1000;
    }
}
