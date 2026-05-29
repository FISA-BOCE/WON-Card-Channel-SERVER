package com.woorifisa.won_card_channel_server.domain.autoinvest.service;

import com.woorifisa.won_card_channel_server.domain.autoinvest.dto.response.InvestAccountDetailsResponse;
import com.woorifisa.won_card_channel_server.domain.autoinvest.exception.code.AutoInvestErrorCode;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;

import java.util.UUID;

public final class InvestAccountResponseValidator {

    private static final String ACTIVE_ACCOUNT_STATUS = "ACTIVE";

    private InvestAccountResponseValidator() {
    }

    // 기본 응답 겁증과 ticker 일치, 거래 가능 여부, 소수점 가능 여부 검증
    public static InvestAccountDetailsResponse validate(UUID invstAccountUuid, ApiResponse<InvestAccountDetailsResponse> response) {
        InvestAccountDetailsResponse data = response == null ? null : response.data();

        if (data == null
                || data.invstAccountUuid() == null
                || data.accountStatus() == null
                || !invstAccountUuid.equals(data.invstAccountUuid())) {
            throw new BusinessException(AutoInvestErrorCode.INVEST_ACCOUNT_RESPONSE_INVALID);
        }

        if (!ACTIVE_ACCOUNT_STATUS.equalsIgnoreCase(data.accountStatus())) {
            throw new BusinessException(AutoInvestErrorCode.INVEST_ACCOUNT_INVALID_STATUS);
        }
        return data;
    }
}
