package com.woorifisa.won_card_channel_server.domain.sweep.service;

import com.woorifisa.won_card_channel_server.domain.card.exception.code.CardErrorCode;
import com.woorifisa.won_card_channel_server.domain.sweep.dto.command.SweepOutboxPublishMessage;
import com.woorifisa.won_card_channel_server.global.config.SqsProperties;
import com.woorifisa.won_card_channel_server.global.config.SweepOutboxPublisherProperties;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SweepOutboxPublishService {

    private final SweepOutboxStatusService statusService;
    private final SqsClient sqsClient;
    private final SqsProperties sqsProperties;
    private final SweepOutboxPublisherProperties publisherProperties;

    public void publish(Long outboxEventId) {
        SweepOutboxPublishMessage message;

        try {
            message = statusService.getPublishMessage(outboxEventId);
        } catch (BusinessException e) {
            log.warn(
                    "스윕 Outbox 발행 메시지 조회에 실패했습니다. outboxEventId={}, errorCode={}",
                    outboxEventId, e.getErrorCode().getCode(), e);
            return;
        }

        try {
            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(sqsProperties.sweepRequestQueueUrl())
                    .messageBody(message.payload())
                    .messageDeduplicationId(message.idempotencyKey())
                    .messageGroupId(createMessageGroupId(message.cardUserUuid()))
                    .build();

            sqsClient.sendMessage(request);

            statusService.markPublished(outboxEventId);

            log.info("스윕 Outbox 이벤트 발행 성공. outboxEventId={}, eventId={}, sweepRequestId={}",
                    message.outboxEventId(), message.eventId(), message.sweepRequestId());
        } catch (Exception e) {
            statusService.markPublishFailed(outboxEventId, e.getMessage(), publisherProperties.maxRetryCount());

            log.warn("스윕 Outbox 이벤트 발행 실패. outboxEventId={}, eventId={}, sweepRequestId={}",
                    message.outboxEventId(), message.eventId(), message.sweepRequestId(), e);
        }
    }

    private String createMessageGroupId(UUID cardUserUuid) {
        if (cardUserUuid == null) {
            throw new BusinessException(CardErrorCode.CARD_USER_NOT_FOUND);
        }

        int shard = Math.floorMod(cardUserUuid.hashCode(), publisherProperties.messageGroupShardCount());
        return "sweep-user-" + shard;
    }

}
