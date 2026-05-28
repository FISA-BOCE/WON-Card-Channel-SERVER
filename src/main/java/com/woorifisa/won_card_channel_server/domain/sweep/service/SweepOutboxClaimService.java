package com.woorifisa.won_card_channel_server.domain.sweep.service;

import com.woorifisa.won_card_channel_server.domain.sweep.model.CardChnSweepOutbox;
import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.OutboxPublishStatus;
import com.woorifisa.won_card_channel_server.domain.sweep.repository.CardChnSweepOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SweepOutboxClaimService {

    private final CardChnSweepOutboxRepository outboxRepository;

    @Transactional
    public List<Long> claimPublishTargets(int batchSize) {
        List<CardChnSweepOutbox> targets = outboxRepository.findPublishTargets(
                List.of(OutboxPublishStatus.PENDING, OutboxPublishStatus.RETRY),
                LocalDateTime.now(),
                PageRequest.of(0, batchSize)
        );

        targets.forEach(CardChnSweepOutbox::markProcessing);

        return targets.stream()
                .map(CardChnSweepOutbox::getOutboxEventId)
                .toList();
    }
}
