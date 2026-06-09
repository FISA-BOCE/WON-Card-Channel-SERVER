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
import com.woorifisa.won_card_channel_server.global.response.SuccessStatus;
import com.woorifisa.won_card_channel_server.global.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class CardApplicationInvestAccountServiceTest {

    private final UUID authUserUuid = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID userUuid = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private final UUID investUserUuid = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private final UUID activeAccountUuid = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private final UUID suspendedAccountUuid = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private CommonUserMappingApi commonUserMappingApi;
    private InvestChannelInvestAccountApi investChannelInvestAccountApi;
    private CardApplicationInvestAccountService service;

    @BeforeEach
    void setUp() {
        commonUserMappingApi = mock(CommonUserMappingApi.class);
        investChannelInvestAccountApi = mock(InvestChannelInvestAccountApi.class);
        service = new CardApplicationInvestAccountService(commonUserMappingApi, investChannelInvestAccountApi);
    }

    @Test
    @DisplayName("매핑된 증권 고객 정보가 없으면 404를 반환한다")
    void getInvestAccountsWithoutLinkedInvestMapping() {
        given(commonUserMappingApi.getMappingStatus(userUuid))
                .willReturn(ApiResponse.of(
                        SuccessStatus.OK,
                        new GetMyUserMappingResponse(
                                userUuid,
                                null,
                                null
                        )
                ));

        assertThatThrownBy(() -> service.getInvestAccounts(authenticatedUser()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND);

        verify(investChannelInvestAccountApi, never()).getInvestAccounts(userUuid);
    }

    @Test
    @DisplayName("ACTIVE 계좌만 카드 신청용 응답으로 반환한다")
    void getInvestAccountsReturnsActiveAccountsOnly() {
        given(commonUserMappingApi.getMappingStatus(userUuid))
                .willReturn(ApiResponse.of(SuccessStatus.OK, linkedInvestMappingResponse()));
        given(investChannelInvestAccountApi.getInvestAccounts(userUuid))
                .willReturn(ApiResponse.of(
                        SuccessStatus.OK,
                        new InvestAccountListResponse(List.of(
                                new InvestAccountListResponse.Account(activeAccountUuid, "123-***-***456", "ACTIVE"),
                                new InvestAccountListResponse.Account(suspendedAccountUuid, "999-***-***000", "SUSPENDED")
                        ))
                ));

        CardApplicationInvestAccountsResponse response = service.getInvestAccounts(authenticatedUser());

        assertThat(response.accounts()).hasSize(1);
        assertThat(response.accounts().get(0).investAccountUuid()).isEqualTo(activeAccountUuid);
        assertThat(response.accounts().get(0).accountNoDisplay()).isEqualTo("123-***-***456");
        assertThat(response.accounts().get(0).isLinked()).isTrue();
    }

    @Test
    @DisplayName("인증 사용자 정보가 없으면 401 예외를 반환한다")
    void getInvestAccountsWithoutAuthenticatedUser() {
        assertThatThrownBy(() -> service.getInvestAccounts(null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.AUTHENTICATION_REQUIRED);
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(authUserUuid, userUuid, "test-jti");
    }

    private GetMyUserMappingResponse linkedInvestMappingResponse() {
        return new GetMyUserMappingResponse(
                userUuid,
                null,
                new GetMyUserMappingResponse.InvestMapping(investUserUuid, true)
        );
    }
}
