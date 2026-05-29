package com.woorifisa.won_card_channel_server.domain.autoinvest.service;

import com.woorifisa.won_card_channel_server.domain.autoinvest.dto.response.InvestAccountDetailsResponse;
import com.woorifisa.won_card_channel_server.domain.autoinvest.dto.response.InvestEtfDetailsResponse;
import com.woorifisa.won_card_channel_server.domain.autoinvest.exception.code.AutoInvestErrorCode;
import com.woorifisa.won_card_channel_server.domain.autoinvest.external.InvestChannelAutoInvestApi;
import com.woorifisa.won_card_channel_server.domain.card.repository.CardChnCardSummaryRepository;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import feign.FeignException;
import java.time.LocalDateTime;
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

    @Override
    @Transactional
    public void createInitialSubscription(
            UUID userUuid,
            UUID invstAccountUuid,
            Long etfId,
            String ticker
    ) {
        validateInvestmentAccount(userUuid, invstAccountUuid);
        InvestEtfDetailsResponse etf = validateEtf(etfId, ticker.trim());

        cardSummaryRepository.findByUserUuid(userUuid)
                .ifPresent(summary -> summary.updateAutoInvestSelection(etf.etfId(), LocalDateTime.now()));
    }

    private InvestAccountDetailsResponse validateInvestmentAccount(UUID userUuid, UUID invstAccountUuid) {
        try {
            ApiResponse<InvestAccountDetailsResponse> response =
                    investChannelAutoInvestApi.getInvestmentAccount(userUuid, invstAccountUuid);
            return InvestAccountResponseValidator.validate(userUuid, invstAccountUuid, response);
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
}
