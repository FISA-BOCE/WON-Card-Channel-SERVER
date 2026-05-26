package com.woorifisa.won_card_channel_server.domain.card;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.woorifisa.won_card_channel_server.domain.auth.exception.code.AuthErrorCode;
import com.woorifisa.won_card_channel_server.domain.card.dto.response.CardCoreCardsResponse;
import com.woorifisa.won_card_channel_server.domain.card.dto.response.ExistingCardSummaryResponse;
import com.woorifisa.won_card_channel_server.domain.card.dto.response.NoCardSummaryResponse;
import com.woorifisa.won_card_channel_server.domain.card.exception.code.CardErrorCode;
import com.woorifisa.won_card_channel_server.domain.card.external.CardCoreCardApi;
import com.woorifisa.won_card_channel_server.domain.card.model.CardChnCardSummary;
import com.woorifisa.won_card_channel_server.domain.card.repository.CardChnCardSummaryRepository;
import com.woorifisa.won_card_channel_server.domain.card.service.CardSummaryService;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import com.woorifisa.won_card_channel_server.global.response.SuccessStatus;
import com.woorifisa.won_card_channel_server.global.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @Mock
    private CardCoreCardApi cardCoreCardApi;

    @InjectMocks
    private CardSummaryService cardSummaryService;

    @Test
    @DisplayName("DB에 카드 요약이 있으면 외부 API를 호출하지 않고 DB 응답을 반환한다")
    void getCardsFromChannelDb() {
        // given
        AuthenticatedUser authenticatedUser = authenticatedUser();
        CardChnCardSummary cardSummary = cardSummary(BigDecimal.valueOf(1_245_000L));

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
        assertUsageSummary(response.usageSummary(), "1245000", "1.0", 500_000L, 1_500_000L);

        then(cardSummaryRepository).should().findByUserUuid(USER_UUID);
        then(cardCoreCardApi).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("DB에 카드 요약이 없고 카드 코어에도 카드가 없으면 hasCard false 응답을 반환한다")
    void getCardsWithoutCardFromCardCore() {
        // given
        AuthenticatedUser authenticatedUser = authenticatedUser();
        CardCoreCardsResponse coreData = new CardCoreCardsResponse(false, null, null, null, null);

        given(cardSummaryRepository.findByUserUuid(USER_UUID))
                .willReturn(Optional.empty());
        given(cardCoreCardApi.getCards(USER_UUID))
                .willReturn(ApiResponse.of(SuccessStatus.CARD_SUMMARY_NOT_FOUND, coreData));

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
        assertThat(response.cardProduct().benefits()).containsExactly(
                "국내외 결제 1% ETF 자동 적립",
                "VOO·QQQ 등 해외 ETF 선택 가능",
                "소수점 매수로 소액부터 가능"
        );

        then(cardSummaryRepository).should().findByUserUuid(USER_UUID);
        then(cardCoreCardApi).should().getCards(USER_UUID);
    }

    @Test
    @DisplayName("DB에 카드 요약이 없고 카드 코어에 카드가 있으면 카드 코어 응답을 반환한다")
    void getCardsWithCardFromCardCore() {
        // given
        AuthenticatedUser authenticatedUser = authenticatedUser();
        CardCoreCardsResponse coreData = new CardCoreCardsResponse(
                true,
                "CARD-UUID-1234-ABCD",
                "**** **** **** 1234",
                "ACTIVE",
                new CardCoreCardsResponse.UsageSummary(BigDecimal.valueOf(1_245_000L))
        );

        given(cardSummaryRepository.findByUserUuid(USER_UUID))
                .willReturn(Optional.empty());
        given(cardCoreCardApi.getCards(USER_UUID))
                .willReturn(ApiResponse.of(SuccessStatus.CARD_SUMMARY_FOUND, coreData));

        // when
        Object result = cardSummaryService.getCards(authenticatedUser);

        // then
        assertThat(result).isInstanceOf(ExistingCardSummaryResponse.class);

        ExistingCardSummaryResponse response = (ExistingCardSummaryResponse) result;
        assertThat(response.hasCard()).isTrue();
        assertThat(response.cardUuid()).isEqualTo("CARD-UUID-1234-ABCD");
        assertThat(response.cardName()).isEqualTo("WON 자동투자 카드");
        assertThat(response.cardNoDisplay()).isEqualTo("**** **** **** 1234");
        assertThat(response.cardStatus()).isEqualTo("ACTIVE");
        assertUsageSummary(response.usageSummary(), "1245000", "1.0", 500_000L, 1_500_000L);

        then(cardSummaryRepository).should().findByUserUuid(USER_UUID);
        then(cardCoreCardApi).should().getCards(USER_UUID);
    }

    @Test
    @DisplayName("인증 사용자 정보가 없으면 인증 필요 예외를 던진다")
    void getCardsWithoutAuthenticatedUser() {
        assertThatThrownBy(() -> cardSummaryService.getCards(null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.AUTHENTICATION_REQUIRED);

        then(cardSummaryRepository).shouldHaveNoInteractions();
        then(cardCoreCardApi).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("카드 코어 응답 데이터가 없으면 카드 응답 형식 예외를 던진다")
    void getCardsCoreResponseDataNull() {
        // given
        AuthenticatedUser authenticatedUser = authenticatedUser();

        given(cardSummaryRepository.findByUserUuid(USER_UUID))
                .willReturn(Optional.empty());
        given(cardCoreCardApi.getCards(USER_UUID))
                .willReturn(ApiResponse.of(SuccessStatus.CARD_SUMMARY_FOUND, null));

        // when & then
        assertThatThrownBy(() -> cardSummaryService.getCards(authenticatedUser))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CardErrorCode.INVALID_CARD_RESPONSE);
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(AUTH_USER_UUID, USER_UUID, "test-jti");
    }

    private void assertUsageSummary(
            ExistingCardSummaryResponse.UsageSummary usageSummary,
            String expectedUsageAmount,
            String expectedRewardRate,
            Long expectedRangeMin,
            Long expectedRangeMax
    ) {
        assertThat(usageSummary.currentMonthUsageAmount()).isEqualByComparingTo(expectedUsageAmount);
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
        assertThat(usageSummary.currentRewardRate()).isEqualByComparingTo(expectedRewardRate);
        assertThat(usageSummary.currentRangeMin()).isEqualTo(expectedRangeMin);
        assertThat(usageSummary.currentRangeMax()).isEqualTo(expectedRangeMax);
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