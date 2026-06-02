package com.woorifisa.won_card_channel_server.domain.sweep.service;

import com.woorifisa.won_card_channel_server.domain.sweep.dto.command.InboxClaimResult;
import com.woorifisa.won_card_channel_server.domain.sweep.dto.event.SweepInvestmentResultEvent;
import com.woorifisa.won_card_channel_server.domain.sweep.model.SweepResultInbox;
import com.woorifisa.won_card_channel_server.domain.sweep.repository.SweepResultInboxRepository;
import com.woorifisa.won_card_channel_server.global.config.SweepResultConsumerProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SweepResultInboxService {

    private final SweepResultInboxRepository inboxRepository;
    private final SweepResultConsumerProperties properties;

    @Transactional
    public InboxClaimResult claim(SweepInvestmentResultEvent event, String payload) {
        return inboxRepository.findByIdempotencyKeyForUpdate(event.idempotencyKey())
                .map(this::claimExisting)
                .orElseGet(() -> claimNewSafely(event, payload));
    }

    @Transactional
    public void markProcessed(Long inboxEventId) {
        SweepResultInbox inbox = inboxRepository.findById(inboxEventId)
                .orElseThrow();
        inbox.markProcessed();
    }

    @Transactional
    public void markFailed(Long inboxEventId, String errorMessage) {
        SweepResultInbox inbox = inboxRepository.findById(inboxEventId)
                .orElseThrow();
        inbox.markFailed(errorMessage);
    }

    private InboxClaimResult claimNewSafely(SweepInvestmentResultEvent event, String payload) {
        try {
            return claimNew(event, payload);
        } catch (DataIntegrityViolationException e) {
            return inboxRepository.findByIdempotencyKeyForUpdate(event.idempotencyKey())
                    .map(this::claimExisting)
                    .orElseThrow(() -> e);
        }
    }

    private InboxClaimResult claimExisting(SweepResultInbox inbox) {
        if (inbox.alreadyProcessed()) {
            return InboxClaimResult.alreadyProcessed(inbox.getInboxEventId());
        }

        LocalDateTime timeoutAt = LocalDateTime.now().minusSeconds(properties.processingTimeoutSeconds());

        if (!inbox.canClaim(timeoutAt)) {
            return InboxClaimResult.notClaimed(inbox.getInboxEventId());
        }

        inbox.markProcessing();
        return InboxClaimResult.claimed(inbox.getInboxEventId());
    }

    private InboxClaimResult claimNew(SweepInvestmentResultEvent event, String payload) {
        SweepResultInbox inbox = SweepResultInbox.received(
                event.eventId(),
                event.sweepRequestId(),
                event.eventType(),
                payload,
                event.correlationId(),
                event.idempotencyKey()
        );

        inbox.markProcessing();
        SweepResultInbox saved = inboxRepository.saveAndFlush(inbox);

        return InboxClaimResult.claimed(saved.getInboxEventId());
    }
}
