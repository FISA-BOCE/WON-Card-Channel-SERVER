package com.woorifisa.won_card_channel_server.domain.sweep.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.woorifisa.won_card_channel_server.domain.sweep.dto.command.InboxClaimResult;
import com.woorifisa.won_card_channel_server.domain.sweep.dto.event.SweepInvestmentResultEvent;
import com.woorifisa.won_card_channel_server.domain.sweep.service.SweepResultInboxService;
import com.woorifisa.won_card_channel_server.domain.sweep.service.SweepResultProcessService;
import com.woorifisa.won_card_channel_server.global.config.SqsProperties;
import com.woorifisa.won_card_channel_server.global.config.SweepResultConsumerProperties;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.util.concurrent.Executor;

@Component
@Slf4j
public class SweepResultConsumer {

    private final SqsClient sqsClient;
    private final SqsProperties sqsProperties;
    private final SweepResultConsumerProperties properties;
    private final ObjectMapper objectMapper;
    private final SweepResultInboxService inboxService;
    private final SweepResultProcessService processService;
    private final Executor executor;

    public SweepResultConsumer(
            SqsClient sqsClient,
            SqsProperties sqsProperties,
            SweepResultConsumerProperties properties,
            ObjectMapper objectMapper,
            SweepResultInboxService inboxService,
            SweepResultProcessService processService,
            @Qualifier("sweepResultConsumerExecutor") Executor executor
    ) {
        this.sqsClient = sqsClient;
        this.sqsProperties = sqsProperties;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.inboxService = inboxService;
        this.processService = processService;
        this.executor = executor;
    }
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
            executor.execute(() -> handleSafely(message));
        }
    }

    private void handleSafely(Message message) {
        try {
            handle(message);
        } catch (Exception e) {
            log.warn("스윕 결과 메시지 worker 처리 실패. messageId={}", message.messageId(), e);
        }
    }

    private void handle(Message message) {
        try {
            SweepInvestmentResultEvent event;

            try {
                event = objectMapper.readValue(message.body(), SweepInvestmentResultEvent.class);
            } catch (JsonProcessingException e) {
                log.warn("역직렬화 불가능한 스윕 결과 메시지 스킵. messageId={}",
                        message.messageId(), e);
                deleteMessage(message);
                return;
            }

            try {
                processService.validate(event);
            } catch (BusinessException e) {
                log.warn("유효하지 않은 스윕 결과 메시지 스킵. messageId={}, eventId={}",
                        message.messageId(), event.eventId(), e);
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
                processService.process(event);
                inboxService.markProcessed(claimResult.inboxEventId());
                deleteMessage(message);

                log.info("스윕 결과 메시지 처리 완료. messageId={}, idempotencyKey={}",
                        message.messageId(), event.idempotencyKey());
            } catch (Exception e) {
                try {
                    inboxService.markFailed(claimResult.inboxEventId(), e.getMessage());
                } catch (Exception markFailedException) {
                    log.error("스윕 결과 inbox 실패 상태 저장 실패. messageId={}, inboxEventId={}",
                            message.messageId(), claimResult.inboxEventId(), markFailedException);
                }
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
