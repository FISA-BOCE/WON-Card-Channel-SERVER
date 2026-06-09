package com.woorifisa.won_card_channel_server.domain.auth.service.impl;

import com.woorifisa.won_card_channel_server.domain.auth.dto.request.CreateLoginRequest;
import com.woorifisa.won_card_channel_server.domain.auth.dto.response.CreateLoginResponse;
import com.woorifisa.won_card_channel_server.domain.auth.dto.request.CreateTokenReissueRequest;
import com.woorifisa.won_card_channel_server.domain.auth.dto.response.CreateTokenReissueResponse;
import com.woorifisa.won_card_channel_server.domain.auth.dto.request.DeleteLogoutRequest;
import com.woorifisa.won_card_channel_server.domain.auth.dto.request.RegisterUserRequest;
import com.woorifisa.won_card_channel_server.domain.auth.model.CardChnAuthSession;
import com.woorifisa.won_card_channel_server.domain.auth.model.CardChnAuthUser;
import com.woorifisa.won_card_channel_server.domain.auth.model.UserStatus;
import com.woorifisa.won_card_channel_server.domain.auth.exception.code.AuthErrorCode;
import com.woorifisa.won_card_channel_server.domain.auth.repository.CardChnAuthSessionRepository;
import com.woorifisa.won_card_channel_server.domain.auth.repository.CardChnAuthUserRepository;
import com.woorifisa.won_card_channel_server.domain.auth.service.AuthService;
import com.woorifisa.won_card_channel_server.domain.auth.service.LedgerAuthClientService;
import com.woorifisa.won_card_channel_server.domain.auth.service.RefreshTokenService;
import com.woorifisa.won_card_channel_server.domain.auth.service.TokenBlacklistService;
import com.woorifisa.won_card_channel_server.domain.user.dto.request.InitializeUserMappingRequest;
import com.woorifisa.won_card_channel_server.domain.user.dto.response.GetMyUserMappingResponse;
import com.woorifisa.won_card_channel_server.domain.user.external.CommonUserMappingApi;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import com.woorifisa.won_card_channel_server.global.security.AuthenticatedUser;
import com.woorifisa.won_card_channel_server.global.security.JwtTokenProvider;
import com.woorifisa.won_card_channel_server.global.security.RequestAccessTokenHolder;
import com.woorifisa.won_card_channel_server.global.security.TextEncryptor;
import com.woorifisa.won_card_channel_server.global.util.HashUtils;
import feign.FeignException;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final CardChnAuthUserRepository userRepository;
    private final CardChnAuthSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final LedgerAuthClientService ledgerAuthClientService;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RequestAccessTokenHolder requestAccessTokenHolder;
    private final TextEncryptor textEncryptor;
    private final CommonUserMappingApi commonUserMappingApi;

    @Override
    @Transactional
    public void registerUser(RegisterUserRequest request) {
        String phoneNumber = request.phoneNumber();
        String telHash = HashUtils.sha256(phoneNumber);
        if (userRepository.findByTelHash(telHash).isPresent()) {
            throw new BusinessException(AuthErrorCode.DUPLICATE_PHONE_NUMBER);
        }

        CardChnAuthUser user = CardChnAuthUser.builder()
                .authUserUuid(UUID.randomUUID())
                .userUuid(UUID.randomUUID())
                .loginId(telHash)
                .userName(request.userName().trim())
                .emailEnc(textEncryptor.encrypt(request.email().trim()))
                .telEnc(textEncryptor.encrypt(phoneNumber))
                .telHash(telHash)
                .passwordHash(passwordEncoder.encode(request.password()))
                .userStatus(UserStatus.ACTIVE)
                .build();

        userRepository.save(user);
        initializeCommonUserMapping(user);
    }

    @Override
    @Transactional
    public CreateLoginResponse authenticateUser(CreateLoginRequest request) {
        String telHash = HashUtils.sha256(request.userId());
        CardChnAuthUser user = userRepository.findByTelHashForUpdate(telHash)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_CREDENTIALS));

        validateActiveUser(user);

        if (!ledgerAuthClientService.fetchAuthenticationResult(request.userId(), request.userPw()).authenticated()) {
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        if (!passwordEncoder.matches(request.userPw(), user.getPasswordHash())) {
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        user.updateLastLoginAt(LocalDateTime.now());
        TokenBundle tokenBundle = issueSession(user);

        return new CreateLoginResponse(
                tokenBundle.accessToken(),
                tokenBundle.refreshToken(),
                jwtTokenProvider.getAccessTokenExpirationSeconds()
        );
    }

    @Override
    @Transactional
    public CreateTokenReissueResponse reissueToken(CreateTokenReissueRequest request) {
        String refreshTokenHash = refreshTokenService.createTokenHash(request.refreshToken());
        CardChnAuthSession session = sessionRepository.findByRefreshTokenHashForUpdate(refreshTokenHash)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_TOKEN));

        if (session.getExpiredAt().isBefore(LocalDateTime.now())) {
            sessionRepository.delete(session);
            throw new BusinessException(AuthErrorCode.TOKEN_EXPIRED);
        }

        CardChnAuthUser user = session.getAuthUser();
        validateActiveUser(user);

        saveBlacklistedAccessToken(session.getAccessTokenJti());

        String newJti = UUID.randomUUID().toString();
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getUserUuid(), user.getAuthUserUuid(), newJti);
        String newRefreshToken = refreshTokenService.createRefreshToken();
        session.rotate(
                refreshTokenService.createTokenHash(newRefreshToken),
                newJti,
                refreshTokenService.getRefreshTokenExpiryAt()
        );

        return new CreateTokenReissueResponse(
                newAccessToken,
                newRefreshToken,
                jwtTokenProvider.getAccessTokenExpirationSeconds()
        );
    }

    @Override
    @Transactional
    public void logoutUser(AuthenticatedUser authenticatedUser, DeleteLogoutRequest request) {
        String refreshTokenHash = refreshTokenService.createTokenHash(request.refreshToken());
        CardChnAuthSession session = sessionRepository.findByRefreshTokenHash(refreshTokenHash)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_TOKEN));

        if (!session.getAuthUser().getUserUuid().equals(authenticatedUser.userUuid())) {
            throw new BusinessException(AuthErrorCode.FORBIDDEN);
        }

        sessionRepository.delete(session);
        tokenBlacklistService.saveBlacklistedToken(
                authenticatedUser.jti(),
                jwtTokenProvider.getRemainingValidityMillis(requestAccessTokenHolder.getRequiredToken())
        );
    }

    private void validateActiveUser(CardChnAuthUser user) {
        if (user.getUserStatus() == UserStatus.DEACTIVATE) {
            throw new BusinessException(AuthErrorCode.WITHDRAWN_ACCOUNT);
        }
    }

    private void saveBlacklistedAccessToken(String jti) {
        CardChnAuthSession currentSession = sessionRepository.findByAccessTokenJti(jti).orElse(null);
        if (currentSession == null) {
            return;
        }
        tokenBlacklistService.saveBlacklistedToken(jti, jwtTokenProvider.getAccessTokenExpirationMillis());
    }

    private TokenBundle issueSession(CardChnAuthUser user) {
        String jti = UUID.randomUUID().toString();
        String accessToken = jwtTokenProvider.generateAccessToken(user.getUserUuid(), user.getAuthUserUuid(), jti);
        String refreshToken = refreshTokenService.createRefreshToken();
        String refreshTokenHash = refreshTokenService.createTokenHash(refreshToken);
        LocalDateTime refreshTokenExpiryAt = refreshTokenService.getRefreshTokenExpiryAt();

        sessionRepository.findByAuthUser_AuthUserUuid(user.getAuthUserUuid())
                .ifPresentOrElse(
                        session -> session.rotate(refreshTokenHash, jti, refreshTokenExpiryAt),
                        () -> sessionRepository.save(CardChnAuthSession.builder()
                                .authUser(user)
                                .refreshTokenHash(refreshTokenHash)
                                .accessTokenJti(jti)
                                .expiredAt(refreshTokenExpiryAt)
                                .build())
                );

        return new TokenBundle(accessToken, refreshToken);
    }

    private void initializeCommonUserMapping(CardChnAuthUser user) {
        try {
            ApiResponse<GetMyUserMappingResponse> response = commonUserMappingApi.initializeUserMapping(
                    new InitializeUserMappingRequest(user.getUserUuid())
            );
            if (isInvalidCommonUserMappingResponse(response)) {
                log.warn(
                        "Common server initializeUserMapping invalid response [status={}, code={}, hasData={}]",
                        response == null ? null : response.status(),
                        response == null ? null : response.code(),
                        response != null && response.data() != null
                );
                throw new BusinessException(AuthErrorCode.COMMON_USER_MAPPING_UNAVAILABLE);
            }
        } catch (FeignException e) {
            log.warn(
                    "Common server initializeUserMapping Feign error [status={}, type={}]",
                    e.status(),
                    e.getClass().getSimpleName()
            );
            throw new BusinessException(AuthErrorCode.COMMON_USER_MAPPING_UNAVAILABLE, e);
        }
    }

    private boolean isInvalidCommonUserMappingResponse(ApiResponse<GetMyUserMappingResponse> response) {
        return response == null
                || response.status() < 200
                || response.status() >= 300
                || response.data() == null
                || response.data().userUuid() == null;
    }

    private record TokenBundle(String accessToken, String refreshToken) {
    }
}
