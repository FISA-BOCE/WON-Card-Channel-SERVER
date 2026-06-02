package com.woorifisa.won_card_channel_server.domain.autoinvest.service;

import java.time.LocalDateTime;

public interface AutoInvestSelectionPromotionService {

    int promoteEffectivePendingSelections(LocalDateTime batchStartedAt);
}
