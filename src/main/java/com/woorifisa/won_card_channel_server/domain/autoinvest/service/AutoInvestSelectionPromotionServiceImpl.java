package com.woorifisa.won_card_channel_server.domain.autoinvest.service;

import com.woorifisa.won_card_channel_server.domain.card.repository.CardChnCardSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AutoInvestSelectionPromotionServiceImpl implements AutoInvestSelectionPromotionService {

    private final CardChnCardSummaryRepository cardSummaryRepository;

    @Override
    @Transactional
    public int promoteEffectivePendingSelections(LocalDateTime batchStartedAt) {
        return cardSummaryRepository.promoteEffectivePendingSelections(batchStartedAt);
    }
}
