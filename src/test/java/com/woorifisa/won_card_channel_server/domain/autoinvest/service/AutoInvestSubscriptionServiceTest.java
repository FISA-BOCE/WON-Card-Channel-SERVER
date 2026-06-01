package com.woorifisa.won_card_channel_server.domain.autoinvest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.woorifisa.won_card_channel_server.domain.auth.exception.code.AuthErrorCode;
import com.woorifisa.won_card_channel_server.domain.autoinvest.dto.request.AutoInvestSubscriptionChangeRequest;
import com.woorifisa.won_card_channel_server.domain.autoinvest.dto.response.AutoInvestSubscriptionChangeResponse;
import com.woorifisa.won_card_channel_server.domain.autoinvest.dto.response.AutoInvestSubscriptionDetailResponse;
import com.woorifisa.won_card_channel_server.domain.autoinvest.dto.response.InvestAccountDetailsResponse;
import com.woorifisa.won_card_channel_server.domain.autoinvest.dto.response.InvestEtfDetailsResponse;
import com.woorifisa.won_card_channel_server.domain.autoinvest.exception.code.AutoInvestErrorCode;
import com.woorifisa.won_card_channel_server.domain.autoinvest.external.InvestChannelAutoInvestApi;
import com.woorifisa.won_card_channel_server.domain.autoinvest.service.validate.InvestEtfResponseValidator;
import com.woorifisa.won_card_channel_server.domain.card.model.CardChnCardSummary;
import com.woorifisa.won_card_channel_server.domain.card.repository.CardChnCardSummaryRepository;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import com.woorifisa.won_card_channel_server.global.response.SuccessStatus;
import com.woorifisa.won_card_channel_server.global.security.AuthenticatedUser;
import feign.FeignException;
import feign.Request;
import feign.Response;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AutoInvestSubscriptionServiceTest {

    private final UUID userUuid = UUID.fromString("f4c7a361-b78e-488f-af69-7f2c79ff1d7b");
    private final UUID authUserUuid = UUID.fromString("d1aa5e15-9dab-462a-8c4d-3b2e8f37a6c3");
    private final UUID invstAccountUuid = UUID.fromString("0a35d84a-27b9-4c3f-bfd4-5b4e2c4d3657");
    private final UUID cardUuid = UUID.fromString("341b7149-0d80-4686-bc96-37ee4b4ae60a");

    private CardChnCardSummaryRepository cardSummaryRepository;
    private InvestChannelAutoInvestApi investChannelAutoInvestApi;
    private InvestEtfResponseValidator investEtfResponseValidator;
    private AutoInvestSubscriptionServiceImpl service;

    @BeforeEach
    void setUp() {
        cardSummaryRepository = Mockito.mock(CardChnCardSummaryRepository.class);
        investChannelAutoInvestApi = Mockito.mock(InvestChannelAutoInvestApi.class);
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        investEtfResponseValidator = new InvestEtfResponseValidator(validator);
        service = new AutoInvestSubscriptionServiceImpl(
                cardSummaryRepository,
                investChannelAutoInvestApi,
                investEtfResponseValidator
        );
    }

    @Test
    @DisplayName("카드 신청 내부 호출이면 card summary의 selected ETF를 동기화한다")
    void createInitialSubscription() {
        CardChnCardSummary summary = cardSummary(100L);
        givenAccountActive();
        given(investChannelAutoInvestApi.getEtf(101L))
                .willReturn(ApiResponse.of(
                        SuccessStatus.OK,
                        new InvestEtfDetailsResponse(101L, "S&P 500 ETF", "VOO", true, true)
                ));
        given(cardSummaryRepository.findByUserUuid(userUuid)).willReturn(Optional.of(summary));

        service.createInitialSubscription(userUuid, invstAccountUuid, 101L, "VOO");

        assertThat(summary.getSelectedEtfId()).isEqualTo(101L);
    }

    @Test
    @DisplayName("자동투자 설정 조회 시 card summary의 selected ETF를 기준으로 응답한다")
    void getSubscription() {
        given(cardSummaryRepository.findByUserUuid(userUuid)).willReturn(Optional.of(cardSummary(101L)));
        given(investChannelAutoInvestApi.getEtf(101L))
                .willReturn(ApiResponse.of(
                        SuccessStatus.OK,
                        new InvestEtfDetailsResponse(101L, "S&P 500 ETF", "VOO", true, true)
                ));

        AutoInvestSubscriptionDetailResponse response = service.getSubscription(authenticatedUser(), cardUuid);

        assertThat(response.cardUuid()).isEqualTo(cardUuid);
        assertThat(response.currentEtf().etfId()).isEqualTo(101L);
        assertThat(response.currentEtf().etfName()).isEqualTo("S&P 500 ETF");
    }

    @Test
    @DisplayName("selected ETF가 없으면 조회 시 예외가 발생한다")
    void getSubscriptionNotFound() {
        given(cardSummaryRepository.findByUserUuid(userUuid)).willReturn(Optional.of(cardSummary(null)));

        assertThatThrownBy(() -> service.getSubscription(authenticatedUser(), cardUuid))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AutoInvestErrorCode.AUTO_INVEST_CHANGE_TARGET_NOT_FOUND);
    }

    @Test
    @DisplayName("ETF 변경 시 selected ETF와 마지막 동기화 시각을 갱신한다")
    void changeSubscription() {
        CardChnCardSummary summary = cardSummary(101L);
        LocalDateTime before = summary.getLastSyncedAt();
        given(cardSummaryRepository.findByUserUuid(userUuid)).willReturn(Optional.of(summary));
        given(investChannelAutoInvestApi.getEtf(101L))
                .willReturn(ApiResponse.of(SuccessStatus.OK,
                        new InvestEtfDetailsResponse(101L, "S&P 500 ETF", "VOO", true, true)));
        given(investChannelAutoInvestApi.getEtf(202L))
                .willReturn(ApiResponse.of(SuccessStatus.OK,
                        new InvestEtfDetailsResponse(202L, "NASDAQ 100 ETF", "QQQ", true, true)));

        AutoInvestSubscriptionChangeResponse response = service.changeSubscription(
                authenticatedUser(),
                cardUuid,
                new AutoInvestSubscriptionChangeRequest(202L)
        );

        assertThat(summary.getSelectedEtfId()).isEqualTo(202L);
        assertThat(summary.getLastSyncedAt()).isAfter(before);
        assertThat(response.cardUuid()).isEqualTo(cardUuid);
        assertThat(response.previousEtf().ticker()).isEqualTo("VOO");
        assertThat(response.newEtf().etfId()).isEqualTo(202L);
    }

    @Test
    @DisplayName("동일한 ETF로 변경하면 예외가 발생한다")
    void changeSubscriptionWithSameEtf() {
        given(cardSummaryRepository.findByUserUuid(userUuid)).willReturn(Optional.of(cardSummary(101L)));
        given(investChannelAutoInvestApi.getEtf(101L))
                .willReturn(ApiResponse.of(SuccessStatus.OK,
                        new InvestEtfDetailsResponse(101L, "S&P 500 ETF", "VOO", true, true)));

        assertThatThrownBy(() -> service.changeSubscription(
                authenticatedUser(),
                cardUuid,
                new AutoInvestSubscriptionChangeRequest(101L)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AutoInvestErrorCode.AUTO_INVEST_SAME_ETF);
    }

    @Test
    @DisplayName("카드 UUID가 다르면 본인 설정이 아닌 것으로 본다")
    void changeSubscriptionNotOwner() {
        given(cardSummaryRepository.findByUserUuid(userUuid)).willReturn(Optional.of(cardSummary(101L)));

        assertThatThrownBy(() -> service.changeSubscription(
                authenticatedUser(),
                UUID.fromString("ec8104fa-3593-4797-93cc-a6cf056c0d78"),
                new AutoInvestSubscriptionChangeRequest(202L)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AutoInvestErrorCode.AUTO_INVEST_NOT_OWNER);
    }

    @Test
    @DisplayName("인증 사용자 정보가 없으면 인증 필요 예외가 발생한다")
    void getSubscriptionWithoutAuthenticatedUser() {
        assertThatThrownBy(() -> service.getSubscription(null, cardUuid))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.AUTHENTICATION_REQUIRED);
    }

    @Test
    @DisplayName("타인 증권계좌면 최초 동기화 시 접근 불가 예외가 발생한다")
    void createInitialSubscriptionWithAnotherUsersAccount() {
        UUID anotherUserUuid = UUID.fromString("8eb9c930-41ff-4e12-92da-34ffdcae8100");
        given(investChannelAutoInvestApi.getInvestmentAccount(userUuid, invstAccountUuid))
                .willReturn(ApiResponse.of(
                        SuccessStatus.OK,
                        new InvestAccountDetailsResponse(invstAccountUuid, anotherUserUuid, "ACTIVE")
                ));

        assertThatThrownBy(() -> service.createInitialSubscription(userUuid, invstAccountUuid, 101L, "VOO"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AutoInvestErrorCode.INVEST_ACCOUNT_FORBIDDEN);
    }

    @Test
    @DisplayName("ETF 조회 연동 실패는 외부 연동 예외로 변환한다")
    void createInitialSubscriptionWhenEtfApiFails() {
        givenAccountActive();
        given(investChannelAutoInvestApi.getEtf(101L)).willThrow(feignException(502));

        assertThatThrownBy(() -> service.createInitialSubscription(userUuid, invstAccountUuid, 101L, "VOO"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AutoInvestErrorCode.ETF_UNAVAILABLE);
    }

    @Test
    @DisplayName("거래 불가능한 ETF면 최초 동기화 시 예외가 발생한다")
    void createInitialSubscriptionWhenEtfNotTradable() {
        givenAccountActive();
        given(investChannelAutoInvestApi.getEtf(101L))
                .willReturn(ApiResponse.of(
                        SuccessStatus.OK,
                        new InvestEtfDetailsResponse(101L, "S&P 500 ETF", "VOO", false, true)
                ));

        assertThatThrownBy(() -> service.createInitialSubscription(userUuid, invstAccountUuid, 101L, "VOO"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AutoInvestErrorCode.ETF_NOT_TRADABLE);
    }

    @Test
    @DisplayName("소수점 매수가 불가능한 ETF면 최초 동기화 시 예외가 발생한다")
    void createInitialSubscriptionWhenEtfFractionalBuyNotAllowed() {
        givenAccountActive();
        given(investChannelAutoInvestApi.getEtf(101L))
                .willReturn(ApiResponse.of(
                        SuccessStatus.OK,
                        new InvestEtfDetailsResponse(101L, "S&P 500 ETF", "VOO", true, false)
                ));

        assertThatThrownBy(() -> service.createInitialSubscription(userUuid, invstAccountUuid, 101L, "VOO"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AutoInvestErrorCode.ETF_FRACTIONAL_BUY_NOT_ALLOWED);
    }

    private void givenAccountActive() {
        given(investChannelAutoInvestApi.getInvestmentAccount(userUuid, invstAccountUuid))
                .willReturn(ApiResponse.of(
                        SuccessStatus.OK,
                        new InvestAccountDetailsResponse(invstAccountUuid, userUuid, "ACTIVE")
                ));
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(authUserUuid, userUuid, "test-jti");
    }

    private CardChnCardSummary cardSummary(Long selectedEtfId) {
        return CardChnCardSummary.builder()
                .userUuid(userUuid)
                .cardUserUuid(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .cardUuid(cardUuid)
                .cardName("WON 자동투자 카드")
                .cardNoDisplay("**** **** **** 1234")
                .cardStatus("ACTIVE")
                .currentMonthUsageAmount(BigDecimal.ZERO)
                .lastSyncedAt(LocalDateTime.of(2026, 5, 28, 12, 0))
                .selectedEtfId(selectedEtfId)
                .build();
    }

    private FeignException feignException(int status) {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "http://localhost/test",
                Map.of(),
                null,
                StandardCharsets.UTF_8,
                null
        );
        return FeignException.errorStatus(
                "getEtf",
                Response.builder()
                        .status(status)
                        .reason("error")
                        .request(request)
                        .headers(Map.of())
                        .build()
        );
    }
}
