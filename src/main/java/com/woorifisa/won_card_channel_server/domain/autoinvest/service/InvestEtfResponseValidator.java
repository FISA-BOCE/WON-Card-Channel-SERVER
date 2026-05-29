package com.woorifisa.won_card_channel_server.domain.autoinvest.service;

import com.woorifisa.won_card_channel_server.domain.autoinvest.dto.response.InvestEtfDetailsResponse;
import com.woorifisa.won_card_channel_server.domain.autoinvest.exception.code.AutoInvestErrorCode;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvestEtfResponseValidator {

    private final Validator validator;

    public InvestEtfDetailsResponse validateBasic(Long etfId, ApiResponse<InvestEtfDetailsResponse> response) {
        InvestEtfDetailsResponse data = response == null ? null : response.data();
        if (data == null) {
            throw new BusinessException(AutoInvestErrorCode.ETF_RESPONSE_INVALID);
        }

        Set<ConstraintViolation<InvestEtfDetailsResponse>> violations = validator.validate(data);
        if (!violations.isEmpty() || !etfId.equals(data.etfId())) {
            throw new BusinessException(AutoInvestErrorCode.ETF_RESPONSE_INVALID);
        }
        return data;
    }

    public InvestEtfDetailsResponse validateSelectable(Long etfId, ApiResponse<InvestEtfDetailsResponse> response) {
        InvestEtfDetailsResponse data = validateBasic(etfId, response);
        if (!Boolean.TRUE.equals(data.isTradeAvailable())) {
            throw new BusinessException(AutoInvestErrorCode.ETF_NOT_TRADABLE);
        }
        if (!Boolean.TRUE.equals(data.isFractionalAvailable())) {
            throw new BusinessException(AutoInvestErrorCode.ETF_FRACTIONAL_BUY_NOT_ALLOWED);
        }
        return data;
    }

    public InvestEtfDetailsResponse validateForAutoInvest(Long etfId, String ticker, ApiResponse<InvestEtfDetailsResponse> response) {
        InvestEtfDetailsResponse data = validateSelectable(etfId, response);
        if (!data.ticker().equalsIgnoreCase(ticker)) {
            throw new BusinessException(AutoInvestErrorCode.ETF_TICKER_MISMATCH);
        }
        return data;
    }
}
