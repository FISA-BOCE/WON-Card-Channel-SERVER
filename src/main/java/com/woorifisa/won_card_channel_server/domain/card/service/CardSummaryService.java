package com.woorifisa.won_card_channel_server.domain.card.service;

import com.woorifisa.won_card_channel_server.domain.auth.exception.code.AuthErrorCode;
import com.woorifisa.won_card_channel_server.domain.card.dto.response.CardCoreCardsResponse;
import com.woorifisa.won_card_channel_server.domain.card.dto.response.CardInfoResponse;
import com.woorifisa.won_card_channel_server.domain.card.dto.response.CardSummaryResponse;
import com.woorifisa.won_card_channel_server.domain.card.dto.response.ExistingCardSummaryResponse;
import com.woorifisa.won_card_channel_server.domain.card.dto.response.NoCardSummaryResponse;
import com.woorifisa.won_card_channel_server.domain.card.exception.code.CardErrorCode;
import com.woorifisa.won_card_channel_server.domain.card.external.CardCoreCardApi;
import com.woorifisa.won_card_channel_server.domain.card.model.CardChnCardSummary;
import com.woorifisa.won_card_channel_server.domain.card.repository.CardChnCardSummaryRepository;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import com.woorifisa.won_card_channel_server.global.response.ApiResponse;
import com.woorifisa.won_card_channel_server.global.security.AuthenticatedUser;
import feign.FeignException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Collections;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CardSummaryService {

    private static final String CARD_PRODUCT_NAME = "WON 자동투자 카드";
    private static final BigDecimal REWARD_RATE_MIN = BigDecimal.valueOf(0.7);
    private static final BigDecimal REWARD_RATE_MIDDLE = BigDecimal.valueOf(1.0);
    private static final BigDecimal REWARD_RATE_MAX = BigDecimal.valueOf(1.2);
    private static final long FIRST_RANGE_MIN = 0L;
    private static final long FIRST_RANGE_MAX = 500_000L;
    private static final long SECOND_RANGE_MAX = 1_500_000L;

    private final CardChnCardSummaryRepository cardSummaryRepository;
    private final CardCoreCardApi cardCoreCardApi;

    public CardSummaryResponse getCards(AuthenticatedUser authenticatedUser) {
        UUID userUuid = extractUserUuid(authenticatedUser);

        return cardSummaryRepository.findByUserUuid(userUuid)
                .<CardSummaryResponse>map(this::toExistingCardResponse)
                .orElseGet(() -> getCardsFromCardCore(userUuid));
    }

    public CardInfoResponse getCardInfo(AuthenticatedUser authenticatedUser) {
        UUID userUuid = extractUserUuid(authenticatedUser);

        return cardSummaryRepository.findByUserUuid(userUuid)
                .map(cardSummary -> new CardInfoResponse(List.of(
                        new CardInfoResponse.CardInfo(
                                cardSummary.getCardName(),
                                cardSummary.getCardNoDisplay()
                        )
                )))
                .orElseGet(() -> new CardInfoResponse(Collections.emptyList()));
    }

    private CardSummaryResponse getCardsFromCardCore(UUID userUuid) {
        try {
            ApiResponse<CardCoreCardsResponse> coreResponse = cardCoreCardApi.getCards(userUuid);
            CardCoreCardsResponse data = extractCardData(coreResponse);

            if (!data.hasCard()) {
                return toNoCardResponse();
            }

            return toExistingCardResponse(data);
        } catch (FeignException e) {
            throw new BusinessException(CardErrorCode.CARD_INFORMATION_UNAVAILABLE, e);
        }
    }

    private UUID extractUserUuid(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.userUuid() == null) {
            throw new BusinessException(AuthErrorCode.AUTHENTICATION_REQUIRED);
        }

        return authenticatedUser.userUuid();
    }

    private CardCoreCardsResponse extractCardData(ApiResponse<CardCoreCardsResponse> coreResponse) {
        if (coreResponse == null || coreResponse.data() == null) {
            throw new BusinessException(CardErrorCode.INVALID_CARD_RESPONSE);
        }

        return coreResponse.data();
    }

    private NoCardSummaryResponse toNoCardResponse() {
        return new NoCardSummaryResponse(
                new NoCardSummaryResponse.CardProduct(
                        CARD_PRODUCT_NAME,
                        REWARD_RATE_MIN,
                        REWARD_RATE_MAX,
                        200_000L,
                        List.of(
                                "국내외 결제 1% ETF 자동 적립",
                                "VOO·QQQ 등 해외 ETF 선택 가능",
                                "소수점 매수로 소액부터 가능"
                        )
                )
        );
    }

    private ExistingCardSummaryResponse toExistingCardResponse(CardChnCardSummary cardSummary) {
        BigDecimal usageAmount = cardSummary.getCurrentMonthUsageAmount();
        CurrentRewardRange currentRewardRange = resolveCurrentRewardRange(usageAmount);

        return new ExistingCardSummaryResponse(
                cardSummary.getCardUuid().toString(),
                cardSummary.getCardName(),
                cardSummary.getCardNoDisplay(),
                cardSummary.getCardStatus(),
                new ExistingCardSummaryResponse.UsageSummary(
                        usageAmount,
                        rewardRanges(),
                        currentRewardRange.rate(),
                        currentRewardRange.min(),
                        currentRewardRange.max()
                )
        );
    }

    private ExistingCardSummaryResponse toExistingCardResponse(CardCoreCardsResponse response) {
        BigDecimal usageAmount = response.usageSummary() == null
                ? null
                : response.usageSummary().currentMonthUsageAmount();
        CurrentRewardRange currentRewardRange = resolveCurrentRewardRange(usageAmount);

        return new ExistingCardSummaryResponse(
                response.cardUuid(),
                CARD_PRODUCT_NAME,
                response.cardNoDisplay(),
                response.cardStatus(),
                new ExistingCardSummaryResponse.UsageSummary(
                        usageAmount,
                        rewardRanges(),
                        currentRewardRange.rate(),
                        currentRewardRange.min(),
                        currentRewardRange.max()
                )
        );
    }

    private List<ExistingCardSummaryResponse.RewardRange> rewardRanges() {
        return List.of(
                new ExistingCardSummaryResponse.RewardRange(FIRST_RANGE_MIN, FIRST_RANGE_MAX, REWARD_RATE_MIN),
                new ExistingCardSummaryResponse.RewardRange(FIRST_RANGE_MAX, SECOND_RANGE_MAX, REWARD_RATE_MIDDLE),
                new ExistingCardSummaryResponse.RewardRange(SECOND_RANGE_MAX, null, REWARD_RATE_MAX)
        );
    }

    private CurrentRewardRange resolveCurrentRewardRange(BigDecimal usageAmount) {
        BigDecimal amount = usageAmount == null ? BigDecimal.ZERO : usageAmount;

        if (amount.compareTo(BigDecimal.valueOf(FIRST_RANGE_MAX)) < 0) {
            return new CurrentRewardRange(FIRST_RANGE_MIN, FIRST_RANGE_MAX, REWARD_RATE_MIN);
        }

        if (amount.compareTo(BigDecimal.valueOf(SECOND_RANGE_MAX)) < 0) {
            return new CurrentRewardRange(FIRST_RANGE_MAX, SECOND_RANGE_MAX, REWARD_RATE_MIDDLE);
        }

        return new CurrentRewardRange(SECOND_RANGE_MAX, null, REWARD_RATE_MAX);
    }

    private record CurrentRewardRange(
            Long min,
            Long max,
            BigDecimal rate
    ) {
    }
}
