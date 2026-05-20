package com.woorifisa.won_card_channel_server.domain.user.service.impl;

import com.woorifisa.won_card_channel_server.domain.auth.model.CardChnAuthSession;
import com.woorifisa.won_card_channel_server.domain.auth.model.CardChnAuthUser;
import com.woorifisa.won_card_channel_server.domain.auth.model.UserStatus;
import com.woorifisa.won_card_channel_server.domain.auth.exception.code.AuthErrorCode;
import com.woorifisa.won_card_channel_server.domain.auth.repository.CardChnAuthSessionRepository;
import com.woorifisa.won_card_channel_server.domain.auth.repository.CardChnAuthUserRepository;
import com.woorifisa.won_card_channel_server.domain.auth.service.RefreshTokenService;
import com.woorifisa.won_card_channel_server.domain.auth.service.TokenBlacklistService;
import com.woorifisa.won_card_channel_server.domain.user.dto.request.DeleteUserRequest;
import com.woorifisa.won_card_channel_server.domain.user.dto.response.GetMyUserResponse;
import com.woorifisa.won_card_channel_server.domain.user.dto.request.UpdateUserRequest;
import com.woorifisa.won_card_channel_server.global.exception.code.CommonErrorCode;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import com.woorifisa.won_card_channel_server.global.security.AuthenticatedUser;
import com.woorifisa.won_card_channel_server.global.security.JwtTokenProvider;
import com.woorifisa.won_card_channel_server.global.security.RequestAccessTokenHolder;
import com.woorifisa.won_card_channel_server.global.security.TextEncryptor;
import com.woorifisa.won_card_channel_server.global.util.MaskingUtils;
import com.woorifisa.won_card_channel_server.global.util.ValidationUtils;
import com.woorifisa.won_card_channel_server.domain.user.service.AutoInvestService;
import com.woorifisa.won_card_channel_server.domain.user.service.CardProductService;
import com.woorifisa.won_card_channel_server.domain.user.service.UserService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final CardChnAuthUserRepository userRepository;
    private final CardChnAuthSessionRepository sessionRepository;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RequestAccessTokenHolder requestAccessTokenHolder;
    private final TextEncryptor textEncryptor;
    private final PasswordEncoder passwordEncoder;
    private final CardProductService cardProductService;
    private final AutoInvestService autoInvestService;

    @Override
    @Transactional(readOnly = true)
    public GetMyUserResponse getMyUser(AuthenticatedUser authenticatedUser) {
        CardChnAuthUser user = getActiveUser(authenticatedUser.userUuid());
        String decryptedTel = user.getTelEnc() == null ? "" : textEncryptor.decryptIfPossible(user.getTelEnc());
        return new GetMyUserResponse(
                user.getUserName(),
                MaskingUtils.maskPhoneNumber(decryptedTel),
                user.getCreatedAt() == null ? "" : user.getCreatedAt().toLocalDate().format(DateTimeFormatter.ISO_DATE)
        );
    }

    @Override
    @Transactional
    public void withdrawUser(AuthenticatedUser authenticatedUser, DeleteUserRequest request) {
        CardChnAuthUser user = userRepository.findByUserUuid(authenticatedUser.userUuid())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "사용자 정보를 찾을 수 없습니다."));

        if (user.getUserStatus() == UserStatus.DEACTIVATE) {
            throw new BusinessException(AuthErrorCode.ALREADY_WITHDRAWN);
        }

        cardProductService.terminateProducts(user);
        autoInvestService.disableAutoInvest(user);

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
        user.withdraw(LocalDateTime.now());
        // TODO(auth): 재가입 제한 30일 정책 확정 후 실제 구현으로 교체
    }

    @Override
    @Transactional
    public void updateUser(AuthenticatedUser authenticatedUser, UpdateUserRequest request) {
        boolean hasEmail = !ValidationUtils.isBlank(request.email());
        boolean hasCurrentPw = !ValidationUtils.isBlank(request.currentPw());
        boolean hasNewPw = !ValidationUtils.isBlank(request.newPw());

        CardChnAuthUser user = getActiveUser(authenticatedUser.userUuid());

        if (hasEmail) {
            user.updateEmailEnc(textEncryptor.encrypt(request.email().trim()));
        }

        if (hasCurrentPw) {
            if (!passwordEncoder.matches(request.currentPw(), user.getPasswordHash())) {
                throw new BusinessException(AuthErrorCode.INVALID_CURRENT_PASSWORD);
            }
            user.changePassword(passwordEncoder.encode(request.newPw()));
        }
    }

    private CardChnAuthUser getActiveUser(UUID userUuid) {
        CardChnAuthUser user = userRepository.findByUserUuid(userUuid)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "사용자 정보를 찾을 수 없습니다."));
        if (user.getUserStatus() == UserStatus.DEACTIVATE) {
            throw new BusinessException(AuthErrorCode.WITHDRAWN_ACCOUNT);
        }
        return user;
    }
}
