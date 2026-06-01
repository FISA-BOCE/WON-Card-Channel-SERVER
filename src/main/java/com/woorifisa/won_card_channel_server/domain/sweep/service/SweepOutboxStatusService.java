package com.woorifisa.won_card_channel_server.domain.sweep.service;

import com.woorifisa.won_card_channel_server.domain.sweep.dto.command.SweepOutboxPublishMessage;
import com.woorifisa.won_card_channel_server.domain.sweep.exception.code.SweepErrorCode;
import com.woorifisa.won_card_channel_server.domain.sweep.model.SweepOutbox;
import com.woorifisa.won_card_channel_server.domain.sweep.repository.SweepOutboxRepository;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SweepOutboxStatusService {

    private final SweepOutboxRepository outboxRepository;

    @Transactional(readOnly = true)
    public SweepOutboxPublishMessage getPublishMessage(Long outboxEventId) {
        SweepOutbox outbox = findOutbox(outboxEventId);

        if (!outbox.isProcessing()) {
            throw new BusinessException(SweepErrorCode.SWEEP_OUTBOX_INVALID_PUBLISH_STATE);
        }

        return new SweepOutboxPublishMessage(
                outbox.getOutboxEventId(),
                outbox.getSweepRequestId(),
                outbox.getEventId(),
                outbox.getPayload(),
                outbox.getIdempotencyKey()
        );
    }

    @Transactional
    public void markPublished(Long outboxEventId) {
        SweepOutbox outbox = findOutbox(outboxEventId);

        if (!outbox.isProcessing()) {
            log.warn(
                    "스윕 Outbox 발행 성공 상태 반영을 건너뜁니다. outboxEventId={}, publishStatus={}",
                    outbox.getOutboxEventId(),
                    outbox.getPublishStatus()
            );
            return;
        }

        outbox.markPublished();
    }

    @Transactional
    public void markPublishFailed(Long outboxEventId, String errorMessage, int maxRetryCount) {
        SweepOutbox outbox = findOutbox(outboxEventId);

        if (!outbox.isProcessing()) {
            log.warn(
                    "스윕 Outbox 발행 실패 상태 반영을 건너뜁니다. outboxEventId={}, publishStatus={}",
                    outbox.getOutboxEventId(),
                    outbox.getPublishStatus()
            );
            return;
        }

        if (outbox.getRetryCount() + 1 >= maxRetryCount) {
            outbox.markFailed(errorMessage);
        } else {
            outbox.markRetry(errorMessage);
        }
    }

    private SweepOutbox findOutbox(Long outboxEventId) {
        return outboxRepository.findById(outboxEventId)
                .orElseThrow(() -> new BusinessException(SweepErrorCode.SWEEP_OUTBOX_NOT_FOUND));
    }
}
