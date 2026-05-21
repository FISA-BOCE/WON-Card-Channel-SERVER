package com.woorifisa.won_card_channel_server.domain.reward.mapper;

import com.woorifisa.won_card_channel_server.domain.reward.dto.response.CardCoreRewardLedgerResponse;
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
