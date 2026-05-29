package com.woorifisa.won_card_channel_server.domain.card.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.woorifisa.won_card_channel_server.domain.auth.exception.code.AuthErrorCode;
import com.woorifisa.won_card_channel_server.domain.auth.model.CardChnAuthUser;
import com.woorifisa.won_card_channel_server.domain.auth.repository.CardChnAuthUserRepository;
import com.woorifisa.won_card_channel_server.domain.auth.model.UserStatus;
import com.woorifisa.won_card_channel_server.domain.autoinvest.dto.response.InvestAccountDetailsResponse;
import com.woorifisa.won_card_channel_server.domain.autoinvest.dto.response.InvestEtfDetailsResponse;
import com.woorifisa.won_card_channel_server.domain.autoinvest.exception.code.AutoInvestErrorCode;
import com.woorifisa.won_card_channel_server.domain.autoinvest.external.InvestChannelAutoInvestApi;
import com.woorifisa.won_card_channel_server.domain.autoinvest.service.InvestAccountResponseValidator;
import com.woorifisa.won_card_channel_server.domain.autoinvest.service.InvestEtfResponseValidator;
import com.woorifisa.won_card_channel_server.domain.autoinvest.service.AutoInvestSubscriptionService;
import com.woorifisa.won_card_channel_server.domain.card.dto.request.CardApplicationCreateRequest;
import com.woorifisa.won_card_channel_server.domain.card.dto.request.CardCoreApplicationRequest;
import com.woorifisa.won_card_channel_server.domain.card.dto.response.CardApplicationCreateResponse;
import com.woorifisa.won_card_channel_server.domain.card.dto.response.CardCoreApplicationResponse;
import com.woorifisa.won_card_channel_server.domain.card.exception.code.CardErrorCode;
import com.woorifisa.won_card_channel_server.domain.card.external.CardCoreCardApplicationApi;
import com.woorifisa.won_card_channel_server.domain.card.model.CardChnCardSummary;
import com.woorifisa.won_card_channel_server.domain.card.repository.CardChnCardSummaryRepository;
import com.woorifisa.won_card_channel_server.domain.user.dto.request.UpdateCardUserMappingRequest;
import com.woorifisa.won_card_channel_server.domain.user.dto.response.GetMyUserMappingResponse;
import com.woorifisa.won_card_channel_server.domain.user.external.CommonUserMappingApi;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import com.woorifisa.won_card_channel_server.global.security.AuthenticatedUser;
import com.woorifisa.won_card_channel_server.global.security.TextEncryptor;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardApplicationService {

    // 와이어프레임 기준 카드명
    private static final String DEFAULT_CARD_NAME = "WON 자동투자 카드";

    private final CardChnAuthUserRepository authUserRepository;
    private final CardChnCardSummaryRepository cardSummaryRepository;
    private final CardCoreCardApplicationApi cardCoreCardApplicationApi;
    private final InvestChannelAutoInvestApi investChannelAutoInvestApi;
    private final AutoInvestSubscriptionService autoInvestSubscriptionService;
    private final CommonUserMappingApi commonUserMappingApi;
    private final InvestEtfResponseValidator investEtfResponseValidator;
    private final TextEncryptor textEncryptor;
    private final ObjectMapper objectMapper;

    @Transactional
    public CardApplicationCreateResponse applyCard(
            AuthenticatedUser authenticatedUser,
            CardApplicationCreateRequest request
    ) {
        UUID userUuid = extractUserUuid(authenticatedUser);
        validateRequest(request);

        // 사용자 확인
        CardChnAuthUser authUser = authUserRepository.findByUserUuid(userUuid)
                .orElseThrow(() -> new BusinessException(CardErrorCode.CARD_USER_NOT_FOUND));

        // 활성 유저만 발급 가능
        if (authUser.getUserStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(CardErrorCode.CARD_ISSUANCE_NOT_ALLOWED);
        }

        // 공통 사용자 검증
        validateInvestMapping(userUuid);
        // 증권 계좌 연동 여부 검증
        validateInvestmentAccount(userUuid, request.invstAccountUuid());

        // 선택 가능한 자동투자 ETF
        InvestEtfDetailsResponse etf = validateEtf(request.etfId(), request.ticker().trim());

        CardCoreApplicationRequest coreRequest = buildCoreApplicationRequest(request);
        CardCoreApplicationResponse cardResponse = requestCardIssuance(
                userUuid,
                coreRequest
        );

        syncCardUserMappingIfPossible(userUuid, authUser);
        saveCardSummaryIfPossible(userUuid, authUser, cardResponse, request.etfId());

        autoInvestSubscriptionService.createInitialSubscription(
                userUuid,
                request.invstAccountUuid(),
                request.etfId(),
                request.ticker().trim()
        );

        return new CardApplicationCreateResponse(
                cardResponse.cardUuid(),
                cardResponse.cardNoDisplay(),
                cardResponse.issuedAt(),
                cardResponse.cardStatus(),
                etf.etfName()
        );
    }

    private UUID extractUserUuid(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.userUuid() == null) {
            throw new BusinessException(AuthErrorCode.AUTHENTICATION_REQUIRED);
        }
        return authenticatedUser.userUuid();
    }

    //  서비스 레벨 검증용 메서드 분리
    private void validateRequest(CardApplicationCreateRequest request) {
        if (request == null || request.applicantInfo() == null || request.requiredTerms() == null) {
            throw new BusinessException(CardErrorCode.CARD_APPLICATION_INVALID_REQUEST);
        }

        if (!Boolean.TRUE.equals(request.requiredTerms())) {
            throw new BusinessException(CardErrorCode.CARD_APPLICATION_TERMS_NOT_AGREED);
        }
    }

    private void validateInvestmentAccount(UUID userUuid, UUID invstAccountUuid) {
        try {

            // 증권 계좌 확인
            ApiResponse<InvestAccountDetailsResponse> response =
                    investChannelAutoInvestApi.getInvestmentAccount(userUuid, invstAccountUuid);
            InvestAccountResponseValidator.validate(userUuid, invstAccountUuid, response);

        } catch (FeignException.NotFound e) {
            throw new BusinessException(AutoInvestErrorCode.INVEST_ACCOUNT_NOT_FOUND, e);
        } catch (FeignException.Forbidden e) {
            throw new BusinessException(AutoInvestErrorCode.INVEST_ACCOUNT_FORBIDDEN, e);
        } catch (FeignException e) {
            throw new BusinessException(AutoInvestErrorCode.INVEST_ACCOUNT_UNAVAILABLE, e);
        }
    }

    private void validateInvestMapping(UUID userUuid) {
        try {
            // 공통계 매핑 여부 확인
            ApiResponse<GetMyUserMappingResponse> response =
                    commonUserMappingApi.getMappingStatus(userUuid);

            GetMyUserMappingResponse data = response == null ? null : response.data();


            if (data == null || data.userUuid() == null || !userUuid.equals(data.userUuid())) {
                throw new BusinessException(CardErrorCode.CARD_MAPPING_RESPONSE_INVALID);
            }
            if (data.cardUserUuid() != null && isCardLinked(data.cardLinkStatus())) {
                throw new BusinessException(CardErrorCode.CARD_ALREADY_EXISTS);
            }
            if (data.investUserUuid() == null || !isInvestLinked(data.investLinkStatus())) {
                throw new BusinessException(CardErrorCode.CARD_INVEST_LINK_REQUIRED);
            }

        } catch (FeignException.NotFound e) {
            throw new BusinessException(CardErrorCode.CARD_INVEST_LINK_REQUIRED, e);
        } catch (FeignException e) {
            throw new BusinessException(CardErrorCode.CARD_MAPPING_UNAVAILABLE, e);
        }
    }

    private boolean isInvestLinked(String investLinkStatus) {
        return investLinkStatus != null
                && ("LINKED".equalsIgnoreCase(investLinkStatus)
                || "ACTIVE".equalsIgnoreCase(investLinkStatus));
    }

    private boolean isCardLinked(String cardLinkStatus) {
        return cardLinkStatus != null
                && ("LINKED".equalsIgnoreCase(cardLinkStatus)
                || "ACTIVE".equalsIgnoreCase(cardLinkStatus));
    }

    private CardCoreApplicationRequest buildCoreApplicationRequest(CardApplicationCreateRequest request) {
        return CardCoreApplicationRequest.from(
                request,
                encryptApplicantField(request.applicantInfo().nameKo()),
                encryptApplicantField(request.applicantInfo().birthDate()),
                encryptApplicantField(request.applicantInfo().phoneNumber()),
                encryptApplicantField(request.applicantInfo().email()),
                encryptApplicantField(request.applicantInfo().address())
        );
    }

    private String encryptApplicantField(String value) {
        return textEncryptor.encrypt(value.trim());
    }

    private void syncCardUserMappingIfPossible(UUID userUuid, CardChnAuthUser authUser) {

        if (authUser.getCardUserUuid() == null) {
            // TODO: 카드 코어 쪽 수정 후 반영 필요
            return;
        }

        try {
            commonUserMappingApi.updateCardUserMapping(
                    userUuid,
                    new UpdateCardUserMappingRequest(authUser.getCardUserUuid())
            );

        } catch (FeignException e) {
            throw new BusinessException(CardErrorCode.CARD_MAPPING_UNAVAILABLE, e);
        }
    }

    private InvestEtfDetailsResponse validateEtf(Long etfId, String ticker) {
        try {
            // 증권 채널계 자동 투자 가능 ETF 목록
            ApiResponse<InvestEtfDetailsResponse> response = investChannelAutoInvestApi.getEtf(etfId);
            return investEtfResponseValidator.validateForAutoInvest(etfId, ticker, response);

        } catch (FeignException.NotFound e) {
            throw new BusinessException(AutoInvestErrorCode.ETF_NOT_FOUND, e);
        } catch (FeignException e) {
            throw new BusinessException(AutoInvestErrorCode.ETF_UNAVAILABLE, e);
        }
    }

    private CardCoreApplicationResponse requestCardIssuance(UUID userUuid, CardCoreApplicationRequest request) {
        try {
            ApiResponse<CardCoreApplicationResponse> response = cardCoreCardApplicationApi.applyCard(userUuid, request);
            return extractCardApplicationData(response);
        } catch (FeignException e) {
            throw mapCardCoreApplicationException(e);
        }
    }

    private CardCoreApplicationResponse extractCardApplicationData(
            ApiResponse<CardCoreApplicationResponse> coreResponse
    ) {
        if (coreResponse == null || coreResponse.data() == null || coreResponse.data().cardUuid() == null) {
            throw new BusinessException(CardErrorCode.INVALID_CARD_RESPONSE);
        }

        return coreResponse.data();
    }

    // 카드 코어에서 받은 도메인 에러 코드 파싱용
    private BusinessException mapCardCoreApplicationException(FeignException e) {
        String errorCode = extractErrorCode(e);
        if (errorCode != null) {
            return switch (errorCode) {
                case "CARD_400_001" -> new BusinessException(CardErrorCode.CARD_APPLICATION_INVALID_REQUEST, e);
                case "CARD_400_002" -> new BusinessException(CardErrorCode.CARD_APPLICATION_TERMS_NOT_AGREED, e);
                case "CARD_409_001" -> new BusinessException(CardErrorCode.CARD_ALREADY_EXISTS, e);
                case "CARD_409_002" -> new BusinessException(CardErrorCode.CARD_USER_ALREADY_EXISTS, e);
                case "CARD_409_003", "CARD_409_004", "CARD_409_005" -> new BusinessException(CardErrorCode.CARD_CONSTRAINT_CONFLICT, e);
                case "CARD_422_001" -> new BusinessException(CardErrorCode.CARD_ISSUANCE_NOT_ALLOWED, e);
                default -> new BusinessException(CardErrorCode.CARD_APPLICATION_UNAVAILABLE, e);
            };
        }
        if (e.status() == HttpStatus.CONFLICT.value()) {
            return new BusinessException(CardErrorCode.CARD_CONSTRAINT_CONFLICT, e);
        }
        if (e.status() == HttpStatus.UNPROCESSABLE_ENTITY.value()) {
            return new BusinessException(CardErrorCode.CARD_ISSUANCE_NOT_ALLOWED, e);
        }
        if (e.status() == HttpStatus.BAD_REQUEST.value()) {
            return new BusinessException(CardErrorCode.CARD_APPLICATION_INVALID_REQUEST, e);
        }
        return new BusinessException(CardErrorCode.CARD_APPLICATION_UNAVAILABLE, e);
    }

    private String extractErrorCode(FeignException e) {
        try {
            String content = e.contentUTF8();
            if (content == null || content.isBlank()) {
                return null;
            }
            JsonNode node = objectMapper.readTree(content);
            JsonNode codeNode = node.get("code");
            return codeNode == null || codeNode.isNull() ? null : codeNode.asText();
        } catch (Exception ignored) {
            return null;
        }
    }

    private void saveCardSummaryIfPossible(
            UUID userUuid,
            CardChnAuthUser authUser,
            CardCoreApplicationResponse cardResponse,
            Long selectedEtfId
    ) {
        if (authUser.getCardUserUuid() == null) {
            // TODO: 카드 코어 쪽 수정 후 반영 필요
            return;
        }

        CardChnCardSummary summary = CardChnCardSummary.builder()
                .userUuid(userUuid)
                .cardUserUuid(authUser.getCardUserUuid())
                .cardUuid(cardResponse.cardUuid())
                .cardName(DEFAULT_CARD_NAME)
                .cardNoDisplay(cardResponse.cardNoDisplay())
                .cardStatus(cardResponse.cardStatus())
                .currentMonthUsageAmount(BigDecimal.ZERO)
                .lastSyncedAt(LocalDateTime.now())
                .selectedEtfId(selectedEtfId)
                .build();

        try {
            cardSummaryRepository.save(summary);
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateConstraintViolation(e)) {
                log.info("Skip duplicate card summary projection for userUuid={}, cardUuid={}", userUuid, cardResponse.cardUuid());
                return;
            }
            log.error("Failed to save card summary projection for userUuid={}, cardUuid={}", userUuid, cardResponse.cardUuid(), e);
            throw e;
        }
    }

    private boolean isDuplicateConstraintViolation(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause() == null ? e.getMessage() : e.getMostSpecificCause().getMessage();
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase();
        return normalized.contains("duplicate")
                || normalized.contains("unique")
                || normalized.contains("uk_card_summary");
    }
}
