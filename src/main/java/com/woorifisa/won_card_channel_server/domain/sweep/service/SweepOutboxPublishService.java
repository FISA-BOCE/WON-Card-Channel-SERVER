package com.woorifisa.won_card_channel_server.domain.sweep.service;

import com.woorifisa.won_card_channel_server.domain.sweep.model.CardChnSweepOutbox;
import com.woorifisa.won_card_channel_server.domain.sweep.repository.CardChnSweepOutboxRepository;
import com.woorifisa.won_card_channel_server.global.config.SqsProperties;
import com.woorifisa.won_card_channel_server.global.config.SweepOutboxPublisherProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class SweepOutboxPublishService {

    private final CardChnSweepOutboxRepository outboxRepository;
    private final SqsClient sqsClient;
    private final SqsProperties sqsProperties;
    private final SweepOutboxPublisherProperties publisherProperties;

    @Transactional
    public void publish(Long outboxEventId) {
        CardChnSweepOutbox outbox = outboxRepository.findById(outboxEventId)
                .orElseThrow();

        try {
            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(sqsProperties.sweepRequestQueueUrl())
                    .messageBody(outbox.getPayload())
                    .messageDeduplicationId(outbox.getIdempotencyKey())
                    .messageGroupId("SWEEP_REQUESTED")
                    .build();

            sqsClient.sendMessage(request);

            outbox.markPublished();

            log.info(
                    "스윕 Outbox 이벤트 발행 성공. outboxEventId={}, eventId={}, sweepRequestId={}",
                    outbox.getOutboxEventId(),
                    outbox.getEventId(),
                    outbox.getSweepRequestId()
            );
        } catch (Exception e) {
            if (outbox.getRetryCount() + 1 >= publisherProperties.maxRetryCount()) {
                outbox.markFailed(e.getMessage());
            } else {
                outbox.markRetry(e.getMessage());
            }

            log.warn(
                    "스윕 Outbox 이벤트 발행 실패. outboxEventId={}, eventId={}, retryCount={}",
                    outbox.getOutboxEventId(),
                    outbox.getEventId(),
                    outbox.getRetryCount(),
                    e
            );
        }
    }

}
