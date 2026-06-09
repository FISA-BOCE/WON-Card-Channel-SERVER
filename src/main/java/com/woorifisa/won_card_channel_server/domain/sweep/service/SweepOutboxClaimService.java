package com.woorifisa.won_card_channel_server.domain.sweep.service;

import com.woorifisa.won_card_channel_server.domain.sweep.exception.code.SweepErrorCode;
import com.woorifisa.won_card_channel_server.domain.sweep.model.SweepOutbox;
import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.OutboxPublishStatus;
import com.woorifisa.won_card_channel_server.domain.sweep.repository.SweepOutboxRepository;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SweepOutboxClaimService {

    private final SweepOutboxRepository outboxRepository;

    @Transactional
    public List<Long> claimPublishTargets(int batchSize) {
        if (batchSize <= 0) {
            throw new BusinessException(SweepErrorCode.SWEEP_OUTBOX_INVALID_BATCH_SIZE);
        }

        List<SweepOutbox> targets = outboxRepository.findPublishTargets(
                List.of(OutboxPublishStatus.PENDING, OutboxPublishStatus.RETRY),
                LocalDateTime.now(),
                PageRequest.of(0, batchSize)
        );

        targets.forEach(SweepOutbox::markProcessing);

        return targets.stream()
                .map(SweepOutbox::getOutboxEventId)
                .toList();
    }
}
