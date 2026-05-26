package com.woorifisa.won_card_channel_server.domain.card;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.woorifisa.won_card_channel_server.domain.auth.exception.code.AuthErrorCode;
import com.woorifisa.won_card_channel_server.domain.card.dto.response.ExistingCardSummaryResponse;
import com.woorifisa.won_card_channel_server.domain.card.dto.response.NoCardSummaryResponse;
import com.woorifisa.won_card_channel_server.domain.card.model.CardChnCardSummary;
import com.woorifisa.won_card_channel_server.domain.card.repository.CardChnCardSummaryRepository;
import com.woorifisa.won_card_channel_server.domain.card.service.CardSummaryService;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import com.woorifisa.won_card_channel_server.global.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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

@ExtendWith(MockitoExtension.class)
class CardSummaryServiceTest {

    private static final UUID USER_UUID =
            UUID.fromString("8976c015-14e7-4c82-8817-978434d353dc");
    private static final UUID AUTH_USER_UUID =
            UUID.fromString("3a106f01-546b-49f3-a195-866b15896784");
    private static final UUID CARD_USER_UUID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID CARD_UUID =
            UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private CardChnCardSummaryRepository cardSummaryRepository;

    @InjectMocks
    private CardSummaryService cardSummaryService;

    @Test
    @DisplayName("카드가 없으면 고정 카드 상품 정보를 반환한다")
    void getCardSummaryWithoutCard() {
        // given
        AuthenticatedUser authenticatedUser = authenticatedUser();

        given(cardSummaryRepository.findByUserUuid(USER_UUID))
                .willReturn(Optional.empty());

        // when
        Object result = cardSummaryService.getCards(authenticatedUser);

        // then
        assertThat(result).isInstanceOf(NoCardSummaryResponse.class);

        NoCardSummaryResponse response = (NoCardSummaryResponse) result;
        assertThat(response.hasCard()).isFalse();
        assertThat(response.cardProduct().productName()).isEqualTo("WON 자동투자 카드");
        assertThat(response.cardProduct().rewardRateMin()).isEqualByComparingTo("0.7");
        assertThat(response.cardProduct().rewardRateMax()).isEqualByComparingTo("1.2");
        assertThat(response.cardProduct().monthlyLimitAmount()).isEqualTo(200_000L);
        assertThat(response.cardProduct().benefits()).hasSize(3);

        then(cardSummaryRepository).should().findByUserUuid(USER_UUID);
    }

    @ParameterizedTest
    @DisplayName("당월 이용금액에 따라 현재 적립 구간을 계산한다")
    @CsvSource(
            value = {
                    "100000,0.7,0,500000",
                    "499999,0.7,0,500000",
                    "500000,1.0,500000,1500000",
                    "1499999,1.0,500000,1500000",
                    "1500000,1.2,1500000,NULL",
                    "2000000,1.2,1500000,NULL"
            },
            nullValues = "NULL"
    )
    void getCardSummaryWithCardCalculatesCurrentRewardRange(
            long usageAmount,
            String expectedRate,
            Long expectedRangeMin,
            Long expectedRangeMax
    ) {
        // given
        AuthenticatedUser authenticatedUser = authenticatedUser();
        CardChnCardSummary cardSummary = cardSummary(BigDecimal.valueOf(usageAmount));

        given(cardSummaryRepository.findByUserUuid(USER_UUID))
                .willReturn(Optional.of(cardSummary));

        // when
        Object result = cardSummaryService.getCards(authenticatedUser);

        // then
        assertThat(result).isInstanceOf(ExistingCardSummaryResponse.class);

        ExistingCardSummaryResponse response = (ExistingCardSummaryResponse) result;
        assertThat(response.hasCard()).isTrue();
        assertThat(response.cardUuid()).isEqualTo(CARD_UUID.toString());
        assertThat(response.cardName()).isEqualTo("WON 자동투자 카드");
        assertThat(response.cardNoDisplay()).isEqualTo("**** **** **** 1234");
        assertThat(response.cardStatus()).isEqualTo("ACTIVE");

        ExistingCardSummaryResponse.UsageSummary usageSummary = response.usageSummary();
        assertThat(usageSummary.currentMonthUsageAmount()).isEqualByComparingTo(BigDecimal.valueOf(usageAmount));
        assertThat(usageSummary.rewardRanges()).hasSize(3);
        assertThat(usageSummary.rewardRanges().get(0).min()).isEqualTo(0L);
        assertThat(usageSummary.rewardRanges().get(0).max()).isEqualTo(500_000L);
        assertThat(usageSummary.rewardRanges().get(0).rate()).isEqualByComparingTo("0.7");
        assertThat(usageSummary.rewardRanges().get(1).min()).isEqualTo(500_000L);
        assertThat(usageSummary.rewardRanges().get(1).max()).isEqualTo(1_500_000L);
        assertThat(usageSummary.rewardRanges().get(1).rate()).isEqualByComparingTo("1.0");
        assertThat(usageSummary.rewardRanges().get(2).min()).isEqualTo(1_500_000L);
        assertThat(usageSummary.rewardRanges().get(2).max()).isNull();
        assertThat(usageSummary.rewardRanges().get(2).rate()).isEqualByComparingTo("1.2");
        assertThat(usageSummary.currentRewardRate()).isEqualByComparingTo(expectedRate);
        assertThat(usageSummary.currentRangeMin()).isEqualTo(expectedRangeMin);
        assertThat(usageSummary.currentRangeMax()).isEqualTo(expectedRangeMax);

        then(cardSummaryRepository).should().findByUserUuid(USER_UUID);
    }

    @Test
    @DisplayName("인증 사용자 정보가 없으면 인증 필요 예외를 던진다")
    void getCardSummaryWithoutAuthenticatedUser() {
        assertThatThrownBy(() -> cardSummaryService.getCards(null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.AUTHENTICATION_REQUIRED);
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(AUTH_USER_UUID, USER_UUID, "test-jti");
    }

    private CardChnCardSummary cardSummary(BigDecimal currentMonthUsageAmount) {
        return CardChnCardSummary.builder()
                .userUuid(USER_UUID)
                .cardUserUuid(CARD_USER_UUID)
                .cardUuid(CARD_UUID)
                .cardName("WON 자동투자 카드")
                .cardNoDisplay("**** **** **** 1234")
                .cardStatus("ACTIVE")
                .currentMonthUsageAmount(currentMonthUsageAmount)
                .lastSyncedAt(LocalDateTime.of(2026, 5, 26, 1, 0))
                .build();
    }
}