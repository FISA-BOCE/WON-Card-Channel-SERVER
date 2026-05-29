package com.woorifisa.won_card_channel_server.domain.reward.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.woorifisa.won_card_channel_server.domain.auth.exception.code.AuthErrorCode;
import com.woorifisa.won_card_channel_server.domain.performance.exception.code.PerformanceErrorCode;
import com.woorifisa.won_card_channel_server.domain.performance.model.CardChnPerformanceSummary;
import com.woorifisa.won_card_channel_server.domain.performance.repository.CardChnPerformanceSummaryRepository;
import com.woorifisa.won_card_channel_server.domain.reward.dto.response.RewardGetCurrentResponse;
import com.woorifisa.won_card_channel_server.domain.reward.exception.code.RewardErrorCode;
import com.woorifisa.won_card_channel_server.domain.reward.external.CardCoreRewardApi;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class RewardGetCurrentMonthServiceTest {

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
    private CardCoreRewardApi cardCoreRewardApi;

    @InjectMocks
    private RewardGetCurrentMonthService rewardGetCurrentMonthService;

    @Test
    @DisplayName("DB에 당월 리워드 정보가 있으면 채널계 DB 데이터로 응답한다")
    void getCurrentMonthRewardFromDb() {
        AuthenticatedUser authenticatedUser = authenticatedUser();
        CardChnPerformanceSummary performanceSummary = performanceSummary(
                new BigDecimal("1000000.9000"),
                new BigDecimal("10000.9000"),
                new BigDecimal("0.010000"),
                "2"
        );
        given(performanceSummaryRepository.findByUserUuidAndBaseMonth(USER_UUID, BASE_MONTH))
                .willReturn(Optional.of(performanceSummary));

        RewardGetCurrentResponse response = rewardGetCurrentMonthService.getCurrentMonthReward(authenticatedUser);

        assertThat(response.baseMonth()).isEqualTo(BASE_MONTH);
        assertThat(response.rewardStatus()).isEqualTo("기준 충족");
        assertThat(response.previousMonthSpendAmount()).isEqualTo(1_000_000L);
        assertThat(response.rewardPointAmount()).isEqualTo(10_000L);
        assertThat(response.rewardRate()).isEqualByComparingTo("0.010000");
        assertThat(response.performanceStatus()).isEqualTo("2");
        then(cardCoreRewardApi).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("전월 이용 금액이 0이면 기준 미달로 응답한다")
    void getCurrentMonthRewardFromDbWithZeroAmount() {
        AuthenticatedUser authenticatedUser = authenticatedUser();
        CardChnPerformanceSummary performanceSummary = performanceSummary(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("0.000000"),
                "1"
        );
        given(performanceSummaryRepository.findByUserUuidAndBaseMonth(USER_UUID, BASE_MONTH))
                .willReturn(Optional.of(performanceSummary));

        RewardGetCurrentResponse response = rewardGetCurrentMonthService.getCurrentMonthReward(authenticatedUser);

        assertThat(response.rewardStatus()).isEqualTo("기준 미달");
        assertThat(response.previousMonthSpendAmount()).isZero();
        assertThat(response.rewardPointAmount()).isZero();
        assertThat(response.rewardRate()).isEqualByComparingTo("0.000000");
        assertThat(response.performanceStatus()).isEqualTo("1");
        then(cardCoreRewardApi).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("DB에 당월 리워드 정보가 없으면 계정계 응답 data를 반환한다")
    void getCurrentMonthRewardFromCardCoreWhenDbEmpty() {
        AuthenticatedUser authenticatedUser = authenticatedUser();
        RewardGetCurrentResponse coreData = coreResponse();
        given(performanceSummaryRepository.findByUserUuidAndBaseMonth(USER_UUID, BASE_MONTH))
                .willReturn(Optional.empty());
        given(cardCoreRewardApi.getCurrentMonthReward(USER_UUID))
                .willReturn(ApiResponse.of(SuccessStatus.OK, coreData));

        RewardGetCurrentResponse response = rewardGetCurrentMonthService.getCurrentMonthReward(authenticatedUser);

        assertThat(response).isSameAs(coreData);
        assertThat(response.baseMonth()).isEqualTo(BASE_MONTH);
        assertThat(response.rewardStatus()).isEqualTo("기준 충족");
        assertThat(response.previousMonthSpendAmount()).isEqualTo(1_000_000L);
        assertThat(response.rewardPointAmount()).isEqualTo(10_000L);
        assertThat(response.rewardRate()).isEqualByComparingTo("0.010000");
        assertThat(response.performanceStatus()).isEqualTo("2");
        then(performanceSummaryRepository).should().findByUserUuidAndBaseMonth(USER_UUID, BASE_MONTH);
        then(cardCoreRewardApi).should().getCurrentMonthReward(USER_UUID);
    }

    @Test
    @DisplayName("인증 사용자 정보가 없으면 인증 필요 예외를 던진다")
    void getCurrentMonthRewardWithoutAuthenticatedUser() {
        assertThatThrownBy(() -> rewardGetCurrentMonthService.getCurrentMonthReward(null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.AUTHENTICATION_REQUIRED);

        then(performanceSummaryRepository).shouldHaveNoInteractions();
        then(cardCoreRewardApi).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("userUuid가 없으면 인증 필요 예외를 던진다")
    void getCurrentMonthRewardWithoutUserUuid() {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(AUTH_USER_UUID, null, "test-jti");

        assertThatThrownBy(() -> rewardGetCurrentMonthService.getCurrentMonthReward(authenticatedUser))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.AUTHENTICATION_REQUIRED);

        then(performanceSummaryRepository).shouldHaveNoInteractions();
        then(cardCoreRewardApi).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("전월 이용 금액이 null이면 금액 형식 예외를 던진다")
    void getCurrentMonthRewardWithNullAmount() {
        AuthenticatedUser authenticatedUser = authenticatedUser();
        given(performanceSummaryRepository.findByUserUuidAndBaseMonth(USER_UUID, BASE_MONTH))
                .willReturn(Optional.of(performanceSummary(
                        null,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        "1"
                )));

        assertThatThrownBy(() -> rewardGetCurrentMonthService.getCurrentMonthReward(authenticatedUser))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(PerformanceErrorCode.INVALID_PERFORMANCE_AMOUNT);

        then(cardCoreRewardApi).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("전월 이용 금액이 음수이면 금액 형식 예외를 던진다")
    void getCurrentMonthRewardWithNegativeAmount() {
        AuthenticatedUser authenticatedUser = authenticatedUser();
        given(performanceSummaryRepository.findByUserUuidAndBaseMonth(USER_UUID, BASE_MONTH))
                .willReturn(Optional.of(performanceSummary(
                        new BigDecimal("-1.0000"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        "1"
                )));

        assertThatThrownBy(() -> rewardGetCurrentMonthService.getCurrentMonthReward(authenticatedUser))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(PerformanceErrorCode.INVALID_PERFORMANCE_AMOUNT);

        then(cardCoreRewardApi).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("계정계 응답 객체가 null이면 리워드 없음 예외를 던진다")
    void getCurrentMonthRewardCoreResponseNull() {
        AuthenticatedUser authenticatedUser = authenticatedUser();
        given(performanceSummaryRepository.findByUserUuidAndBaseMonth(USER_UUID, BASE_MONTH))
                .willReturn(Optional.empty());
        given(cardCoreRewardApi.getCurrentMonthReward(USER_UUID)).willReturn(null);

        assertThatThrownBy(() -> rewardGetCurrentMonthService.getCurrentMonthReward(authenticatedUser))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(RewardErrorCode.REWARD_LEDGER_NOT_FOUND);
    }

    @Test
    @DisplayName("계정계 응답 data가 null이면 리워드 없음 예외를 던진다")
    void getCurrentMonthRewardCoreDataNull() {
        AuthenticatedUser authenticatedUser = authenticatedUser();
        given(performanceSummaryRepository.findByUserUuidAndBaseMonth(USER_UUID, BASE_MONTH))
                .willReturn(Optional.empty());
        given(cardCoreRewardApi.getCurrentMonthReward(USER_UUID))
                .willReturn(ApiResponse.of(SuccessStatus.OK, null));

        assertThatThrownBy(() -> rewardGetCurrentMonthService.getCurrentMonthReward(authenticatedUser))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(RewardErrorCode.REWARD_LEDGER_NOT_FOUND);
    }

    @Test
    @DisplayName("계정계가 400을 반환하면 리워드 없음 예외를 던진다")
    void getCurrentMonthRewardCoreBadRequest() {
        AuthenticatedUser authenticatedUser = authenticatedUser();
        given(performanceSummaryRepository.findByUserUuidAndBaseMonth(USER_UUID, BASE_MONTH))
                .willReturn(Optional.empty());
        given(cardCoreRewardApi.getCurrentMonthReward(USER_UUID))
                .willThrow(feignException(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> rewardGetCurrentMonthService.getCurrentMonthReward(authenticatedUser))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(RewardErrorCode.REWARD_LEDGER_NOT_FOUND);
    }

    @Test
    @DisplayName("계정계가 404를 반환하면 리워드 없음 예외를 던진다")
    void getCurrentMonthRewardCoreNotFound() {
        AuthenticatedUser authenticatedUser = authenticatedUser();
        given(performanceSummaryRepository.findByUserUuidAndBaseMonth(USER_UUID, BASE_MONTH))
                .willReturn(Optional.empty());
        given(cardCoreRewardApi.getCurrentMonthReward(USER_UUID))
                .willThrow(feignException(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> rewardGetCurrentMonthService.getCurrentMonthReward(authenticatedUser))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(RewardErrorCode.REWARD_LEDGER_NOT_FOUND);
    }

    @Test
    @DisplayName("계정계가 기타 Feign 예외를 반환하면 리워드 없음 예외를 던진다")
    void getCurrentMonthRewardCoreUnavailable() {
        AuthenticatedUser authenticatedUser = authenticatedUser();
        given(performanceSummaryRepository.findByUserUuidAndBaseMonth(USER_UUID, BASE_MONTH))
                .willReturn(Optional.empty());
        given(cardCoreRewardApi.getCurrentMonthReward(USER_UUID))
                .willThrow(feignException(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> rewardGetCurrentMonthService.getCurrentMonthReward(authenticatedUser))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(RewardErrorCode.REWARD_LEDGER_NOT_FOUND);
    }

    private RewardGetCurrentResponse coreResponse() {
        return new RewardGetCurrentResponse(
                BASE_MONTH,
                "기준 충족",
                1_000_000L,
                10_000L,
                new BigDecimal("0.010000"),
                "2"
        );
    }

    private CardChnPerformanceSummary performanceSummary(
            BigDecimal previousMonthSpendAmount,
            BigDecimal rewardPointAmount,
            BigDecimal rewardRate,
            String performanceStatus
    ) {
        return CardChnPerformanceSummary.builder()
                .performanceId(1L)
                .userUuid(USER_UUID)
                .cardUserUuid(CARD_USER_UUID)
                .baseMonth(BASE_MONTH)
                .previousMonthSpendAmount(previousMonthSpendAmount)
                .currentMonthSpendAmount(BigDecimal.ZERO)
                .rewardRate(rewardRate)
                .rewardPointAmount(rewardPointAmount)
                .performanceStatus(performanceStatus)
                .lastSyncedAt(LocalDateTime.of(2026, 5, 28, 1, 0))
                .build();
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(AUTH_USER_UUID, USER_UUID, "test-jti");
    }

    private FeignException feignException(HttpStatus status) {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "/internal/cards/rewards/monthly",
                Map.of(),
                null,
                null,
                null
        );

        return switch (status) {
            case BAD_REQUEST -> new FeignException.BadRequest("bad request", request, null, Map.of());
            case NOT_FOUND -> new FeignException.NotFound("not found", request, null, Map.of());
            default -> new FeignException.BadGateway("bad gateway", request, null, Map.of());
        };
    }
}
