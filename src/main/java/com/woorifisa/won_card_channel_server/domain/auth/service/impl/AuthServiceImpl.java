package com.woorifisa.won_card_channel_server.domain.auth.service.impl;

import com.woorifisa.won_card_channel_server.domain.auth.dto.CreateLoginRequest;
import com.woorifisa.won_card_channel_server.domain.auth.dto.CreateLoginResponse;
import com.woorifisa.won_card_channel_server.domain.auth.dto.CreateTokenReissueRequest;
import com.woorifisa.won_card_channel_server.domain.auth.dto.CreateTokenReissueResponse;
import com.woorifisa.won_card_channel_server.domain.auth.dto.DeleteLogoutRequest;
import com.woorifisa.won_card_channel_server.domain.auth.dto.RegisterUserRequest;
import com.woorifisa.won_card_channel_server.domain.auth.dto.RegisterUserResponse;
import com.woorifisa.won_card_channel_server.domain.auth.model.CardChnAuthSession;
import com.woorifisa.won_card_channel_server.domain.auth.model.CardChnAuthUser;
import com.woorifisa.won_card_channel_server.domain.auth.model.UserStatus;
import com.woorifisa.won_card_channel_server.domain.auth.exception.code.AuthErrorCode;
import com.woorifisa.won_card_channel_server.domain.auth.repository.CardChnAuthSessionRepository;
import com.woorifisa.won_card_channel_server.domain.auth.repository.CardChnAuthUserRepository;
import com.woorifisa.won_card_channel_server.domain.auth.service.AuthService;
import com.woorifisa.won_card_channel_server.domain.auth.service.LedgerAuthClient;
import com.woorifisa.won_card_channel_server.domain.auth.service.RefreshTokenService;
import com.woorifisa.won_card_channel_server.domain.auth.service.TokenBlacklistService;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import com.woorifisa.won_card_channel_server.global.security.AuthenticatedUser;
import com.woorifisa.won_card_channel_server.global.security.JwtTokenProvider;
import com.woorifisa.won_card_channel_server.global.security.TextEncryptor;
import com.woorifisa.won_card_channel_server.global.util.HashUtils;
import com.woorifisa.won_card_channel_server.global.util.ValidationUtils;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final CardChnAuthUserRepository userRepository;
    private final CardChnAuthSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final LedgerAuthClient ledgerAuthClient;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TextEncryptor textEncryptor;

    @Override
    @Transactional
    public RegisterUserResponse registerUser(RegisterUserRequest request) {
        validateRegisterUserRequest(request);

        String phoneNumber = request.phoneNumber();
        String telHash = HashUtils.sha256(phoneNumber);
        if (userRepository.findByTelHash(telHash).isPresent()) {
            throw new BusinessException(AuthErrorCode.DUPLICATE_PHONE_NUMBER);
        }

        CardChnAuthUser user = CardChnAuthUser.builder()
                .authUserUuid(UUID.randomUUID().toString())
                .cardUserUuid(UUID.randomUUID().toString())
                .userUuid(UUID.randomUUID().toString())
                .loginId(telHash)
                .userName(request.userName().trim())
                .emailEnc(textEncryptor.encrypt(request.email().trim()))
                .telEnc(textEncryptor.encrypt(phoneNumber))
                .telHash(telHash)
                .passwordHash(passwordEncoder.encode(request.password()))
                .userStatus(UserStatus.ACTIVE)
                .build();

        userRepository.save(user);
        return new RegisterUserResponse();
    }

    @Override
    @Transactional
    public CreateLoginResponse authenticateUser(CreateLoginRequest request) {
        validateCreateLoginRequest(request);

        String telHash = HashUtils.sha256(request.userId());
        CardChnAuthUser user = userRepository.findByTelHashForUpdate(telHash)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_CREDENTIALS));

        validateActiveUser(user);

        if (!ledgerAuthClient.fetchAuthenticationResult(request.userId(), request.userPw()).authenticated()) {
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
        if (request == null || ValidationUtils.isBlank(request.refreshToken())) {
            throw new BusinessException(AuthErrorCode.INVALID_INPUT);
        }

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
        if (request == null || ValidationUtils.isBlank(request.refreshToken())) {
            throw new BusinessException(AuthErrorCode.INVALID_INPUT);
        }

        String refreshTokenHash = refreshTokenService.createTokenHash(request.refreshToken());
        CardChnAuthSession session = sessionRepository.findByRefreshTokenHash(refreshTokenHash)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.TOKEN_EXPIRED, "이미 만료되었거나 유효하지 않은 토큰입니다."));

        if (!session.getAuthUser().getUserUuid().equals(authenticatedUser.userUuid())) {
            throw new BusinessException(AuthErrorCode.FORBIDDEN);
        }

        sessionRepository.delete(session);
        tokenBlacklistService.saveBlacklistedToken(authenticatedUser.jti(), jwtTokenProvider.getRemainingValidityMillis(authenticatedUser.token()));
    }

    private void validateCreateLoginRequest(CreateLoginRequest request) {
        if (request == null
                || ValidationUtils.isBlank(request.userId())
                || ValidationUtils.isBlank(request.userPw())
                || !ValidationUtils.isValidUserId(request.userId())
                || !ValidationUtils.isValidPassword(request.userPw())) {
            throw new BusinessException(AuthErrorCode.INVALID_INPUT);
        }
    }

    private void validateRegisterUserRequest(RegisterUserRequest request) {
        if (request == null
                || !ValidationUtils.isValidUserId(request.phoneNumber())
                || ValidationUtils.isBlank(request.userName())
                || request.userName().trim().length() > 100
                || !ValidationUtils.isValidPassword(request.password())
                || !ValidationUtils.isValidEmail(request.email())
                || request.termsAgreed() == null
                || !request.termsAgreed()
                || !request.password().equals(request.passwordConfirm())) {
            throw new BusinessException(AuthErrorCode.INVALID_INPUT);
        }
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
        sessionRepository.findByAuthUser_AuthUserUuid(user.getAuthUserUuid())
                .ifPresent(sessionRepository::delete);

        String jti = UUID.randomUUID().toString();
        String accessToken = jwtTokenProvider.generateAccessToken(user.getUserUuid(), user.getAuthUserUuid(), jti);
        String refreshToken = refreshTokenService.createRefreshToken();

        CardChnAuthSession session = CardChnAuthSession.builder()
                .authUser(user)
                .refreshTokenHash(refreshTokenService.createTokenHash(refreshToken))
                .accessTokenJti(jti)
                .expiredAt(refreshTokenService.getRefreshTokenExpiryAt())
                .build();
        sessionRepository.save(session);

        return new TokenBundle(accessToken, refreshToken);
    }

    private record TokenBundle(String accessToken, String refreshToken) {
    }
}
