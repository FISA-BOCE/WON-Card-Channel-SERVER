package com.woorifisa.won_card_channel_server.domain.user.service.impl;

import com.woorifisa.won_card_channel_server.domain.auth.model.CardChnAuthUser;
import com.woorifisa.won_card_channel_server.domain.card.repository.CardChnCardSummaryRepository;
import com.woorifisa.won_card_channel_server.domain.user.service.AutoInvestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class JpaAutoInvestServiceImpl implements AutoInvestService {

    private final CardChnCardSummaryRepository cardSummaryRepository;

    @Override
    @Transactional
    public void disableAutoInvest(CardChnAuthUser user) {
        if (user == null || user.getUserUuid() == null) {
            return;
        }

        cardSummaryRepository.findByUserUuid(user.getUserUuid())
                .ifPresent(summary -> summary.updateSelectedEtfId(null));
    }
}
