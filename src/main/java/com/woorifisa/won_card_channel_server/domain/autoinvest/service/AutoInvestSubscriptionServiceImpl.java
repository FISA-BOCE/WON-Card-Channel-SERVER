package com.woorifisa.won_card_channel_server.domain.autoinvest.service;

import com.woorifisa.won_card_channel_server.domain.auth.exception.code.AuthErrorCode;
import com.woorifisa.won_card_channel_server.domain.autoinvest.dto.request.AutoInvestSubscriptionChangeRequest;
import com.woorifisa.won_card_channel_server.domain.autoinvest.dto.response.AutoInvestSubscriptionChangeResponse;
import com.woorifisa.won_card_channel_server.domain.autoinvest.dto.response.AutoInvestSubscriptionDetailResponse;
import com.woorifisa.won_card_channel_server.domain.autoinvest.dto.response.InvestAccountDetailsResponse;
import com.woorifisa.won_card_channel_server.domain.autoinvest.dto.response.InvestEtfDetailsResponse;
import com.woorifisa.won_card_channel_server.domain.autoinvest.exception.code.AutoInvestErrorCode;
import com.woorifisa.won_card_channel_server.domain.autoinvest.external.InvestChannelAutoInvestApi;
import com.woorifisa.won_card_channel_server.domain.card.model.CardChnCardSummary;
import com.woorifisa.won_card_channel_server.domain.card.repository.CardChnCardSummaryRepository;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import com.woorifisa.won_card_channel_server.global.security.AuthenticatedUser;
import feign.FeignException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Slf4j
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AutoInvestSubscriptionServiceImpl implements AutoInvestSubscriptionService {

    private final CardChnCardSummaryRepository cardSummaryRepository;
    private final InvestChannelAutoInvestApi investChannelAutoInvestApi;
    private final InvestEtfResponseValidator investEtfResponseValidator;
    private final Validator validator;

    @Override
    @Transactional
    public void createInitialSubscription(
            UUID userUuid,
            UUID investAccountUuid,
            Long etfId,
            String ticker
    ) {
        validateInvestmentAccount(userUuid, investAccountUuid);
        InvestEtfDetailsResponse etf = validateEtf(etfId, ticker.trim());

        cardSummaryRepository.findByUserUuid(userUuid)
                .ifPresent(summary -> summary.updateAutoInvestSelection(etf.etfId(), LocalDateTime.now()));
    }

    @Override
    public AutoInvestSubscriptionDetailResponse getSubscription(AuthenticatedUser authenticatedUser, UUID cardUuid) {
        UUID userUuid = extractUserUuid(authenticatedUser);
        CardChnCardSummary summary = findRequiredCardSummary(userUuid);
        validateCardOwner(cardUuid, summary);

        Long selectedEtfId = summary.getSelectedEtfId();
        if (selectedEtfId == null) {
            throw new BusinessException(AutoInvestErrorCode.AUTO_INVEST_CHANGE_TARGET_NOT_FOUND);
        }

        InvestEtfDetailsResponse etf = loadEtfDetails(selectedEtfId);
        return new AutoInvestSubscriptionDetailResponse(
                summary.getCardUuid(),
                new AutoInvestSubscriptionDetailResponse.CurrentEtf(
                        etf.etfId(),
                        etf.etfName(),
                        etf.ticker(),
                        summary.getLastSyncedAt()
                ),
                true
        );
    }

    @Override
    @Transactional
    public AutoInvestSubscriptionChangeResponse changeSubscription(
            AuthenticatedUser authenticatedUser,
            UUID cardUuid,
            AutoInvestSubscriptionChangeRequest request
    ) {
        UUID userUuid = extractUserUuid(authenticatedUser);
        CardChnCardSummary summary = findRequiredCardSummary(userUuid);
        validateCardOwner(cardUuid, summary);

        Long currentEtfId = summary.getSelectedEtfId();
        if (currentEtfId == null) {
            throw new BusinessException(AutoInvestErrorCode.AUTO_INVEST_CHANGE_TARGET_NOT_FOUND);
        }
        if (currentEtfId.equals(request.etfId())) {
            throw new BusinessException(AutoInvestErrorCode.AUTO_INVEST_SAME_ETF);
        }

        InvestEtfDetailsResponse previousEtf = loadEtfDetails(currentEtfId);
        InvestEtfDetailsResponse newEtf = loadAutoInvestSelectableEtf(request.etfId());
        LocalDateTime changedAt = LocalDateTime.now();
        summary.updateAutoInvestSelection(newEtf.etfId(), changedAt);

        return new AutoInvestSubscriptionChangeResponse(
                summary.getCardUuid(),
                new AutoInvestSubscriptionChangeResponse.PreviousEtf(
                        previousEtf.etfName(),
                        previousEtf.ticker(),
                        changedAt
                ),
                new AutoInvestSubscriptionChangeResponse.NewEtf(
                        newEtf.etfId(),
                        newEtf.etfName(),
                        newEtf.ticker(),
                        nextEffectiveFrom(changedAt)
                )
        );
    }

    private InvestAccountDetailsResponse validateInvestmentAccount(UUID userUuid, UUID investAccountUuid) {
        try {
            ApiResponse<InvestAccountDetailsResponse> response =
                    investChannelAutoInvestApi.getInvestmentAccount(userUuid, investAccountUuid);
            return InvestAccountResponseValidator.validate(userUuid, investAccountUuid, response);
        } catch (FeignException.NotFound e) {
            throw new BusinessException(AutoInvestErrorCode.INVEST_ACCOUNT_NOT_FOUND, e);
        } catch (FeignException.Forbidden e) {
            throw new BusinessException(AutoInvestErrorCode.INVEST_ACCOUNT_FORBIDDEN, e);
        } catch (FeignException e) {
            throw new BusinessException(AutoInvestErrorCode.INVEST_ACCOUNT_UNAVAILABLE, e);
        }
    }

    private InvestEtfDetailsResponse validateEtf(Long etfId, String ticker) {
        try {
            ApiResponse<InvestEtfDetailsResponse> response = investChannelAutoInvestApi.getEtf(etfId);
            return investEtfResponseValidator.validateForAutoInvest(etfId, ticker, response);
        } catch (FeignException.NotFound e) {
            throw new BusinessException(AutoInvestErrorCode.ETF_NOT_FOUND, e);
        } catch (FeignException e) {
            throw new BusinessException(AutoInvestErrorCode.ETF_UNAVAILABLE, e);
        }
    }

    private UUID extractUserUuid(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.userUuid() == null) {
            throw new BusinessException(AuthErrorCode.AUTHENTICATION_REQUIRED);
        }
        return authenticatedUser.userUuid();
    }

    private CardChnCardSummary findRequiredCardSummary(UUID userUuid) {
        return cardSummaryRepository.findByUserUuid(userUuid)
                .orElseThrow(() -> new BusinessException(AutoInvestErrorCode.AUTO_INVEST_CHANGE_TARGET_NOT_FOUND));
    }

    private void validateCardOwner(UUID cardUuid, CardChnCardSummary summary) {
        if (cardUuid == null || summary.getCardUuid() == null || !summary.getCardUuid().equals(cardUuid)) {
            throw new BusinessException(AutoInvestErrorCode.AUTO_INVEST_NOT_OWNER);
        }
    }

    private InvestEtfDetailsResponse loadEtfDetails(Long etfId) {
        try {
            ApiResponse<InvestEtfDetailsResponse> response = investChannelAutoInvestApi.getEtf(etfId);
            InvestEtfDetailsResponse data = response == null ? null : response.data();
            if (data == null) {
                throw new BusinessException(AutoInvestErrorCode.ETF_RESPONSE_INVALID);
            }

            Set<ConstraintViolation<InvestEtfDetailsResponse>> violations = validator.validate(data);
            if (!violations.isEmpty() || !etfId.equals(data.etfId())) {
                throw new BusinessException(AutoInvestErrorCode.ETF_RESPONSE_INVALID);
            }
            return data;
        } catch (FeignException.NotFound e) {
            throw new BusinessException(AutoInvestErrorCode.ETF_NOT_FOUND, e);
        } catch (FeignException e) {
            throw new BusinessException(AutoInvestErrorCode.ETF_UNAVAILABLE, e);
        }
    }

    private InvestEtfDetailsResponse loadAutoInvestSelectableEtf(Long etfId) {
        InvestEtfDetailsResponse data = loadEtfDetails(etfId);
        if (!Boolean.TRUE.equals(data.isTradeAvailable())) {
            throw new BusinessException(AutoInvestErrorCode.ETF_NOT_TRADABLE);
        }
        if (!Boolean.TRUE.equals(data.isFractionalAvailable())) {
            throw new BusinessException(AutoInvestErrorCode.ETF_FRACTIONAL_BUY_NOT_ALLOWED);
        }
        return data;
    }

    private LocalDateTime nextEffectiveFrom(LocalDateTime changedAt) {
        return changedAt.plusMonths(1)
                .with(TemporalAdjusters.firstDayOfMonth())
                .toLocalDate()
                .atStartOfDay();
    }
}
