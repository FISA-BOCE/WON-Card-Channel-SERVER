package com.woorifisa.won_card_channel_server.domain.performance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.woorifisa.won_card_channel_server.domain.auth.exception.code.AuthErrorCode;
import com.woorifisa.won_card_channel_server.domain.performance.dto.response.PreviousPerformanceResponse;
import com.woorifisa.won_card_channel_server.domain.performance.exception.code.PerformanceErrorCode;
import com.woorifisa.won_card_channel_server.domain.performance.external.CardCorePerformanceApi;
import com.woorifisa.won_card_channel_server.domain.performance.model.CardChnPerformanceSummary;
import com.woorifisa.won_card_channel_server.domain.performance.repository.CardChnPerformanceSummaryRepository;
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
class PerformanceSummaryServiceTest {

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
    private CardCorePerformanceApi cardCorePerformanceApi;

    @InjectMocks
    private PerformanceSummaryService performanceSummaryService;

    @Test
    @DisplayName("DB has performance summary then returns response from channel DB")
    void getPreviousPerformanceFromDb() {
        AuthenticatedUser authenticatedUser = authenticatedUser();
        CardChnPerformanceSummary performanceSummary = performanceSummary(
                new BigDecimal("1000000.9000"),
                new BigDecimal("10000.9000"),
                new BigDecimal("0.010000"),
                "2"
        );
        given(performanceSummaryRepository.findByUserUuidAndBaseMonth(USER_UUID, BASE_MONTH))
                .willReturn(Optional.of(performanceSummary));

        PreviousPerformanceResponse response = performanceSummaryService.getPreviousPerformance(authenticatedUser);

        assertThat(response.baseMonth()).isEqualTo(BASE_MONTH);
        assertThat(response.rewardStatus()).isEqualTo("기준 충족");
        assertThat(response.previousMonthSpendAmount()).isEqualTo(1_000_000L);
        assertThat(response.rewardPointAmount()).isEqualTo(10_000L);
        assertThat(response.rewardRate()).isEqualByComparingTo("0.010000");
        assertThat(response.performanceStatus()).isEqualTo("2");
        then(cardCorePerformanceApi).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("DB performance amount is zero then reward status is not satisfied")
    void getPreviousPerformanceFromDbWithZeroAmount() {
        AuthenticatedUser authenticatedUser = authenticatedUser();
        CardChnPerformanceSummary performanceSummary = performanceSummary(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("0.000000"),
                "1"
        );
        given(performanceSummaryRepository.findByUserUuidAndBaseMonth(USER_UUID, BASE_MONTH))
                .willReturn(Optional.of(performanceSummary));

        PreviousPerformanceResponse response = performanceSummaryService.getPreviousPerformance(authenticatedUser);

        assertThat(response.rewardStatus()).isEqualTo("기준 미달");
        assertThat(response.previousMonthSpendAmount()).isZero();
        assertThat(response.rewardPointAmount()).isZero();
        assertThat(response.rewardRate()).isEqualByComparingTo("0.000000");
        assertThat(response.performanceStatus()).isEqualTo("1");
        then(cardCorePerformanceApi).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("DB has no summary then returns card-core response data")
    void getPreviousPerformanceFromCardCoreWhenDbEmpty() {
        AuthenticatedUser authenticatedUser = authenticatedUser();
        PreviousPerformanceResponse coreData = coreResponse();
        given(performanceSummaryRepository.findByUserUuidAndBaseMonth(USER_UUID, BASE_MONTH))
                .willReturn(Optional.empty());
        given(cardCorePerformanceApi.getMonthlyPerformance(USER_UUID))
                .willReturn(ApiResponse.of(SuccessStatus.PREVIOUS_PERFORMANCE_FOUND, coreData));

        PreviousPerformanceResponse response = performanceSummaryService.getPreviousPerformance(authenticatedUser);

        assertThat(response).isSameAs(coreData);
        assertThat(response.baseMonth()).isEqualTo(BASE_MONTH);
        assertThat(response.rewardStatus()).isEqualTo("기준 충족");
        assertThat(response.previousMonthSpendAmount()).isEqualTo(1_000_000L);
        assertThat(response.rewardPointAmount()).isEqualTo(10_000L);
        assertThat(response.rewardRate()).isEqualByComparingTo("0.010000");
        assertThat(response.performanceStatus()).isEqualTo("2");
        then(performanceSummaryRepository).should().findByUserUuidAndBaseMonth(USER_UUID, BASE_MONTH);
        then(cardCorePerformanceApi).should().getMonthlyPerformance(USER_UUID);
    }

    @Test
    @DisplayName("AuthenticatedUser is null then throws authentication required")
    void getPreviousPerformanceWithoutAuthenticatedUser() {
        assertThatThrownBy(() -> performanceSummaryService.getPreviousPerformance(null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.AUTHENTICATION_REQUIRED);

        then(performanceSummaryRepository).shouldHaveNoInteractions();
        then(cardCorePerformanceApi).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("userUuid is null then throws authentication required")
    void getPreviousPerformanceWithoutUserUuid() {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(AUTH_USER_UUID, null, "test-jti");

        assertThatThrownBy(() -> performanceSummaryService.getPreviousPerformance(authenticatedUser))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.AUTHENTICATION_REQUIRED);

        then(performanceSummaryRepository).shouldHaveNoInteractions();
        then(cardCorePerformanceApi).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("previousMonthSpendAmount is null then throws invalid performance amount")
    void getPreviousPerformanceWithNullAmount() {
        AuthenticatedUser authenticatedUser = authenticatedUser();
        given(performanceSummaryRepository.findByUserUuidAndBaseMonth(USER_UUID, BASE_MONTH))
                .willReturn(Optional.of(performanceSummary(
                        null,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        "1"
                )));

        assertThatThrownBy(() -> performanceSummaryService.getPreviousPerformance(authenticatedUser))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(PerformanceErrorCode.INVALID_PERFORMANCE_AMOUNT);

        then(cardCorePerformanceApi).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("previousMonthSpendAmount is negative then throws invalid performance amount")
    void getPreviousPerformanceWithNegativeAmount() {
        AuthenticatedUser authenticatedUser = authenticatedUser();
        given(performanceSummaryRepository.findByUserUuidAndBaseMonth(USER_UUID, BASE_MONTH))
                .willReturn(Optional.of(performanceSummary(
                        new BigDecimal("-1.0000"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        "1"
                )));

        assertThatThrownBy(() -> performanceSummaryService.getPreviousPerformance(authenticatedUser))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(PerformanceErrorCode.INVALID_PERFORMANCE_AMOUNT);

        then(cardCorePerformanceApi).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("card-core response is null then throws performance not found")
    void getPreviousPerformanceCoreResponseNull() {
        AuthenticatedUser authenticatedUser = authenticatedUser();
        given(performanceSummaryRepository.findByUserUuidAndBaseMonth(USER_UUID, BASE_MONTH))
                .willReturn(Optional.empty());
        given(cardCorePerformanceApi.getMonthlyPerformance(USER_UUID)).willReturn(null);

        assertThatThrownBy(() -> performanceSummaryService.getPreviousPerformance(authenticatedUser))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(PerformanceErrorCode.PERFORMANCE_NOT_FOUND);
    }

    @Test
    @DisplayName("card-core data is null then throws performance not found")
    void getPreviousPerformanceCoreDataNull() {
        AuthenticatedUser authenticatedUser = authenticatedUser();
        given(performanceSummaryRepository.findByUserUuidAndBaseMonth(USER_UUID, BASE_MONTH))
                .willReturn(Optional.empty());
        given(cardCorePerformanceApi.getMonthlyPerformance(USER_UUID))
                .willReturn(ApiResponse.of(SuccessStatus.PREVIOUS_PERFORMANCE_FOUND, null));

        assertThatThrownBy(() -> performanceSummaryService.getPreviousPerformance(authenticatedUser))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(PerformanceErrorCode.PERFORMANCE_NOT_FOUND);
    }

    @Test
    @DisplayName("card-core returns 400 then throws performance not found")
    void getPreviousPerformanceCoreBadRequest() {
        AuthenticatedUser authenticatedUser = authenticatedUser();
        given(performanceSummaryRepository.findByUserUuidAndBaseMonth(USER_UUID, BASE_MONTH))
                .willReturn(Optional.empty());
        given(cardCorePerformanceApi.getMonthlyPerformance(USER_UUID))
                .willThrow(feignException(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> performanceSummaryService.getPreviousPerformance(authenticatedUser))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(PerformanceErrorCode.PERFORMANCE_NOT_FOUND);
    }

    @Test
    @DisplayName("card-core returns 404 then throws performance not found")
    void getPreviousPerformanceCoreNotFound() {
        AuthenticatedUser authenticatedUser = authenticatedUser();
        given(performanceSummaryRepository.findByUserUuidAndBaseMonth(USER_UUID, BASE_MONTH))
                .willReturn(Optional.empty());
        given(cardCorePerformanceApi.getMonthlyPerformance(USER_UUID))
                .willThrow(feignException(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> performanceSummaryService.getPreviousPerformance(authenticatedUser))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(PerformanceErrorCode.PERFORMANCE_NOT_FOUND);
    }

    @Test
    @DisplayName("card-core returns other feign error then throws performance not found")
    void getPreviousPerformanceCoreUnavailable() {
        AuthenticatedUser authenticatedUser = authenticatedUser();
        given(performanceSummaryRepository.findByUserUuidAndBaseMonth(USER_UUID, BASE_MONTH))
                .willReturn(Optional.empty());
        given(cardCorePerformanceApi.getMonthlyPerformance(USER_UUID))
                .willThrow(feignException(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> performanceSummaryService.getPreviousPerformance(authenticatedUser))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(PerformanceErrorCode.PERFORMANCE_NOT_FOUND);
    }

    private PreviousPerformanceResponse coreResponse() {
        return new PreviousPerformanceResponse(
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
                "/internal/cards/performance/monthly",
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
