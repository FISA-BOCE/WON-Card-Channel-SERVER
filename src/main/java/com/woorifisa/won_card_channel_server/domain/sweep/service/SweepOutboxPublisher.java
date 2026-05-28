package com.woorifisa.won_card_channel_server.domain.sweep.service;

import com.woorifisa.won_card_channel_server.global.config.SweepOutboxPublisherProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SweepOutboxPublisher {

    private final SweepOutboxClaimService claimService;
    private final SweepOutboxPublishService publishService;
    private final SweepOutboxPublisherProperties properties;

    @Scheduled(fixedDelayString = "${sweep.outbox.publisher.fixed-delay-ms:10000}")
    public void publish() {
        if (!properties.enabled()) {
            return;
        }

        List<Long> outboxEventIds = claimService.claimPublishTargets(properties.batchSize());

        if (outboxEventIds.isEmpty()) {
            return;
        }

        log.info("스윕 Outbox 이벤트 발행 시작. count={}", outboxEventIds.size());

        for (Long outboxEventId : outboxEventIds) {
            publishService.publish(outboxEventId);
        }
    }
}
