package com.woorifisa.won_card_channel_server.domain.reward.mapper;

import com.woorifisa.won_card_channel_server.domain.reward.dto.response.CardCoreRewardLedgerDetailResponse;
import com.woorifisa.won_card_channel_server.domain.reward.dto.response.CardCoreRewardLedgerResponse;
import com.woorifisa.won_card_channel_server.domain.reward.dto.response.RewardLedgerDetailResponse;
import com.woorifisa.won_card_channel_server.domain.reward.dto.response.RewardLedgerResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RewardLedgerMapper {

    public RewardLedgerResponse toResponse(CardCoreRewardLedgerResponse response) {
        List<CardCoreRewardLedgerResponse.CardCoreRewardLedgerItem> ledgers =
                response.ledgers() == null ? List.of() : response.ledgers();

        return new RewardLedgerResponse(
                response.baseYear(),
                response.totalAccumulatedAmount(),
                ledgers.stream()
                        .map(this::toLedgerItem)
                        .toList()
        );
    }

    public RewardLedgerDetailResponse toDetailResponse(CardCoreRewardLedgerDetailResponse response) {
        return new RewardLedgerDetailResponse(
                response.pointLedgerId(),
                response.baseMonth(),
                response.type(),
                response.pointAmount(),
                response.occurredAt(),
                toRewardDetail(response.detail())

        );
    }

    private RewardLedgerDetailResponse.RewardDetail toRewardDetail(
            CardCoreRewardLedgerDetailResponse.CardCoreRewardDetail detail
    ) {
        return new RewardLedgerDetailResponse.RewardDetail(
                detail.previousMonthSpendAmount(),
                detail.targetSpendAmount(),
                detail.shortfallAmount()
        );
    }

    private RewardLedgerResponse.RewardLedgerItem toLedgerItem(
            CardCoreRewardLedgerResponse.CardCoreRewardLedgerItem item
    ) {
        return new RewardLedgerResponse.RewardLedgerItem(
                item.pointLedgerId(),
                item.baseMonth(),
                item.pointAmount(),
                item.type(),
                item.occurredAt()
        );
    }
}
