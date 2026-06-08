package com.woorifisa.won_card_channel_server.domain.card.service;

import com.woorifisa.won_card_channel_server.domain.auth.exception.code.AuthErrorCode;
import com.woorifisa.won_card_channel_server.domain.card.dto.response.CardApplicationInvestAccountsResponse;
import com.woorifisa.won_card_channel_server.domain.card.dto.response.InvestAccountListResponse;
import com.woorifisa.won_card_channel_server.domain.card.external.InvestChannelInvestAccountApi;
import com.woorifisa.won_card_channel_server.domain.user.dto.response.GetMyUserMappingResponse;
import com.woorifisa.won_card_channel_server.domain.user.external.CommonUserMappingApi;
import com.woorifisa.won_card_channel_server.global.exception.code.CommonErrorCode;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import com.woorifisa.won_card_channel_server.global.security.AuthenticatedUser;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardApplicationInvestAccountService {

    private static final String ACTIVE_ACCOUNT_STATUS = "ACTIVE";

    private final CommonUserMappingApi commonUserMappingApi;
    private final InvestChannelInvestAccountApi investChannelInvestAccountApi;

    public CardApplicationInvestAccountsResponse getInvestAccounts(AuthenticatedUser authenticatedUser) {
        UUID userUuid = extractUserUuid(authenticatedUser);
        validateInvestMapping(userUuid);

        ApiResponse<InvestAccountListResponse> response = fetchInvestAccounts(userUuid);
        InvestAccountListResponse data = response.data();

        if (data == null || data.accounts() == null) {
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
        }

        List<CardApplicationInvestAccountsResponse.Account> accounts = data.accounts().stream()
                .filter(this::isActiveAccount)
                .map(account -> new CardApplicationInvestAccountsResponse.Account(
                        account.investAccountUuid(),
                        account.accountNoDisplay(),
                        true
                ))
                .toList();

        return new CardApplicationInvestAccountsResponse(accounts);
    }

    private UUID extractUserUuid(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.userUuid() == null) {
            throw new BusinessException(AuthErrorCode.AUTHENTICATION_REQUIRED);
        }
        return authenticatedUser.userUuid();
    }

    private void validateInvestMapping(UUID userUuid) {
        try {
            ApiResponse<GetMyUserMappingResponse> response = commonUserMappingApi.getMappingStatus(userUuid);
            GetMyUserMappingResponse data = response == null ? null : response.data();

            if (data == null
                    || data.userUuid() == null
                    || !userUuid.equals(data.userUuid())
                    || data.invest() == null
                    || data.invest().investUserUuid() == null
                    || !Boolean.TRUE.equals(data.invest().isConnected())) {
                throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND);
            }
        } catch (FeignException.NotFound e) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, e);
        } catch (FeignException e) {
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }

    private ApiResponse<InvestAccountListResponse> fetchInvestAccounts(UUID userUuid) {
        try {
            ApiResponse<InvestAccountListResponse> response = investChannelInvestAccountApi.getInvestAccounts(userUuid);
            if (response == null) {
                throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
            }
            return response;
        } catch (FeignException e) {
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }

    private boolean isActiveAccount(InvestAccountListResponse.Account account) {
        return account != null && ACTIVE_ACCOUNT_STATUS.equalsIgnoreCase(account.accountStatus());
    }
}
