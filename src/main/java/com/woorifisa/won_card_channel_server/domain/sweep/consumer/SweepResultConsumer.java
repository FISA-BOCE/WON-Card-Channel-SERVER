package com.woorifisa.won_card_channel_server.domain.sweep.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.woorifisa.won_card_channel_server.domain.sweep.dto.command.InboxClaimResult;
import com.woorifisa.won_card_channel_server.domain.sweep.dto.event.SweepInvestmentResultEvent;
import com.woorifisa.won_card_channel_server.domain.sweep.service.SweepResultInboxService;
import com.woorifisa.won_card_channel_server.domain.sweep.service.SweepResultProcessService;
import com.woorifisa.won_card_channel_server.global.config.SqsProperties;
import com.woorifisa.won_card_channel_server.global.config.SweepResultConsumerProperties;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

@Component
@RequiredArgsConstructor
@Slf4j
public class SweepResultConsumer {

    private final SqsClient sqsClient;
    private final SqsProperties sqsProperties;
    private final SweepResultConsumerProperties properties;
    private final ObjectMapper objectMapper;
    private final SweepResultInboxService inboxService;
    private final SweepResultProcessService processService;

    @Scheduled(fixedDelayString = "${sweep.result.consumer.fixed-delay-ms:10000}")
    public void poll() {
        if (!properties.enabled()) {
            return;
        }

        ReceiveMessageResponse response = sqsClient.receiveMessage(
                ReceiveMessageRequest.builder()
                        .queueUrl(sqsProperties.sweepResultQueueUrl())
                        .maxNumberOfMessages(properties.maxMessages())
                        .waitTimeSeconds(properties.waitTimeSeconds())
                        .build()
        );

        for (Message message : response.messages()) {
            handle(message);
        }
    }

    private void handle(Message message) {
        try {
            SweepInvestmentResultEvent event =
                    objectMapper.readValue(message.body(), SweepInvestmentResultEvent.class);

            try {
                processService.validate(event);
            } catch (BusinessException e) {
                log.warn("유효하지 않은 스윕 결과 메시지 스킵. messageId={}", message.messageId(), e);
                deleteMessage(message);
                return;
            }

            InboxClaimResult claimResult = inboxService.claim(event, message.body());

            if (claimResult.alreadyProcessed()) {
                deleteMessage(message);
                return;
            }

            if (!claimResult.claimed()) {
                return;
            }

            try {
                processService.process(claimResult.inboxEventId(), event);
                inboxService.markProcessed(claimResult.inboxEventId());
                deleteMessage(message);

                log.info("스윕 결과 메시지 처리 완료. messageId={}, idempotencyKey={}",
                        message.messageId(), event.idempotencyKey());
            } catch (Exception e) {
                inboxService.markFailed(claimResult.inboxEventId(), e.getMessage());
                throw e;
            }
        } catch (Exception e) {
            log.warn("스윕 결과 메시지 처리 실패. messageId={}", message.messageId(), e);
        }
    }

    private void deleteMessage(Message message) {
        sqsClient.deleteMessage(
                DeleteMessageRequest.builder()
                        .queueUrl(sqsProperties.sweepResultQueueUrl())
                        .receiptHandle(message.receiptHandle())
                        .build()
        );
    }
}
