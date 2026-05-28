package com.woorifisa.won_card_channel_server.domain.spend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.woorifisa.won_card_channel_server.domain.auth.exception.code.AuthErrorCode;
import com.woorifisa.won_card_channel_server.domain.performance.model.CardChnPerformanceSummary;
import com.woorifisa.won_card_channel_server.domain.performance.repository.CardChnPerformanceSummaryRepository;
import com.woorifisa.won_card_channel_server.domain.spend.dto.response.SpendCurrentAmountResponse;
import com.woorifisa.won_card_channel_server.domain.spend.exception.code.SpendErrorCode;
import com.woorifisa.won_card_channel_server.domain.spend.external.CardCoreSpendApi;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import com.woorifisa.won_card_channel_server.global.response.SuccessStatus;
import com.woorifisa.won_card_channel_server.global.security.AuthenticatedUser;
import feign.FeignException;
import feign.Request;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class SpendSummaryServiceTest {

    private static final UUID USER_UUID =
            UUID.fromString("8976c015-14e7-4c82-8817-978434d353dc");
    private static final UUID AUTH_USER_UUID =
            UUID.fromString("3a106f01-546b-49f3-a195-866b15896784");
    private static final UUID CARD_USER_UUID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String BASE_MONTH = YearMonth.now(ZoneId.of("Asia/Seoul")).toString();

    @Mock
    private CardChnPerformanceSummaryRepository performanceSummaryRepository;

    @Mock
    private CardCoreSpendApi cardCoreSpendApi;

    @InjectMocks
    private SpendSummaryService spendSummaryService;

    @ParameterizedTest
    @DisplayName("DB 조회 성공 시 경계값 기준으로 실적 구간과 리워드를 계산한다")
    @CsvSource(
            value = {
                    "0,1,2,500000,1.0,0.7,0",
                    "499999,1,2,1,1.0,0.7,3499",
                    "500000,2,3,1000000,1.2,1.0,5000",
                    "1499999,2,3,1,1.2,1.0,14999",
                    "1500000,3,3,0,1.2,1.2,18000",
                    "4000000,3,3,0,1.2,1.2,40000"
            }
    )
    void getSpendSummaryFromDbCalculatesPerformanceRange(
            long currentSpendAmount,
            String expectedCurrentStatus,
            String expectedNextStatus,
            long expectedRemainingAmount,
            String expectedNextRewardRate,
            String expectedCurrentRewardRate,
            long expectedRewardAmount
    ) {
        // given
        AuthenticatedUser authenticatedUser = authenticatedUser();
        given(performanceSummaryRepository.findByUserUuidAndBaseMonth(USER_UUID, BASE_MONTH))
                .willReturn(Optional.of(performanceSummary(BigDecimal.valueOf(currentSpendAmount))));

        // when
        SpendCurrentAmountResponse response = spendSummaryService.getSpendSummary(authenticatedUser);

        // then
        assertThat(response.hasCurrentSpendAmount()).isTrue();
        assertThat(response.baseMonth()).isEqualTo(BASE_MONTH);
        assertThat(response.currentSpendAmount()).isEqualTo(currentSpendAmount);
        assertThat(response.currentRewardRate()).isEqualByComparingTo(expectedCurrentRewardRate);
        assertThat(response.nextPerformanceStatus()).isEqualTo(expectedNextStatus);
        assertThat(response.amountRemainingUntilNextPerformance()).isEqualTo(expectedRemainingAmount);
        assertThat(response.nextRewardRate()).isEqualByComparingTo(expectedNextRewardRate);
        assertRewardRanges(response.rewardRanges());
        assertThat(response.expectedReward().targetSpendAmount()).isEqualTo(currentSpendAmount);
        assertThat(response.expectedReward().rewardRate()).isEqualByComparingTo(expectedCurrentRewardRate);
        assertThat(response.expectedReward().expectedRewardAmount()).isEqualTo(expectedRewardAmount);

        String actualCurrentStatus = calculateCurrentStatusFromResponse(response);
        assertThat(actualCurrentStatus).isEqualTo(expectedCurrentStatus);
        then(cardCoreSpendApi).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("DB에 없으면 계정계 API 응답 data를 꺼내 최종 응답으로 조립한다")
    void getSpendSummaryFromCardCoreWhenDbEmpty() {
        // given
        AuthenticatedUser authenticatedUser = authenticatedUser();
        SpendCurrentAmountResponse coreResponse = coreResponse();

        given(performanceSummaryRepository.findByUserUuidAndBaseMonth(USER_UUID, BASE_MONTH))
                .willReturn(Optional.empty());
        given(cardCoreSpendApi.getSpendSummary(USER_UUID))
                .willReturn(ApiResponse.of(SuccessStatus.CURRENT_SPEND_AMOUNT_FOUND, coreResponse));

        // when
        SpendCurrentAmountResponse response = spendSummaryService.getSpendSummary(authenticatedUser);

        // then
        assertThat(response).isNotSameAs(coreResponse);
        assertThat(response.hasCurrentSpendAmount()).isEqualTo(coreResponse.hasCurrentSpendAmount());
        assertThat(response.baseMonth()).isEqualTo(coreResponse.baseMonth());
        assertThat(response.currentSpendAmount()).isEqualTo(coreResponse.currentSpendAmount());
        assertThat(response.currentRewardRate()).isEqualByComparingTo(coreResponse.currentRewardRate());
        assertThat(response.nextPerformanceStatus()).isEqualTo(coreResponse.nextPerformanceStatus());
        assertThat(response.amountRemainingUntilNextPerformance())
                .isEqualTo(coreResponse.amountRemainingUntilNextPerformance());
        assertThat(response.nextRewardRate()).isEqualByComparingTo(coreResponse.nextRewardRate());
        assertRewardRanges(response.rewardRanges());
        assertThat(response.expectedReward()).isEqualTo(coreResponse.expectedReward());
        then(performanceSummaryRepository).should().findByUserUuidAndBaseMonth(USER_UUID, BASE_MONTH);
        then(cardCoreSpendApi).should().getSpendSummary(USER_UUID);
    }

    @Test
    @DisplayName("인증 사용자 정보가 없으면 인증 필요 예외를 던지고 DB와 계정계를 호출하지 않는다")
    void getSpendSummaryWithoutAuthenticatedUser() {
        assertThatThrownBy(() -> spendSummaryService.getSpendSummary(null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.AUTHENTICATION_REQUIRED);

        then(performanceSummaryRepository).shouldHaveNoInteractions();
        then(cardCoreSpendApi).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("계정계 응답 객체가 null이면 응답 형식 예외를 던진다")
    void getSpendSummaryCoreResponseNull() {
        // given
        AuthenticatedUser authenticatedUser = authenticatedUser();
        given(performanceSummaryRepository.findByUserUuidAndBaseMonth(USER_UUID, BASE_MONTH))
                .willReturn(Optional.empty());
        given(cardCoreSpendApi.getSpendSummary(USER_UUID)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> spendSummaryService.getSpendSummary(authenticatedUser))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(SpendErrorCode.INVALID_SPEND_RESPONSE);
    }

    @Test
    @DisplayName("계정계 응답 data가 null이면 응답 형식 예외를 던진다")
    void getSpendSummaryCoreResponseDataNull() {
        // given
        AuthenticatedUser authenticatedUser = authenticatedUser();
        given(performanceSummaryRepository.findByUserUuidAndBaseMonth(USER_UUID, BASE_MONTH))
                .willReturn(Optional.empty());
        given(cardCoreSpendApi.getSpendSummary(USER_UUID))
                .willReturn(ApiResponse.of(SuccessStatus.CURRENT_SPEND_AMOUNT_FOUND, null));

        // when & then
        assertThatThrownBy(() -> spendSummaryService.getSpendSummary(authenticatedUser))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(SpendErrorCode.INVALID_SPEND_RESPONSE);
    }

    @Test
    @DisplayName("계정계가 404를 반환하면 CARD_404_001 예외를 던진다")
    void getSpendSummaryCoreNotFound() {
        // given
        AuthenticatedUser authenticatedUser = authenticatedUser();
        given(performanceSummaryRepository.findByUserUuidAndBaseMonth(USER_UUID, BASE_MONTH))
                .willReturn(Optional.empty());
        given(cardCoreSpendApi.getSpendSummary(USER_UUID)).willThrow(feignException(HttpStatus.NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> spendSummaryService.getSpendSummary(authenticatedUser))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(SpendErrorCode.CURRENT_SPEND_AMOUNT_NOT_FOUND);
    }

    @Test
    @DisplayName("계정계 호출 중 404 외 Feign 예외가 발생하면 이용 금액 조회 불가 예외를 던진다")
    void getSpendSummaryCoreUnavailable() {
        // given
        AuthenticatedUser authenticatedUser = authenticatedUser();
        given(performanceSummaryRepository.findByUserUuidAndBaseMonth(USER_UUID, BASE_MONTH))
                .willReturn(Optional.empty());
        given(cardCoreSpendApi.getSpendSummary(USER_UUID)).willThrow(feignException(HttpStatus.BAD_GATEWAY));

        // when & then
        assertThatThrownBy(() -> spendSummaryService.getSpendSummary(authenticatedUser))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(SpendErrorCode.SPEND_INFORMATION_UNAVAILABLE);
    }

    private void assertRewardRanges(List<SpendCurrentAmountResponse.RewardRange> rewardRanges) {
        assertThat(rewardRanges).hasSize(3);
        assertThat(rewardRanges.get(0).min()).isEqualTo(0L);
        assertThat(rewardRanges.get(0).max()).isEqualTo(499_999L);
        assertThat(rewardRanges.get(0).rate()).isEqualByComparingTo("0.7");
        assertThat(rewardRanges.get(1).min()).isEqualTo(500_000L);
        assertThat(rewardRanges.get(1).max()).isEqualTo(1_499_999L);
        assertThat(rewardRanges.get(1).rate()).isEqualByComparingTo("1.0");
        assertThat(rewardRanges.get(2).min()).isEqualTo(1_500_000L);
        assertThat(rewardRanges.get(2).max()).isNull();
        assertThat(rewardRanges.get(2).rate()).isEqualByComparingTo("1.2");
    }

    private String calculateCurrentStatusFromResponse(SpendCurrentAmountResponse response) {
        if (response.currentSpendAmount() < 500_000L) {
            return "1";
        }
        if (response.currentSpendAmount() < 1_500_000L) {
            return "2";
        }
        return "3";
    }

    private SpendCurrentAmountResponse coreResponse() {
        return new SpendCurrentAmountResponse(
                true,
                BASE_MONTH,
                1_245_000L,
                new BigDecimal("1.0"),
                "3",
                255_000L,
                new BigDecimal("1.2"),
                null,
                new SpendCurrentAmountResponse.ExpectedReward(
                        1_245_000L,
                        new BigDecimal("1.0"),
                        12_450L
                )
        );
    }

    private CardChnPerformanceSummary performanceSummary(BigDecimal currentSpendAmount) {
        return CardChnPerformanceSummary.builder()
                .performanceId(1L)
                .userUuid(USER_UUID)
                .cardUserUuid(CARD_USER_UUID)
                .baseMonth(BASE_MONTH)
                .previousMonthSpendAmount(BigDecimal.ZERO)
                .currentMonthSpendAmount(currentSpendAmount)
                .rewardRate(BigDecimal.ZERO)
                .rewardPointAmount(BigDecimal.ZERO)
                .performanceStatus(null)
                .lastSyncedAt(LocalDateTime.of(2026, 5, 28, 1, 0))
                .build();
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(AUTH_USER_UUID, USER_UUID, "test-jti");
    }

    private FeignException feignException(HttpStatus status) {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "/internal/cards/spend-summary",
                Map.of(),
                null,
                null,
                null
        );

        return switch (status) {
            case NOT_FOUND -> new FeignException.NotFound(
                    "not found",
                    request,
                    null,
                    Map.of()
            );
            default -> new FeignException.BadGateway(
                    "bad gateway",
                    request,
                    null,
                    Map.of()
            );
        };
    }
}
